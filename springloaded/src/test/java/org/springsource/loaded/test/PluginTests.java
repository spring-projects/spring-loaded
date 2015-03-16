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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.Plugins;
import org.springsource.loaded.ReloadEventProcessorPlugin;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.agent.ClassVisitingConstructorAppender;
import org.springsource.loaded.test.infra.TestClassloaderWithRewriting;


/**
 * Test the plugins (built in and user definable)
 * 
 * @author Andy Clement
 * @since 1.0
 */
public class PluginTests extends SpringLoadedTests {

	@Before
	public void setUp() throws Exception {
		super.setup();
		GlobalConfiguration.reloadMessages = true;
	}

	@After
	public void tearDown() throws Exception {
		super.teardown();
		ensureCaptureOff();
		GlobalConfiguration.reloadMessages = false;
	}

	@Test
	public void constructorAppender() throws Exception {
		ClassReader cr = new ClassReader(loadBytesForClass("plugins.One"));
		Target.reset();
		ClassVisitingConstructorAppender ca = new ClassVisitingConstructorAppender(Target.class.getName().replace('.',
				'/'), "foo");
		cr.accept(ca, 0);
		byte[] newbytes = ca.getBytes();
		Class<?> clazz = loadit("plugins.One", newbytes);
		clazz.newInstance();
		List<Object> instances = Target.collectedInstances;
		assertEquals(1, instances.size());
		assertTrue(instances.get(0).toString().startsWith("plugins.One"));
		Target.reset();
		clazz.getDeclaredConstructor(String.class, Integer.TYPE).newInstance("abc", 32);
		instances = Target.collectedInstances;
		assertEquals(1, instances.size());
		assertTrue(instances.get(0).toString().startsWith("plugins.One"));
		clazz.getDeclaredMethod("run").invoke(instances.get(0));
	}

	// Test a simple plugin that processes reload events
	@Test
	public void testSimplePlugin() throws Exception {
		binLoader = new TestClassloaderWithRewriting("meta1", true);
		String t = "simple.Basic";
		captureOn();
		TypeRegistry r = getTypeRegistry(t);
		String output = captureOff();
		assertContains("Instantiated ReloadEventProcessorPlugin1", output);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("hello", result.returnValue);
		captureOn();
		rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		output = captureOff();
		assertContains("Reloading: Loading new version of simple.Basic [2]", output);
		assertContains("ReloadEventProcessorPlugin1: reloadEvent(simple.Basic,simple.Basic,2)", output);
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("goodbye", result.returnValue);
	}

	// Test a simple plugin that processes reload events
	@Test
	public void testSimplePluginWithUnableToReloadEvent() throws Exception {
		binLoader = new TestClassloaderWithRewriting("meta1", true);
		String t = "simple.Basic";
		captureOn();
		TypeRegistry r = getTypeRegistry(t);
		String output = captureOff();
		assertContains("Instantiated ReloadEventProcessorPlugin1", output);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("hello", result.returnValue);
		captureOn();
		rtype.loadNewVersion("4", retrieveRename(t, t + "4"));
		output = captureOff();
		assertContains("ReloadEventProcessorPlugin1: unableToReloadEvent(simple.Basic,simple.Basic,4)", output);
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("hello", result.returnValue);
	}

	@SuppressWarnings("unused")
	@Test
	public void testServicesFileWithPluginCommentedOut() throws Exception {
		binLoader = new TestClassloaderWithRewriting("meta2", true);
		String t = "simple.Basic";
		captureOn();
		TypeRegistry r = getTypeRegistry(t);
		String output = captureOff();
		assertDoesNotContain("Instantiated ReloadEventProcessorPlugin1", output);
	}

	@SuppressWarnings("unused")
	@Test
	public void testServicesFileWithPluginAndNoNewline() throws Exception {
		binLoader = new TestClassloaderWithRewriting("meta3", true);
		String t = "simple.Basic";
		captureOn();
		TypeRegistry r = getTypeRegistry(t);
		String output = captureOff();
		assertContains("Instantiated ReloadEventProcessorPlugin1", output);
	}

