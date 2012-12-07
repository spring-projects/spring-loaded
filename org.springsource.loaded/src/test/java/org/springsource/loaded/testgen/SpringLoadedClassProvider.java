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
package org.springsource.loaded.testgen;

import org.springsource.loaded.MethodInvokerRewriter;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.test.SpringLoadedTests;

import junit.framework.Assert;


/**
 * Implements IClassProvider for a test execution context where classes are loaded and instrumented by SpringLoaded.
 * 
 * @author kdvolder
 */
public class SpringLoadedClassProvider extends SpringLoadedTests implements IClassProvider {

	private TypeRegistry typeRegistry;

	public SpringLoadedClassProvider(String typeRegistryConfig) throws Exception {
		setup();
		typeRegistry = getTypeRegistry(typeRegistryConfig);
	}

	public Class<?> loadClassVersion(String typeName, String version) {
		if (typeRegistry.isReloadableTypeName(typeName.replace('.', '/'))) {
			ReloadableType rtype = reloadableClass(typeName);
			if (!version.equals("")) {
				int targetVersion = Integer.valueOf(version);
				int loadedVersion = 1; //By convention version numbers of reloaded types start from "002".
				while (loadedVersion < targetVersion) {
					String nextVersion = String.format("%03d", ++loadedVersion);
					reloadType(rtype, nextVersion);
				}
				Assert.assertEquals(targetVersion, loadedVersion);
			}
			return rtype.getClazz();
		} else {
			Assert.assertEquals("Non reloadable types shouldn't have versions!", "", version);
			return nonReloadableClass(typeName);
		}
	}

	protected ReloadableType reloadableClass(String className) {
		return typeRegistry.addType(className, loadBytesForClass(className));
	}

	protected Class<?> nonReloadableClass(String className) {
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, loadBytesForClass(className));
		return loadit(className, rewrittenBytes);
	}

	protected void reloadType(ReloadableType target, String version) {
		String targetClassName = target.getClazz().getName();
		target.loadNewVersion(version, retrieveRename(targetClassName, targetClassName + version));
	}

	/**
	 * Only for testing purposes
	 */
	public ClassLoader getClassLoader() {
		return binLoader;
	}
}
