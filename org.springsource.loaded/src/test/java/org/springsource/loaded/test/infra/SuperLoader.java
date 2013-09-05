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
package org.springsource.loaded.test.infra;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.springsource.loaded.LoadtimeInstrumentationPlugin;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils;
import org.springsource.loaded.agent.SpringLoadedPreProcessor;
import org.springsource.loaded.test.TestUtils;


/**
 * Should be used in concert with SubLoader - these two enable us to look at reloading across classloader boundaries.
 * 
 * @author Andy Clement
 */
public class SuperLoader extends ClassLoader {

	// @formatter:off
	static String[] folders = new String[] { 
		TestUtils.getPathToClasses("../org.springsource.loaded.testdata.superloader")
		};
	static String[] jars = new String[] { 
		"../org.springsource.loaded.testdata.groovy/groovy-1.8.2.jar"
		};
	// @formatter:on

	public SuperLoader() {
		jars = new String[] { "../org.springsource.loaded.testdata.groovy/groovy-1.8.2.jar" };
	}

	public SuperLoader(String... jars) {
		SuperLoader.jars = jars;
	}

	public SuperLoader(String metainfFolder) {
		String[] newFolders = new String[2];
		newFolders[0] = folders[0];
		newFolders[1] = "../org.springsource.loaded.testdata/" + metainfFolder;
		folders = newFolders;
	}

	public SuperLoader(String metainfFolder, boolean b) {
		String[] newFolders = new String[3];
		newFolders[0] = folders[0];
		newFolders[1] = "../org.springsource.loaded.testdata/" + metainfFolder;
		newFolders[2] = TestUtils.getPathToClasses("../org.springsource.loaded.testdata");
		folders = newFolders;
	}

	public Class<?> findClass(String name) throws ClassNotFoundException {
		//		System.out.println(">> SuperLoader.findClass(" + name + ")");
		Class<?> c = null;
		// Look in the filesystem first
		try {
			for (int i = 0; i < folders.length; i++) {
				File f = new File(folders[i], name.replace('.', '/') + ".class");
				if (f.exists()) {
					byte[] data = Utils.loadBytesFromStream(new FileInputStream(f));
					TypeRegistry tr = TypeRegistry.getTypeRegistryFor(this);
					if (tr != null) {
						// not yet doing this - the testcase tends to do any client side rewriting for this
						ReloadableType rtype = tr.addType(name, data);
						data = rtype.bytesLoaded;
					}
					c = defineClass(name, data, 0, data.length);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (c == null) {
			// Try the jar
			try {
				for (int i = 0; i < jars.length; i++) {
					// System.out.println("Checking jar for "+name);
					ZipFile zipfile = new ZipFile(jars[i]);
					String slashedClassName = name.replace('.', '/');
					ZipEntry zipentry = zipfile.getEntry(slashedClassName + ".class");
					if (zipentry != null) {
						byte[] data = Utils.loadBytesFromStream(zipfile.getInputStream(zipentry));
						TypeRegistry tr = TypeRegistry.getTypeRegistryFor(this);
						if (tr != null) {

							// Give the plugins a chance to rewrite stuff too
							for (org.springsource.loaded.Plugin plugin : SpringLoadedPreProcessor.getGlobalPlugins()) {
								if (plugin instanceof LoadtimeInstrumentationPlugin) {
									LoadtimeInstrumentationPlugin loadtimeInstrumentationPlugin = (LoadtimeInstrumentationPlugin) plugin;
									if (loadtimeInstrumentationPlugin.accept(slashedClassName, this, null, data)) {
										data = loadtimeInstrumentationPlugin.modify(slashedClassName, this, data);
									}
								}
							}

							//System.out.println("Transforming " + name);
							data = tr.methodCallRewrite(data);
						}

						c = defineClass(name, data, 0, data.length);
						break;
					}
				}
				// zipfile.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (c == null) {
			throw new ClassNotFoundException(name);
		}
		return c;
	}

	@Override
	public URL findResource(String name) {
		try {
			// System.out.println("Find resource for "+name);
			// Look in the folders we care about
			for (int i = 0; i < folders.length; i++) {
				File file = new File(folders[i], name);
				//				System.out.println(file.exists());
				if (file.exists()) {
					return file.toURI().toURL();
				}
			}
			for (int i = 0; i < jars.length; i++) {
				ZipFile zipfile = new ZipFile(jars[i]);
				ZipEntry zipentry = zipfile.getEntry(name);
				if (zipentry != null) {
					return new URL("jar:file:" + new File(jars[i]).getCanonicalPath() + "!/" + name);
				}
				zipfile.close();
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		//		List<URL> urls = super.findResources(name)
		final List<URL> urls = new ArrayList<URL>();
		try {
			// System.out.println("Find resource for "+name);
			// Look in the folders we care about
			for (int i = 0; i < folders.length; i++) {
				File file = new File(folders[i], name);
				//				System.out.println(file.exists());
				if (file.exists()) {
					urls.add(file.toURI().toURL());
				}
			}
			//			for (int i = 0; i < jars.length; i++) {
			//				ZipFile zipfile = new ZipFile(jars[i]);
			//				ZipEntry zipentry = zipfile.getEntry(name);
			//				if (zipentry != null) {
			//					return new URL("jar:file:" + new File(jars[i]).getCanonicalPath() + "!/" + name);
			//				}
			//				zipfile.close();
			//			}
			return new Enumeration<URL>() {

				int counter = 0;

				public boolean hasMoreElements() {
					return counter < urls.size();
				}

				public URL nextElement() {
					return urls.get(counter++);
				}

			};
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}