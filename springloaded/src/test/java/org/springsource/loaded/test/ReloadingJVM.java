/*
 * Copyright 2010-2012 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springsource.loaded.test;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.springsource.loaded.Utils;

/**
 * Launches a separate JVM that has the agent attached. This JVM is running the class ReloadingJVMCommandProcess and
 * can be told to run commands like 'load a class' or 'execute a method'. The aim is this is very similar to testing
 * a real environment where the agent is attached to a process.
 *
 * @author Andy Clement
 */
public class ReloadingJVM {
	private static final boolean DEBUG_CLIENT_SIDE = true;
	private static final char NUL = '\0';
	private static int BUFFER_SIZE = 100;
	public static String agentJarLocation = null;
	String javaclasspath;
	File testdataDirectory;
	private Process process;
	private boolean debug = false;

	DataOutputStream writer;
	private LinkedBlockingDeque<String> stdoutQueue = new LinkedBlockingDeque<String>(BUFFER_SIZE);
	private LinkedBlockingDeque<String> stderrQueue = new LinkedBlockingDeque<String>(BUFFER_SIZE);

	static String search(File where) {
		File[] fs = where.listFiles();
		if (fs != null) {
			for (File f : fs) {
				if (f.isDirectory()) {
					String s = search(f);
					if (s != null) {
						return s;
					}
				} else if (f.getName().startsWith("springloaded") && f.getName().endsWith(".jar") && !f.getName().contains("sources") && !f.getName().contains("javadoc")) {
					return f.getAbsolutePath();
				}
			}
		}
		return null;
	}

	static {
		// Find the agent
		File searchLocation = new File("..");
		agentJarLocation = search(searchLocation);
	}

