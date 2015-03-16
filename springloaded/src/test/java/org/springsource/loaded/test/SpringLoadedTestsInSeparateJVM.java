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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springsource.loaded.test.ReloadingJVM.JVMOutput;

/**
 * These tests use a harness that forks a JVM with the agent attached, closely simulating a real environment. The forked
 * process is running a special class that can be sent commands.
 * 
 * @author Andy Clement
 */
public class SpringLoadedTestsInSeparateJVM extends SpringLoadedTests {

	private static ReloadingJVM jvm;

	@BeforeClass
	public static void startJVM() throws Exception {
		//		jvm = ReloadingJVM.launch("verbose;explain");
		jvm = ReloadingJVM.launch("");
	}

	@AfterClass
	public static void stopJVM() {
		jvm.shutdown();
	}

	@After
	public void teardown() throws Exception {
		super.teardown();
		jvm.clearTestdataDirectory();
	}

	@Test
	public void testEcho() throws Exception {
		JVMOutput result = jvm.echo("hello");
		assertStdout("hello", result);
	}

	@Test
	public void testRunClass() throws Exception {
		JVMOutput output = jvm.run("jvmtwo.Runner");
		assertStdout("jvmtwo.Runner.run() running", output);
	}

	@Test
	public void githubIssue34() throws Exception {
		jvm.copyToTestdataDirectory("issue34.Interface1");
		jvm.copyToTestdataDirectory("issue34.Interface2");
		jvm.copyToTestdataDirectory("issue34.Implementation1");
		jvm.copyToTestdataDirectory("issue34.Implementation2");
		jvm.copyToTestdataDirectory("issue34.Implementation3");
		JVMOutput output = jvm.run("issue34.Implementation3");
		assertStdout("Hello World!\n", output);
	}

	@Test
	public void serialization() throws Exception {
		jvm.copyToTestdataDirectory("remote.Serialize");
		jvm.copyToTestdataDirectory("remote.Person");
		JVMOutput output = null;

		// When the Serialize class is run directly, we see: byteinfo:len=98:crc=c1047cf6
		// When run via the non separate JVM test, we see: byteinfo:len=98:crc=7e07276a
		// When run here, we see: byteinfo:len=98:crc=c1047cf6

		output = jvm.run("remote.Serialize");
		assertStdoutContains("check ok\n", output);

		// Load new Person
		jvm.updateClass("remote.Person", retrieveRename("remote.Person", "remote.Person2"));
		pause(2);

		output = jvm.run("remote.Serialize");
		assertStdoutContains("check ok\n", output);

		// Load original Person
		jvm.updateClass("remote.Person", loadBytesForClass("remote.Person"));
		pause(2);

		output = jvm.run("remote.Serialize");
		assertStdoutContains("check ok\n", output);
	}

	// Deserializing something serialized earlier
	@Test
	public void serialization2() throws Exception {
		jvm.copyToTestdataDirectory("remote.Serialize");
		jvm.copyToTestdataDirectory("remote.Person");
		JVMOutput output = null;

		output = jvm.run("remote.Serialize");
		assertStdoutContains("check ok\n", output);

		jvm.newInstance("a", "remote.Serialize");
		JVMOutput jo = jvm.call("a", "checkPredeserializedData");
		assertStdoutContains("Pre-serialized form checked ok\n", jo);
	}

	// Deserialize a groovy closure
	@Test
	public void serializationGroovy() throws Exception {
		//		debug();
		jvm.copyToTestdataDirectory("remote.SerializeG");
		jvm.copyToTestdataDirectory("remote.FakeClosure");

		// Notes on serialization
		// When SerializeG run standalone, reports byteinfo:len=283:crc=245529d9
		// When run in agented JVM, reports byteinfo:len=283:crc=245529d9

		jvm.newInstance("a", "remote.SerializeG");
		JVMOutput jo = jvm.call("a", "checkPredeserializedData");
		assertStdoutContains("Pre-serialized groovy form checked ok\n", jo);
	}

