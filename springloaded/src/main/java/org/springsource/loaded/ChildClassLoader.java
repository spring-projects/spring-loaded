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

import java.net.URL;
import java.net.URLClassLoader;

/**
 * The ChildClassLoader will load the generated dispatchers and executors which change for each reload. Instances of
 * this can be discarded which will cause 'old' dispatchers/executors to be candidates for GC too (avoiding memory leaks
 * when lots of reloads occur).
 */
public class ChildClassLoader extends URLClassLoader {

	private static URL[] NO_URLS = new URL[0];

	private int definedCount = 0;

	public ChildClassLoader(ClassLoader classloader) {
		super(NO_URLS, classloader);
	}

	public Class<?> defineClass(String name, byte[] bytes) {
		definedCount++;
		return super.defineClass(name, bytes, 0, bytes.length);
	}

	public int getDefinedCount() {
		return definedCount;
	}

}