	// registering a global plugin
	@Test
	public void testGlobalPluginRegistration() throws Exception {
		binLoader = new TestClassloaderWithRewriting("metaNotExist", true);
		String t = "simple.Basic";
		ReloadEventProcessorPlugin repp = new ReloadEventProcessorPlugin() {

			public void reloadEvent(String typename, Class<?> clazz, String encodedTimestamp) {
				System.out.println("Plugin: reloadEvent(" + typename + "," + clazz.getName() + "," + encodedTimestamp
						+ ")");
			}

			public boolean shouldRerunStaticInitializer(String typename, Class<?> clazz, String encodedTimestamp) {
				return false;
			}

		};
		try {
			Plugins.registerGlobalPlugin(repp);
			TypeRegistry r = getTypeRegistry(t);
			ReloadableType rtype = r.addType(t, loadBytesForClass(t));
			captureOn();
			rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
			String output = captureOff();
			System.out.println(output);
			assertContains("Reloading: Loading new version of simple.Basic [2]", output);
			assertUniqueContains("Plugin: reloadEvent(simple.Basic,simple.Basic,2)", output);
			result = runUnguarded(rtype.getClazz(), "run");
			assertEquals("goodbye", result.returnValue);
		}
		finally {
			Plugins.unregisterGlobalPlugin(repp);
		}
	}

	@Test
	public void testPluginRerunStaticInitializerRequest() throws Exception {
		binLoader = new TestClassloaderWithRewriting("metaNotExist", true);
		String t = "simple.Basic";
		ReloadEventProcessorPlugin repp = new ReloadEventProcessorPlugin() {

			public void reloadEvent(String typename, Class<?> clazz, String encodedTimestamp) {
				System.out.println("Plugin: reloadEvent(" + typename + "," + clazz.getName() + "," + encodedTimestamp
						+ ")");
			}

			public boolean shouldRerunStaticInitializer(String typename, Class<?> clazz, String encodedTimestamp) {
				System.out.println("Plugin: rerun request for " + typename);
				return false;
			}

		};
		try {
			Plugins.registerGlobalPlugin(repp);
			TypeRegistry r = getTypeRegistry(t);
			ReloadableType rtype = r.addType(t, loadBytesForClass(t));
			captureOn();
			rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
			String output = captureOff();
			System.out.println(output);
			assertContains("Reloading: Loading new version of simple.Basic [2]", output);
			assertUniqueContains("Plugin: reloadEvent(simple.Basic,simple.Basic,2)", output);
			assertContains("Reloading: Loading new version of simple.Basic [2]", output);
			assertUniqueContains("Plugin: rerun request for simple.Basic", output);
			result = runUnguarded(rtype.getClazz(), "run");
			assertEquals("goodbye", result.returnValue);
		}
		finally {
			Plugins.unregisterGlobalPlugin(repp);
		}
	}

	@Test
	public void testPluginRerunStaticInitializerRequest2() throws Exception {
		binLoader = new TestClassloaderWithRewriting("metaNotExist", true);
		String t = "clinit.One";
		ReloadEventProcessorPlugin repp = new ReloadEventProcessorPlugin() {

			public void reloadEvent(String typename, Class<?> clazz, String encodedTimestamp) {
				System.out.println("Plugin: reloadEvent(" + typename + "," + clazz.getName() + "," + encodedTimestamp
						+ ")");
			}

			public boolean shouldRerunStaticInitializer(String typename, Class<?> clazz, String encodedTimestamp) {
				System.out.println("Plugin: rerun request for " + typename);
				return true; // if this were false, the result below would be 5!
			}

		};
		try {
			Plugins.registerGlobalPlugin(repp);
			TypeRegistry r = getTypeRegistry(t);
			ReloadableType rtype = r.addType(t, loadBytesForClass(t));
			captureOn();
			rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
			String output = captureOff();
			System.out.println(output);
			assertContains("Reloading: Loading new version of clinit.One [2]", output);
			assertUniqueContains("Plugin: reloadEvent(clinit.One,clinit.One,2)", output);
			assertContains("Reloading: Loading new version of clinit.One [2]", output);
			assertUniqueContains("Plugin: rerun request for clinit.One", output);
			result = runUnguarded(rtype.getClazz(), "run");
			assertEquals("7", result.returnValue);
		}
		finally {
			Plugins.unregisterGlobalPlugin(repp);
		}
	}

}