	@Test
	public void githubIssue34_2() throws Exception {
		jvm.copyToTestdataDirectory("issue34.InnerEnum$sorters");
		jvm.copyToTestdataDirectory("issue34.InnerEnum$MyComparator");
		jvm.copyToTestdataDirectory("issue34.InnerEnum$sorters$1");
		JVMOutput output = jvm.run("issue34.InnerEnum");
		assertStdout("Hello World!\n", output);
	}

	@Test
	public void testCreatingAndInvokingMethodsOnInstance() throws Exception {
		assertStderrContains("creating new instance 'a' of type 'jvmtwo.Runner'", jvm.newInstance("a", "jvmtwo.Runner"));
		assertStdout("jvmtwo.Runner.run1() running", jvm.call("a", "run1"));
	}

	/* careful with this test - when the forked JVM takes a while the output can interfere with later tests, needs fixing up
	 *
	 */
	@Ignore
	@Test
	public void reloadedPerformance() throws Exception {
		//		debug();
		jvm.newInstance("a", "remote.Perf1");
		JVMOutput jo = jvm.call("a", "time"); // 75ms
		jo = jvm.call("a", "time"); // 75ms
		pause(5);
		jvm.updateClass("remote.Perf1", retrieveRename("remote.Perf1", "remote.Perf2"));
		pause(2);
		// In Perf2 the static method is gone, why does it give us a NSME?
		jo = jvm.call("a", "time"); // 150ms
		pause(5);
	}

	@SuppressWarnings("unused")
	private final static void debug() {
		jvm.shutdown();
		jvm = ReloadingJVM.launch("", true);
	}

	@SuppressWarnings("unused")
	private final static void debug(String options) {
		jvm = ReloadingJVM.launch(options, true);
	}

	@Test
	public void testReloadingInOtherVM() throws Exception {
		jvm.newInstance("b", "remote.One");
		assertStdout("first", jvm.call("b", "run"));
		pause(1);
		jvm.updateClass("remote.One", retrieveRename("remote.One", "remote.One2"));
		pause(2);
		assertStdoutContains("second", jvm.call("b", "run"));
	}

	// TODO tidyup test data area after each test?
	// TODO flush/replace classloader in forked VM to clear it out after each test?

	// GRAILS-10411
	/**
	 * GRAILS-10411. The supertype is not reloadable, the subtype is reloadable and makes super calls to overridden
	 * methods.
	 */
	@Test
	public void testClassMakingSuperCalls() throws Exception {
		String supertype = "grails.Top";
		String subtype = "foo.Controller";
		jvm.copyToTestdataDirectory(supertype);
		jvm.copyToTestdataDirectory(subtype);
		JVMOutput jo = jvm.newInstance("bb", subtype);
		System.out.println(jo);
		pause(1);
		assertStdout("Top.foo() running\nController.foo() running\n", jvm.call("bb", "foo"));
		pause(1);
		jvm.updateClass(subtype, retrieveRename(subtype, subtype + "2"));
		waitForReloadToOccur();
		jo = jvm.call("bb", "foo");
		assertStdoutContains("Top.foo() running\nController.foo() running again!\n", jo);
	}

	/**
	 * GRAILS-10411. The supertype is not reloadable, the subtype is reloadable and makes super calls to overridden
	 * methods. This time the supertype method is protected.
	 */
	@Test
	public void testClassMakingSuperCalls2() throws Exception {
		String supertype = "grails.TopB";
		String subtype = "foo.ControllerB";
		jvm.copyToTestdataDirectory(supertype);
		jvm.copyToTestdataDirectory(subtype);
		jvm.newInstance("a", subtype);
		assertStdout("TopB.foo() running\nControllerB.foo() running\n", jvm.call("a", "foo"));
		jvm.updateClass(subtype, retrieveRename(subtype, subtype + "2"));
		waitForReloadToOccur();
		assertStdoutContains("TopB.foo() running\nControllerB.foo() running again!\n", jvm.call("a", "foo"));
	}

	// ---

	private void waitForReloadToOccur() {
		try {
			Thread.sleep(2000);
		}
		catch (Exception e) {
		}
	}


}
