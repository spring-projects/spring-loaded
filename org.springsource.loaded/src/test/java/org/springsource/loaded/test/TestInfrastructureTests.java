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

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.springsource.loaded.Utils;
import org.springsource.loaded.test.infra.TestClassLoader;


/**
 * Checks the behaviour of the infrastructure being used for tests.
 * 
 * @author Andy Clement
 */
public class TestInfrastructureTests extends SpringLoadedTests {

	// Just attempt to access something in the testdata project through the classloader
	@Test
	public void loader() {
		TestClassLoader tcl = new TestClassLoader(toURLs(TestDataPath), this.getClass().getClassLoader());
		URL url = tcl.findResource("data/SimpleClass.class");
		Assert.assertNotNull(url);
		url = tcl.findResource("data/MissingClass.class");
		Assert.assertNull(url);
	}

	// Check loading of data as a byte array
	// Size changed here from 331 to 394 when switched to AspectJ project for testcode!
	@Test
	public void loading() {
		TestClassLoader tcl = new TestClassLoader(toURLs(TestDataPath), this.getClass().getClassLoader());
		byte[] classdata = Utils.loadClassAsBytes(tcl, "data.SimpleClass");
		Assert.assertNotNull(classdata);
		Assert.assertEquals(394, classdata.length);
	}

}
