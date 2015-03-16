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

import org.springsource.loaded.test.SpringLoadedTests;
import org.springsource.loaded.test.infra.TestClassLoader;


/**
 * Provides a test execution context that is 'just java'. It provides classes from new ClassLoader instance, so that
 * each test run can have its own fresh copy of the classes and not suffer from the fact that different tests may be
 * loading different versions of the same class.
 * 
 * @author kdvolder
 */
public class JustJavaClassProvider extends SpringLoadedTests implements IClassProvider {

	public JustJavaClassProvider() {
		binLoader = new TestClassLoader(toURLs(TestDataPath), ClassLoader.getSystemClassLoader());
	}

	public Class<?> loadClassVersion(String typeName, String version) {
		if (version == null || "".equals(version)) {
			return loadClass(typeName);
		}
		else {
			return loadit(typeName, retrieveRename(typeName, typeName + version));
		}
	}

	/**
	 * Provided only for testing purposes.
	 */
	public ClassLoader getClassLoader() {
		return binLoader;
	}

}
