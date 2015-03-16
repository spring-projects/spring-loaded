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

package org.springsource.loaded;

/**
 * API for directly interacting with SpringLoaded.
 * 
 * @author Andy Clement
 * @since 0.8.0
 */
public class SpringLoaded {

	/**
	 * Force a reload of an existing type.
	 * 
	 * @param clazz the class to be reloaded
	 * @param newbytes the data bytecode data to reload as the new version
	 * @return int return code: 0 is success. 1 is unknown classloader, 2 is unknown type (possibly not yet loaded). 3
	 *         is reload event failed. 4 is exception occurred.
	 */
	public static int loadNewVersionOfType(Class<?> clazz, byte[] newbytes) {
		return loadNewVersionOfType(clazz.getClassLoader(), clazz.getName(), newbytes);
	}

	/**
	 * Force a reload of an existing type.
	 * 
	 * @param classLoader the classloader that was used to load the original form of the type
	 * @param dottedClassname the dotted name of the type being reloaded, e.g. com.foo.Bar
	 * @param newbytes the data bytecode data to reload as the new version
	 * @return int return code: 0 is success. 1 is unknown classloader, 2 is unknown type (possibly not yet loaded). 3
	 *         is reload event failed. 4 is exception occurred.
	 */
	public static int loadNewVersionOfType(ClassLoader classLoader, String dottedClassname, byte[] newbytes) {
		try {
			// Obtain the type registry of interest
			TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(classLoader);
			if (typeRegistry == null) {
				return 1;
			}
			// Find the reloadable type
			ReloadableType reloadableType = typeRegistry.getReloadableType(dottedClassname.replace('.', '/'));
			if (reloadableType == null) {
				return 2;
			}
			// Create a unique version tag for this reload attempt
			String tag = Utils.encode(System.currentTimeMillis());
			boolean reloaded = reloadableType.loadNewVersion(tag, newbytes);
			return reloaded ? 0 : 3;
		}
		catch (Exception e) {
			e.printStackTrace();
			return 4;
		}
	}
}
