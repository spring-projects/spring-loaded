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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

public class TestClassLoader extends URLClassLoader {

	private static int idCt = 0;

	private int id = idCt++;

	public TestClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	@Override
	protected synchronized Class<?> loadClass(String arg0, boolean arg1) throws ClassNotFoundException {
		//		System.out.println(this+" being asked to load class "+arg0+","+arg1);
		return super.loadClass(arg0, arg1);
	}

	@Override
	public URL getResource(String name) {
		//		System.out.println(this+" being asked to getResource "+name);
		return super.getResource(name);
	}

	@Override
	public URL findResource(String arg0) {
		//		System.out.println(this+" being asked to find resource "+arg0);
		return super.findResource(arg0);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		//		System.out.println(this+" being asked to find class "+name);
		return super.findClass(name);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		//		System.out.println(this + " loading "+name);
		return super.loadClass(name);
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		//		System.out.println(this+" being asked to find resources "+name);
		return super.findResources(name);
	}

	public Class<?> defineTheClass(String name, byte[] bytes) {
		//		System.out.println(this + " defining "+name);
		return super.defineClass(name, bytes, 0, bytes.length);
	}

	@Override
	public String toString() {
		return "ClassLoader( " + id + " )";
	}

}
