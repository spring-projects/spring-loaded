/*
 * Copyright 2010-2012 VMware and contributors
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

package org.springsource.loaded.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils;
import org.springsource.loaded.test.infra.TestClassloaderWithRewriting;


/**
 * Test reloading of enum types.
 * 
 * @author Andy Clement
 * @since 0.8.4
 */
public class EnumTests extends SpringLoadedTests {

	@Before
	public void setUp() throws Exception {
		super.setup();
		GlobalConfiguration.reloadMessages = true;
		binLoader = new TestClassloaderWithRewriting(true, true, true);
	}

	@After
	public void tearDown() throws Exception {
		super.teardown();
		ensureCaptureOff();
		GlobalConfiguration.reloadMessages = false;
	}

	/**
	 * Test new values are visible on reload
	 */
	@Test
	public void testEnums1() throws Exception {
		String t = "enumtests.Colours";
		String runner = "enumtests.RunnerA";

		// Class<?> enumClazz = 
		binLoader.loadClass(t);
		Class<?> runnerClazz = binLoader.loadClass(runner);
		assertNotNull(runnerClazz);

		// Check we loaded it as reloadable
		ReloadableType rtype = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(t), false);
		assertNotNull(rtype);

		String output = runMethodAndCollectOutput(runnerClazz, "run1");
		assertContains("Red", output);
		assertContains("Green", output);
		assertContains("Blue", output);
		assertContains("[Red Green Blue]", output);
		assertContains("value count = 3", output);

