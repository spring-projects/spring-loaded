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

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springsource.loaded.test.ReloadingJVM.Output;


public class SpringLoadedTestsInSeparateJVM extends SpringLoadedTests {

	ReloadingJVM jvm;

	@Before
	public void setup() throws Exception {
		super.setup();
		jvm = ReloadingJVM.launch();
	}

	@After
	public void teardown() {
		jvm.shutdown();
	}

	@Ignore // unfinished
	// Launch a vm and get it to run something!
	@Test
	public void testEcho() throws Exception {
		Output result = jvm.echo("hello");
		assertStdout("hello", result);
	}

	@Ignore // unfinished
	@Test
	public void testRunClass() throws Exception {
		assertStdout("jvmtwo.Runner.run() running", jvm.run("jvmtwo.Runner"));
	}

	@Ignore // unfinished
	@Test
	public void testCreatingAndInvokingMethodsOnInstance() throws Exception {
		assertStderrContains("creating new instance 'a' of type 'jvmtwo.Runner'", jvm.newInstance("a", "jvmtwo.Runner"));
		assertStdout("jvmtwo.Runner.run1() running", jvm.call("a", "run1"));
	}

	//	@Test
	//	public void testReloadingInOtherVM() throws Exception {
	//		jvm.newInstance("a", "remote.One");
	//		assertStdout("first load", jvm.call("a", "run"));
	//		try {
	//			Thread.sleep(20000);
	//		} catch (Exception e) {
	//		}
	//
	//		// Need to load a new version into that remote JVM !
	//		// send the bytes of the new version
	//
	//		byte[] newbytes = retrieveRename("remote.One", "remote.One2");
	//		jvm.reload("remote.One", newbytes);
	//
	//		assertStdout("second2 load", jvm.call("a", "run"));
	//	}

	// ---

	private void assertStdout(String expectedStdout, Output actualOutput) {
		if (!expectedStdout.equals(actualOutput.stdout)) {
			//			assertEquals(expectedStdout, actualOutput.stdout);
			fail("Expected stdout '" + expectedStdout + "' not found in \n" + actualOutput.toString());
		}
	}

	private void assertStderrContains(String expectedStderrContains, Output actualOutput) {
		if (actualOutput.stderr.indexOf(expectedStderrContains) == -1) {
			fail("Expected stderr to contain '" + expectedStderrContains + "'\n" + actualOutput.toString());
		}
	}

}
