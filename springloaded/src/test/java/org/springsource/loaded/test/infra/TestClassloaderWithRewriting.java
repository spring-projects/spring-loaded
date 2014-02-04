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
import java.net.URLClassLoader;
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
 * The groovy infrastructure caches lots of stuff about classes during execution. Although we can actively clear our some of this,
 * we do need to modify the groovy infrastructure so that reflective calls are intercepted. Unlike TestClassLoader which is a simple
 * URLClassLoader, this classloader extends ClassLoader, allowing us to get in there and modify the bytes with an interception
 * rewrite.
 * 
 * @author Andy Clement
 */
public class TestClassloaderWithRewriting extends ClassLoader {

	private boolean useRegistry = false;

	// @formatter:off
	static String[] folders = new String[] { 
		TestUtils.getPathToClasses("../testdata-groovy") 
		};
	static String[] jars = new String[] { 
		"../testdata-groovy/groovy-1.8.2.jar"
		};
	// @formatter:on

	public TestClassloaderWithRewriting() {
		jars = new String[] { "../testdata-groovy/groovy-1.8.2.jar" };
	}

	public TestClassloaderWithRewriting(String metainfFolder) {
		String[] newFolders = new String[2];
		newFolders[0] = folders[0];
		newFolders[1] = "../testdata/" + metainfFolder;
		folders = newFolders;
		jars = new String[] { "../testdata-groovy/groovy-1.8.2.jar" };
	}

	public TestClassloaderWithRewriting(String metainfFolder, boolean b) {
		String[] newFolders = new String[4];
		newFolders[0] = folders[0];
		newFolders[1] = "../testdata/" + metainfFolder;
		newFolders[2] = TestUtils.getPathToClasses("../testdata");
		newFolders[3] = TestUtils.getPathToClasses("../testdata-plugin");
		folders = newFolders;
		jars = new String[] { "../testdata-groovy/groovy-1.8.2.jar" };
	}

	public TestClassloaderWithRewriting(String metainfFolder, boolean b, boolean useRegistry, URLClassLoader classLoader) {
		super(classLoader);
		String[] newFolders = new String[3];
		newFolders[0] = folders[0];
		newFolders[1] = "../testdata/" + metainfFolder;
		newFolders[2] = TestUtils.getPathToClasses("../testdata");
		folders = newFolders;
		this.useRegistry = useRegistry;
		jars = new String[] { "../testdata-groovy/groovy-1.8.2.jar" };
	}

	public TestClassloaderWithRewriting(boolean b, boolean useRegistry, boolean addCglib) {
		String[] newFolders = new String[2];
		newFolders[0] = folders[0];
		newFolders[1] = TestUtils.getPathToClasses("../testdata");
		folders = newFolders;
		this.useRegistry = useRegistry;
		jars = new String[] { "../testdata/lib/cglib-nodep-2.2.jar" };
	}

	public Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> c = null;
		// Look in the filesystem first
		try {
			for (int i = 0; i < folders.length; i++) {
				File f = new File(folders[i], name.replace('.', '/') + ".class");
				if (f.exists()) {
					byte[] data = Utils.loadBytesFromStream(new FileInputStream(f));
					TypeRegistry tr = TypeRegistry.getTypeRegistryFor(this);
					if (tr != null) {
						if (useRegistry) {
							ReloadableType rt = tr.addType(name, data);
							if (rt == null) {
								System.out.println("Not made reloadable " + name);
							} else {
								return rt.getClazz();
							}
						}
						// not yet doing this - the testcase tends to do any client side rewriting for this
					}
					c = defineClass(name, data, 0, data.length);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Exception ex = null;
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

							// TODO make conditional?
							if (slashedClassName.equals("net/sf/cglib/core/ReflectUtils")) {
								// intercept call to defineclass so we can make it reloadable.  In practice this isn't necessary
								// as the springloadedpreprocessor will get called
								data = RewriteReflectUtilsDefineClass.rewriteReflectUtilsDefineClass(data);
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
				ex = e;
				e.printStackTrace(System.err);
			}
		}

		if (c == null) {
			if (ex!=null) {
				throw new ClassNotFoundException(name,ex);
			}
			else {
				throw new ClassNotFoundException(name);
			}
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