		rtype.loadNewVersion(retrieveRename(t, t + "2"));
		output = runMethodAndCollectOutput(runnerClazz, "run1");
		assertContains("Red", output);
		assertContains("Green", output);
		assertContains("Blue", output);
		assertContains("[Red Green Blue Yellow]", output);
		assertContains("value count = 4", output);
	}

	/**
	 * More elaborate reloading. The enum implements an interface, the interface is changed (method added) on reload and
	 * the enum reloaded to implement the new method.
	 */
	@Test
	public void testEnums2() throws Exception {
		String t = "enumtests.ColoursB";
		String runner = "enumtests.RunnerB";
		String intface = "enumtests.Intface";

		binLoader.loadClass(t);
		Class<?> runnerClazz = binLoader.loadClass(runner);
		assertNotNull(runnerClazz);
		String output = runMethodAndCollectOutput(runnerClazz, "run1");
		assertContains("[111 222 333]", output);

		// Check we loaded it as reloadable
		ReloadableType rtypeEnum = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(t), false);
		assertNotNull(rtypeEnum);
		ReloadableType rtypeRunner = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(runner),
				false);
		assertNotNull(rtypeRunner);
		ReloadableType rtypeIntface = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(intface),
				false);
		assertNotNull(rtypeIntface);

		rtypeEnum.loadNewVersion(retrieveRename(t, t + "2"));
		output = runMethodAndCollectOutput(runnerClazz, "run1");
		assertContains("[222 444 666 888]", output);
		assertContains("value count = 4", output);

		// Load a new version of the interface and the runner and the enum (which implements the interface)
		String[] renames = new String[] { "enumtests.ColoursB3:enumtests.ColoursB",
			"enumtests.Intface3:enumtests.Intface" };
		rtypeIntface.loadNewVersion("3", retrieveRename(intface, intface + "3"));
		rtypeEnum.loadNewVersion("3", retrieveRename(t, t + "3", renames));
		rtypeRunner.loadNewVersion("3", retrieveRename(runner, runner + "3", renames));
		output = runMethodAndCollectOutput(runnerClazz, "run1");

		// one less value and using new interface method (getDoubleIntValue())
		assertContains("[222 444 666]", output);
		assertContains("value count = 3", output);
	}

	@Test
	public void testEnums3() throws Exception {
		String t = "enumtests.ColoursB";
		String runner = "enumtests.RunnerB";

		binLoader.loadClass(t);
		Class<?> runnerClazz = binLoader.loadClass(runner);
		assertNotNull(runnerClazz);
		String output = runMethodAndCollectOutput(runnerClazz, "run1");
		assertContains("[111 222 333]", output);

		// Check we loaded it as reloadable
		ReloadableType rtype = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(t), false);
		assertNotNull(rtype);

		rtype.loadNewVersion(retrieveRename(t, t + "2"));
		output = runMethodAndCollectOutput(runnerClazz, "run1");
		assertContains("[222 444 666 888]", output);
		assertContains("value count = 4", output);
	}

	/**
	 * There is no need to intercept Class.getEnumConstants() - that method uses a cached enumConstants array that is
	 * cleared when an enum type is reloaded
	 */
	@Test
	public void testEnumsReflection() throws Exception {
		String t = "enumtests.ColoursC";
		String runner = "enumtests.RunnerC";

		Class<?> clazz = binLoader.loadClass(t);
		assertNotNull(clazz);
		Class<?> runnerClazz = binLoader.loadClass(runner);
		assertNotNull(runnerClazz);
		String output = runMethodAndCollectOutput(runnerClazz, "callGetEnumConstants");
		assertContains("[Red Green Blue Orange Yellow]", output);
		assertContains("value count = 5", output);

		// Check we loaded it as reloadable
		ReloadableType rtype = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(t), false);
		assertNotNull(rtype);

		rtype.loadNewVersion(retrieveRename(t, t + "2"));
		output = runMethodAndCollectOutput(runnerClazz, "callGetEnumConstants");
		assertContains("[Red Green Blue Orange Yellow Magenta Cyan]", output);
		assertContains("value count = 7", output);
	}

	/**
	 * Test the valueOf(String) method added to enum types, which delegates to the valueOf(EnumClass,String) on Enum.
	 * Need to clear enumConstantDirectory in Class.class.
	 */
	@Test
	public void testEnumsValueOf1() throws Exception {
		String t = "enumtests.ColoursC";
		String runner = "enumtests.RunnerC";

		Class<?> clazz = binLoader.loadClass(t);
		assertNotNull(clazz);
		Class<?> runnerClazz = binLoader.loadClass(runner);
		assertNotNull(runnerClazz);
		String output = runMethodAndCollectOutput(runnerClazz, "callValueOf1");
		assertContains("[Red Green Blue Orange Yellow]", output);
		assertContains("value count = 5", output);

		// Check we loaded it as reloadable
		ReloadableType rtype = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(t), false);
		assertNotNull(rtype);

		rtype.loadNewVersion(retrieveRename(t, t + "2", t + "2:" + t));
		output = runMethodAndCollectOutput(runnerClazz, "callValueOf1");
		assertContains("[Red Green Blue Orange Yellow Magenta Cyan]", output);
		assertContains("value count = 7", output);
	}

	/**
	 * Test the Enum.valueOf(EnumClass,String) - needs enumConstantDirectory clearing on reload.
	 */
	@Test
	public void testEnumsValueOf2() throws Exception {
		String t = "enumtests.ColoursC";
		String runner = "enumtests.RunnerC";

		Class<?> clazz = binLoader.loadClass(t);
		assertNotNull(clazz);
		Class<?> runnerClazz = binLoader.loadClass(runner);
		assertNotNull(runnerClazz);
		String output = runMethodAndCollectOutput(runnerClazz, "callValueOf2");
		assertContains("[Red Green Blue Orange Yellow]", output);
		assertContains("value count = 5", output);

		// Check we loaded it as reloadable
		ReloadableType rtype = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(t), false);
		assertNotNull(rtype);

		rtype.loadNewVersion(retrieveRename(t, t + "2", t + "2:" + t));
		output = runMethodAndCollectOutput(runnerClazz, "callValueOf2");
		assertContains("[Red Green Blue Orange Yellow Magenta Cyan]", output);
		assertContains("value count = 7", output);
	}

	/**
	 * Old constructor deleted, new one added. This checks that the rewritten clinit that initializes the set of values
	 * is working.
	 */
	@Test
	public void testEnumsConstructor() throws Exception {
		String t = "enumtests.ColoursD";
		String runner = "enumtests.RunnerD";

		binLoader.loadClass(t);
		Class<?> runnerClazz = binLoader.loadClass(runner);
		assertNotNull(runnerClazz);
		String output = runMethodAndCollectOutput(runnerClazz, "run1");
		assertContains("[Red 1111 0 Green 2222 1 Blue 3333 2]", output);

		// Check we loaded it as reloadable
		ReloadableType rtype = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(t), false);
		assertNotNull(rtype);
		ReloadableType rtypeRunner = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(runner),
				false);
		assertNotNull(rtypeRunner);

		// Changes from ints to chars
		rtype.loadNewVersion(retrieveRename(t, t + "2"));

		rtypeRunner.loadNewVersion(retrieveRename(runner, runner + "2", "enumtests.ColoursD2:enumtests.ColoursD"));
		output = runMethodAndCollectOutput(runnerClazz, "run1");
		assertContains("[Red a 0 Green b 1 Blue c 2]", output);
		assertContains("value count = 3", output);
	}

	// TODO for this to work properly need support for very large enums (static method splitting)
	// currently this will pass because we don't make enum values reloadable (changeable) if there are more than 1000 in an enum type
	// it should be possible to reload if the values don't change, but a NSFE will be thrown if the values do change.
	@Test
	public void testLargeEnum() throws Exception {
		String t = "enumtests.ColoursE";
		String runner = "enumtests.RunnerE";

		binLoader.loadClass(t);
		Class<?> runnerClazz = binLoader.loadClass(runner);
		assertNotNull(runnerClazz);
		String output = runMethodAndCollectOutput(runnerClazz, "run1");
		assertContains("[Red 1111 0 Green 2222 1 Blue 3333 2]", output);

		// Check we loaded it as reloadable
		ReloadableType rtype = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(t), false);
		// Utils.dump(rtype.getSlashedName(), rtype.bytesLoaded);
		assertNotNull(rtype);
		ReloadableType rtypeRunner = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(runner),
				false);
		assertNotNull(rtypeRunner);

		assertTrue(rtype.loadNewVersion("1", rtype.bytesInitial));

		output = runMethodAndCollectOutput(runnerClazz, "run1");
		assertContains("[Red 1111 0 Green 2222 1 Blue 3333 2]", output);

		// Changes from ints to chars
		rtype.loadNewVersion(retrieveRename(t, t + "2"));
		// expect this in the console:
		//			Caused by: java.lang.NoSuchFieldError: JOE1
		//			at enumtests.ColoursE$$E2. enum constant initialization$2(ColoursE2.java:1)
		//			at enumtests.ColoursE$$E2.___clinit___(ColoursE2.java:3)
	}
}