	private ReloadingJVM(String agentOptions, boolean debug) {
		try {
			this.debug = debug;
			javaclasspath = System.getProperty("java.class.path");

			// Create a temporary folder where we can load/replace class files for the file watcher to observe
			testdataDirectory = File.createTempFile("_sl", "");
			testdataDirectory.delete();
			testdataDirectory.mkdir();
			if (DEBUG_CLIENT_SIDE) {
				System.out.println("Found agent at " + agentJarLocation);
				System.out.println("(client) Test data directory is " + testdataDirectory);
			}
			javaclasspath = javaclasspath + File.pathSeparator + new File("../testdata-groovy/groovy-all-1.8.6.jar").toString();
			javaclasspath = javaclasspath + File.pathSeparator + testdataDirectory.toString();
			if (DEBUG_CLIENT_SIDE) {
				System.out.println("(client) Classpath for JVM that is being launched: " + javaclasspath);
			}
//			String OPTS = debug ? "-Xdebug -Xrunjdwp:transport=dt_socket,address=5100,server=y,suspend=y" : "";
			String AGENT_OPTION_STRING = "";
			if (agentOptions != null && agentOptions.length() > 0) {
				AGENT_OPTION_STRING = "-Dspringloaded=" + agentOptions;
			}
			if (DEBUG_CLIENT_SIDE) {
				System.out.println("java.home=" + System.getProperty("java.home"));
			}
//			process = Runtime.getRuntime().exec(
//					System.getProperty("java.home") + "/bin/java -noverify -javaagent:" + agentJarLocation + " -cp " + javaclasspath + " " + AGENT_OPTION_STRING + " " + OPTS + " "
//							+ ReloadingJVMCommandProcess.class.getName(), new String[] { OPTS });
			final ProcessBuilder builder = new ProcessBuilder();
			List<String> commands = new ArrayList<String>();
			commands.add(System.getProperty("java.home") + "/bin/java");
			commands.add("-noverify");
			commands.add("-javaagent:" + agentJarLocation);
			commands.add("-cp");
			commands.add(javaclasspath);
			if (AGENT_OPTION_STRING.length() > 0)
				commands.add(AGENT_OPTION_STRING);
			if (debug) {
				commands.add("-Xdebug");
				commands.add("-Xrunjdwp:transport=dt_socket,address=5100,server=y,suspend=y");
			}
			commands.add(ReloadingJVMCommandProcess.class.getName());
			builder.command(commands);

			process = builder.start();
			writer = new DataOutputStream(process.getOutputStream());
			startStdoutReader(process.getInputStream());
			startStderrReader(process.getErrorStream());

			if (debug) {
				System.out.println("Debugging launched VM, port 5100");
			}
			JVMOutput text = waitFor("ReloadingJVM:started");
			if (DEBUG_CLIENT_SIDE) {
				System.out.println(text);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace(System.err);
			throw new RuntimeException("Unable to launch JVM", ioe);
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
		}
	}

	public static ReloadingJVM launch(String options) {
		return new ReloadingJVM(options, false);
	}

	public static ReloadingJVM launch(String options, boolean debug) {
		return new ReloadingJVM(options, debug);
	}

	private JVMOutput waitFor(String message) throws InterruptedException {
		return captureOutput(message);
	}

	public JVMOutput sendAndReceive(String message) {
		try {
			if (DEBUG_CLIENT_SIDE) {
				System.out.println("(client) >> sending command '" + message + "'");
			}
			writer.writeUTF(message);
			writer.flush();
		} catch (IOException ioe) {
			throw new RuntimeException("Unexpected problem during message transfer, message='" + message + "'", ioe);
		}
		return captureOutput("!!");
	}

	static class JVMOutput {
		public final String stdout;
		public final String stderr;

		JVMOutput(String stdout, String stderr) {
			this.stdout = stdout;
			this.stderr = stderr;
		}

		public String toString() {
			StringBuilder s = new StringBuilder("==STDOUT==\n").append(stdout).append("\n").append("==STDERR==\n").append(stderr).append("\n==========\n");
			return s.toString();
		}
	}

	private JVMOutput captureOutput(String terminationString) {
		// stdout
		JVMOutput jvmOutput = null;
		try {
			String stderr = receiveFromControlQueue(stderrQueue, terminationString);
			Thread.yield();
			Thread.sleep(500); // give some time to thread to fill the queue...
			String stdout = receiveFromOutputQueue(stdoutQueue);
			jvmOutput = new JVMOutput(stdout, stderr);
		} catch (InterruptedException ex) {
			ex.printStackTrace(System.err);
			jvmOutput = new JVMOutput("", "Error occured. ReloadingJVM was interrupted");
		}
		return jvmOutput;
	}

	private String receiveFromOutputQueue(LinkedBlockingDeque<String> outputQueue) {
		StringWriter stringWriter = new StringWriter();
		List<String> lines = new ArrayList<String>();
		outputQueue.drainTo(lines);
		for (String line : lines) {
			stringWriter.append(line).append("\n");
			if (DEBUG_CLIENT_SIDE) {
				System.out.println("(client) >>> received: " + line);
			}
		}
		String capturedLines = stringWriter.toString();

		return capturedLines;
	}

	private String receiveFromControlQueue(LinkedBlockingDeque<String> controlQueue, String terminationString) throws InterruptedException {
		String nulLine = new String(new char[] { NUL });
		String line = nulLine;
		// stderr mimics control channel
		StringWriter stringWriter = new StringWriter();
		do {
			line = controlQueue.take(); // blocks
			if (DEBUG_CLIENT_SIDE) {
				System.out.println("(client) >> received: " + line);
			}
			stringWriter.append(line).append("\n");
		} while (!terminationString.equals(line) && !nulLine.equals(line));
		String capturedLines = stringWriter.toString();
		return capturedLines;
	}

	public void shutdown() {
		System.out.println(sendAndReceive("exit"));
		deleteIt(testdataDirectory);
		process.destroy();
	}

	/**
	 * Recursively delete a file (emptying sub-directories if necessary)
	 */
	private void deleteIt(File f) {
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (File file : files) {
				deleteIt(file);
			}
			f.delete();
		} else {
			f.delete();
		}
	}

	public JVMOutput echo(String string) {
		return sendAndReceive("echo " + string);
	}

	/**
	 * Call the static main() method on the specified class.
	 */
	public JVMOutput run(String classname) {
		copyToTestdataDirectory(classname);
		return sendAndReceive("run " + classname);
	}

	public void copyToTestdataDirectory(String classname) {
		if (DEBUG_CLIENT_SIDE) {
			System.out.println("(client) copying class to test data directory: " + classname);
		}
		String classfile = classname.replaceAll("\\.", File.separator) + ".class";
		File f = new File("../testdata/build/classes/main", classfile);
		if (!f.exists()) {
			f = new File("../testdata-groovy/build/classes/main", classfile);
		}
		byte[] data = Utils.load(f);
		// Ensure directories exist
		int dotPos = classname.lastIndexOf(".");
		if (dotPos != -1) {
			new File(testdataDirectory, classname.substring(0, dotPos).replaceAll("\\.", File.separator)).mkdirs();
		}
		Utils.write(new File(testdataDirectory, classfile), data);
	}

