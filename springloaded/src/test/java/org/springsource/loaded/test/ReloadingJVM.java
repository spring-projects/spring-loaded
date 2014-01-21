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

public class ReloadingJVM {

	public final static String agentJarLocation = "../org.springsource.loaded/springloaded-1.0.0.jar";
	String javaclasspath;
	Process process;
	DataInputStream reader;
	DataOutputStream writer;
	DataInputStream readerErrors;

	private ReloadingJVM() {
		try {
			javaclasspath = System.getProperty("java.class.path");
			javaclasspath = javaclasspath + File.pathSeparator + TestUtils.getPathToClasses("../testdata");
			if (DEBUG_CLIENT_SIDE) {
				System.out.println("(client) Classpath for JVM that is being launched: " + javaclasspath);
			}
			String OPTS = "JVMOPTS=\"-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=4000,server=y,suspend=y\"";
			process = Runtime.getRuntime().exec(
					"java -javaagent:" + agentJarLocation + " -cp " + javaclasspath + " "
							+ ReloadingJVMCommandProcess.class.getName(), new String[] { OPTS });
			// "java -javaagent:../org.springsource.loaded/target/classes -cp " + jcp + " " + TestController.class.getName());
			writer = new DataOutputStream(process.getOutputStream());
			reader = new DataInputStream(process.getInputStream());
			readerErrors = new DataInputStream(process.getErrorStream());
			System.out.println(waitFor("ReloadingJVM:started"));
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to launch JVM", ioe);
		}
	}

	public static ReloadingJVM launch() {
		return new ReloadingJVM();
	}

	private Output waitFor(String message) {
		return captureOutput(message);
	}

	private final static boolean DEBUG_CLIENT_SIDE = true;

	private Output sendAndReceive(String message) {
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

	static class Output {
		public final String stdout;
		public final String stderr;

		Output(String stdout, String stderr) {
			this.stdout = stdout;
			this.stderr = stderr;
		}

		public String toString() {
			StringBuilder s = new StringBuilder("==STDOUT==\n").append(stdout).append("\n").append("==STDERR==\n").append(stderr)
					.append("\n");
			return s.toString();
		}
	}

	private Output captureOutput(String terminationString) {
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
			return new Output(stdout, stderr);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void shutdown() {
		System.out.println(sendAndReceive("exit"));
		process.destroy();
	}

	public Output echo(String string) {
		return sendAndReceive("echo " + string);
	}

	/**
	 * Call the static run() method on the specified class.
	 */
	public Output run(String classname) {
		return sendAndReceive("run " + classname);
	}

	public Output newInstance(String instanceName, String classname) {
		return sendAndReceive("new " + instanceName + " " + classname);
	}

	public Output call(String instanceName, String methodname) {
		return sendAndReceive("call " + instanceName + " " + methodname);
	}

	public void reload(String classname, byte[] newBytes) {
		Output output = sendAndReceive("reload " + classname + " " + toHexString(newBytes));
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

}
