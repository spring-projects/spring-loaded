/*
 * Copyright 2013 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springsource.loaded.perf.test;

import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Ignore;
import org.junit.Test;
import org.springsource.loaded.test.SpringLoadedTests;
import org.springsource.loaded.test.infra.TestClassLoader;

/**
 * Check the performance of weaving code.
 * 
 * @author Andy Clement
 * @since 1.1.5
 */
public class WeavingPerformanceTests extends SpringLoadedTests {

	protected String ExpressionsJar = "../testdata/lib/spring-expression-4.0.0.M3.jar";

	/**
	 * Work in progress...
	 * 
	 * Process a jar file and pretend that everything in it is reloadable. How long does it take?
	 * 
	 * Possible approaches here: - load all the types through a reloading classloader? This will not be testing the
	 * agent specifically, just the lower level infrastructure - Run this test inside a JVM that has reloading turned on
	 * This would test the agent.
	 * 
	 * 
	 */
	@Ignore
	@Test
	public void jar() throws Exception {

		TestClassLoader tcl = new TestClassLoader(toURLs(ExpressionsJar), this.getClass().getClassLoader());
		ZipFile zf = new ZipFile(ExpressionsJar);
		Enumeration<? extends ZipEntry> entries = zf.entries();
		while (entries.hasMoreElements()) {
			ZipEntry ze = entries.nextElement();
			System.out.println(ze.getName());
			String name = ze.getName();
			if (name.endsWith(".class")) {
				name = name.substring(0, name.length() - 6);
				tcl.loadClass(name.replaceAll("/", "."));
			}
		}
		//		URL url = tcl.findResource("data/SimpleClass.class");
		//		Assert.assertNotNull(url);
		//		url = tcl.findResource("data/MissingClass.class");
		//		Assert.assertNull(url);

		//		TypeRegistry typeRegistry = getTypeRegistry("data.SimpleClass");
		//		byte[] sc = loadBytesForClass("data.SimpleClass");
		//		ReloadableType rtype = new ReloadableType("data.SimpleClass", sc, 1, typeRegistry, null);
		//
		//		assertEquals(1, rtype.getId());
		//		assertEquals("data.SimpleClass", rtype.getName());
		//		assertEquals("data/SimpleClass", rtype.getSlashedName());
		//		assertNotNull(rtype.getTypeDescriptor());
		//		assertEquals(typeRegistry, rtype.getTypeRegistry());

	}
}