	public void copyResourceToTestDataDirectory(String resourcename) {
		if (DEBUG_CLIENT_SIDE) {
			System.out.println("(client) copying resource to test data directory: " + resourcename);
		}
		File f = new File("../testdata-groovy/", resourcename);
		byte[] data = Utils.load(f);
		//		// Ensure directories exist
		//		int dotPos = classname.lastIndexOf(".");
		//		if (dotPos!=-1) {
		//			new File(testdataDirectory,classname.substring(0,dotPos).replaceAll("\\.",File.separator)).mkdirs();
		//		}
		Utils.write(new File(testdataDirectory, resourcename), data);
	}

	public void clearTestdataDirectory() {
		File[] fs = testdataDirectory.listFiles();
		for (File f : fs) {
			delete(f);
		}
	}

	private void delete(File toDelete) {
		if (toDelete.isDirectory()) {
			File[] fs = toDelete.listFiles();
			for (File f : fs) {
				delete(f);
			}
		} else {
			toDelete.delete();
		}
	}

	public JVMOutput newInstance(String instanceName, String classname) {
		copyToTestdataDirectory(classname);
		return sendAndReceive("new " + instanceName + " " + classname);
	}

	public JVMOutput reload(String dottedClassname) {
		return sendAndReceive("reload " + dottedClassname);
	}

	public JVMOutput call(String instanceName, String methodname) {
		return sendAndReceive("call " + instanceName + " " + methodname);
	}

	public void reload(String classname, byte[] newBytes) {
		JVMOutput output = sendAndReceive("reload " + classname + " " + toHexString(newBytes));
		// assert it is ok
	}

	private String toHexString(byte[] bs) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < bs.length; i++) {
			s.append(Integer.toHexString(bs[i] >>> 4));
			s.append(Integer.toHexString(bs[i] & 0xf));
		}
		return s.toString();
	}

	public void updateClass(String string, byte[] newdata) {
		String classfile = string.replaceAll("\\.", File.separator) + ".class";
		Utils.write(new File(testdataDirectory, classfile), newdata);
	}

	private static abstract class ReadingThread implements Runnable {
		private static final char NL = '\n';
		private static final char CR = '\r';

		private Reader output;
		private StringBuffer lineBuffer;

		ReadingThread(InputStream output) {
			this.output = new InputStreamReader(output);
			this.lineBuffer = new StringBuffer();
		}

		public void run() {
			while (true) {
				try {
					int readCharacter = output.read();
					if (-1 == readCharacter) {
						endOfStream();
						break;
					} else {
						char lastChar = (char) readCharacter;
						if (CR == lastChar || NL == lastChar) {
							// skip new line characters
						} else {
							lineBuffer.append(lastChar);
						}
						if (NL == lastChar) {
							// sync and reset buffer on new line
							lineReceived();
						}
					}
				} catch (IOException e) {
					onException(e);
				}
			}
		}

		private void onException(IOException e) {
			// we've broken: flush lines, notify interested parties..
			lineReceived();
		}

		private void endOfStream() {
			// nothing to read - notify interested parties..
			lineReceived();
		};

		private void lineReceived() {
			synchronized (lineBuffer) {
				String line = lineBuffer.toString();
				lineBuffer.setLength(0);
				if (line.length() > 0) {
					onLine(line);
				}
			}
		}

		abstract void onLine(String line);
	}

	private class StdoutThread extends ReadingThread {
		StdoutThread(InputStream output) {
			super(output);
		}

		void onLine(String line) {
			try {
				stdoutQueue.put(line);
			} catch (InterruptedException e) {
				System.err.println("Unable to put following stdout line: '" + line + "'");
			}
		}
	}

	private class StderrThread extends ReadingThread {
		public StderrThread(InputStream output) {
			super(output);
		}

		@Override
		void onLine(String line) {
			try {
				stderrQueue.put(line);
			} catch (InterruptedException e) {
				System.err.println("Unable to put following stderr line: '" + line + "'");
			}
		}
	}

	private void startStderrReader(InputStream errorStream) {
		Thread thread = new Thread(new StderrThread(errorStream), "stderr reader");
		thread.setDaemon(true);
		thread.start();
	}

	private void startStdoutReader(InputStream stdoutStream) {
		Thread thread = new Thread(new StdoutThread(stdoutStream), "stdout reader");
		thread.setDaemon(true);
		thread.start();
	}
}
