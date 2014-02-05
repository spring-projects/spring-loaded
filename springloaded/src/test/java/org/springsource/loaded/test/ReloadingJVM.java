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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import org.springsource.loaded.Utils;

/**
 * Launches a separate JVM that has the agent attached. This JVM is running the class ReloadingJVMCommandProcess and
 * can be told to run commands like 'load a class' or 'execute a method'. The aim is this is very similar to testing
 * a real environment where the agent is attached to a process.
 *
 * @author Andy Clement
 */
public class ReloadingJVM {

	public static String agentJarLocation = null;
	String javaclasspath;
	File testdataDirectory;
	Process process;
	DataInputStream reader;
	DataOutputStream writer;
	DataInputStream readerErrors;

	static String search(File where) {
		File[] fs = where.listFiles();
		if (fs!=null) {
			for (File f: fs) {
				if (f.isDirectory()) {
					String s = search(f);
					if (s!=null) {
						return s;
					}
				}
				else if (f.getName().startsWith("springloaded") && f.getName().endsWith(".jar") && !f.getName().contains("sources")) {
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
	
	private ReloadingJVM(String agentOptions) {
		try {
			javaclasspath = System.getProperty("java.class.path");
			
			// Create a temporary folder where we can load/replace class files for the file watcher to observe
			testdataDirectory = File.createTempFile("_sl","");
			testdataDirectory.delete();
			testdataDirectory.mkdir();
			if (DEBUG_CLIENT_SIDE) {
				System.out.println("Found agent at "+agentJarLocation);
				System.out.println("(client) Test data directory is "+testdataDirectory);
			}
			javaclasspath = javaclasspath + File.pathSeparator + testdataDirectory.toString();
			if (DEBUG_CLIENT_SIDE) {
				System.out.println("(client) Classpath for JVM that is being launched: " + javaclasspath);
			}
			String OPTS = "";//"-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=4000,server=y,suspend=y";
			String AGENT_OPTION_STRING = "";
			if (agentOptions!=null && agentOptions.length()>0) {
				AGENT_OPTION_STRING = "-Dspringloaded="+agentOptions;
			}
			process = Runtime.getRuntime().exec(
					"java -noverify -javaagent:" + agentJarLocation + " -cp " + javaclasspath + " " + AGENT_OPTION_STRING +
					" "+OPTS+" "
							+ ReloadingJVMCommandProcess.class.getName(), new String[] { OPTS });
			writer = new DataOutputStream(process.getOutputStream());
			reader = new DataInputStream(process.getInputStream());
			readerErrors = new DataInputStream(process.getErrorStream());
			JVMOutput text = waitFor("ReloadingJVM:started");
			if (DEBUG_CLIENT_SIDE) {
				System.out.println(text);
			}
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to launch JVM", ioe);
		}
	}

	public static ReloadingJVM launch(String options) {
		return new ReloadingJVM(options);
	}

	private JVMOutput waitFor(String message) {
		return captureOutput(message);
	}

	private final static boolean DEBUG_CLIENT_SIDE = false;

	private JVMOutput sendAndReceive(String message) {
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
			StringBuilder s = new StringBuilder("==STDOUT==\n").append(stdout).append("\n").append("==STDERR==\n").append(stderr)
					.append("\n==========\n");
			return s.toString();
		}
	}

	private JVMOutput captureOutput(String terminationString) {
		try {
			long time = System.currentTimeMillis();
			int timeout = 1000; // 1s timeout
			byte[] buf = new byte[1024];
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while ((System.currentTimeMillis() - time) < timeout) {
				while (reader.available() != 0) {
					int read = reader.read(buf);
					baos.write(buf, 0, read);
					if (baos.toString().indexOf(terminationString) != -1) {
						break;
					}
				}
			}
			String stdout = baos.toString();
			baos = new ByteArrayOutputStream();
			while (readerErrors.available() != 0) {
				int read = readerErrors.read(buf);
				baos.write(buf, 0, read);
			}
			String stderr = baos.toString();
			if (DEBUG_CLIENT_SIDE) {
				System.out.println("(client) >> received  \n== STDOUT ==\n" + stdout + "\n== STDERR==\n" + stderr);
			}
			// append system error
			return new JVMOutput(stdout, stderr);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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
			for (File file: files) {
				deleteIt(file);
			}
//			System.out.println("Deleting "+f);
			f.delete();
		} else {
//			System.out.println("Deleting "+f);
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
			System.out.println("(client) copying class to test data directory: "+classname);
		}
		String classfile = classname.replaceAll("\\.",File.separator)+".class";
		File f = new File("../testdata/bin",classfile);
		byte[] data = Utils.load(f);
		// Ensure directories exist
		int dotPos = classname.lastIndexOf(".");
		if (dotPos!=-1) {
			new File(testdataDirectory,classname.substring(0,dotPos).replaceAll("\\.",File.separator)).mkdirs();
		}
		Utils.write(new File(testdataDirectory,classfile),data);
	}

	public JVMOutput newInstance(String instanceName, String classname) {
		copyToTestdataDirectory(classname);
		return sendAndReceive("new " + instanceName + " " + classname);
	}
	
	public JVMOutput reload(String dottedClassname) {
		return sendAndReceive("reload "+dottedClassname);
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
		String classfile = string.replaceAll("\\.",File.separator)+".class";
		Utils.write(new File(testdataDirectory,classfile),newdata);
	}

}
