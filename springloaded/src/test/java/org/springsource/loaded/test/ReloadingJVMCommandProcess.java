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

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;


/**
 * When a ReloadingJVM is launched, this is the program it runs. It can be driven by commands and instructed to do
 * things.
 *
 * @author Andy Clement
 * @since 0.7.3
 */
public class ReloadingJVMCommandProcess {

	static class ExtensibleClassLoader extends URLClassLoader {

		public ExtensibleClassLoader(ClassLoader parent) {
			super(new URL[0], parent);
		}

		public void addJar(String jar) {
			try {
				addURL(new File(jar).toURI().toURL());
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

	}

	static ExtensibleClassLoader cl;

	public static void main(String[] argv) throws IOException {
		cl = new ExtensibleClassLoader(ReloadingJVMCommandProcess.class.getClassLoader());
		System.err.println("ReloadingJVM:started");
		System.err.flush();
		try {
			DataInputStream br = new DataInputStream((System.in));
			do {
				try {
					String command = br.readUTF();
					System.err.println("(jvm) processing command '" + command + "'");
					StringTokenizer st = new StringTokenizer(command);
					String commandName = st.nextToken();
					List<String> arguments = new ArrayList<String>();
					while (st.hasMoreTokens()) {
						arguments.add(st.nextToken());
					}
					//					String[] args = (arguments.size() > 0 ? arguments.toArray(new String[arguments.size()]) : null);
					if (commandName.equals("exit")) {
						System.err.println("ReloadingJVM:terminating!!");
						System.exit(0);
						return;
					}
					else if (commandName.equals("echo")) {
						echoCommand(arguments);
					}
					else if (commandName.equals("run")) {
						runCommand(arguments.get(0), asArray(arguments, 1));
					}
					else if (commandName.equals("new")) {
						newCommand(arguments.get(0), arguments.get(1));
					}
					else if (commandName.equals("call")) {
						callCommand(arguments.get(0), arguments.get(1));
					}
					else if (commandName.equals("reload")) {
						reloadCommand(arguments.get(0), arguments.size() == 1 ? null : arguments.get(1));
					}
					else if (commandName.equals("isreloadabletype")) {
						isReloadableTypeCheck(arguments.get(0));
					}
					else if (commandName.equals("extendcp")) {
						extendClasspath(arguments.get(0));
					}
					else {
						System.out.println("Don't understand command '" + commandName + "' !!");
					}
				}
				catch (Exception e) {
					e.printStackTrace(System.out);
				}
				finally {
					try {
						Thread.sleep(750);
					}
					catch (Exception e) {
						e.printStackTrace(System.out);
					}
				}
			}
			while (true);
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	private static String[] asArray(List<String> arguments, int startpos) {
		String[] result = new String[arguments.size() - startpos];
		for (int i = startpos; i < arguments.size(); i++) {
			result[i - startpos] = arguments.get(i - startpos);
		}
		return result;
	}

	private static void echoCommand(List<String> arguments) {
		for (int i = 0, max = arguments.size(); i < max; i++) {
			if (i > 0) {
				System.out.print(" ");
			}
			System.out.print(arguments.get(i));
		}
	}

	/**
	 * Call the static run() method on the specified class.
	 */
	private static void runCommand(String classname, String[] arguments) {
		try {
			//			System.out.println("Running the main method on "+classname+" with arguments ["+toString(arguments)+"]");
			Class<?> clazz = Class.forName(classname);
			Method m = clazz.getDeclaredMethod("run");
			m.invoke(null);
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	//	private static String toString(String[] array) {
	//		if (array == null) {
	//			return "null";
	//		}
	//		StringBuilder s = new StringBuilder();
	//		for (String string: array) {
	//			s.append(string);
	//			s.append(" ");
	//		}
	//		return s.toString().trim();
	//	}

	private static Map<String, Object> instances = new HashMap<String, Object>();

	private static void callCommand(String instanceName, String methodName) {
		try {
			System.err.println("Calling method '" + methodName + "' on variable '" + instanceName + "'");
			Object o = instances.get(instanceName);
			Class<?> clazz = o.getClass();
			Method m = clazz.getDeclaredMethod(methodName);
			m.invoke(o);
			System.err.println("!!");
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	private static void extendClasspath(String newClasspathEntry) {
		System.err.println("Extending classpath to include: " + newClasspathEntry);
		cl.addJar(newClasspathEntry);
		System.err.println("!!");
	}

	private static void isReloadableTypeCheck(String classname) {
		try {
			Class<?> clazz = Class.forName(classname);
			TypeRegistry tr = TypeRegistry.getTypeRegistryFor(clazz.getClassLoader());
			if (tr.getReloadableType(clazz) != null) {
				System.out.println(classname + " is reloadable type");
			}
			else {
				System.out.println(classname + " is not reloadable type");
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	private static void reloadCommand(String classname, String data) {
		try {
			Class<?> clazz = Class.forName(classname);
			TypeRegistry tr = TypeRegistry.getTypeRegistryFor(clazz.getClassLoader());
			ReloadableType rt = tr.getReloadableType(clazz);
			byte[] newdata = data != null ? fromHexString(data) : rt.bytesInitial;
			boolean b = rt.loadNewVersion("2", newdata);
			if (!b) {
				throw new IllegalStateException("Failed to reload new verion of " + classname);
			}
			System.err.println("!!");
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	private static byte[] fromHexString(String data) {
		byte[] bs = new byte[data.length() / 2];
		for (int i = 0; i < bs.length; i++) {
			bs[i] = (byte) ((Byte.parseByte(data.substring(i * 2, i * 2 + 1)) << 4) | Byte.parseByte(data.substring(
					i * 2 + 1,
					i * 2 + 2)));
		}
		return bs;
	}

	private static void newCommand(String instanceName, String classname) {
		try {
			System.err.println("(jvm) creating new instance '" + instanceName + "' of type '" + classname + "'");
			Class<?> clazz = Class.forName(classname, true, cl);
			instances.put(instanceName, clazz.newInstance());
			System.err.println("(jvm) instance successfully created!!");
		}
		catch (Exception e) {
			System.out.println("(jvm) failed to create instance " + e.getMessage() + "!!");
			e.printStackTrace(System.out);
		}

	}
}
