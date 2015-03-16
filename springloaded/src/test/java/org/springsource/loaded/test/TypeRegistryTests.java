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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypePattern;
import org.springsource.loaded.TypeRegistry;


/**
 * Tests for the TypeRegistry that exercise it in the same way it will actively be used when managing ReloadableType
 * instances.
 *
 * @author Andy Clement
 * @since 1.0
 */
public class TypeRegistryTests extends SpringLoadedTests {

	@Test
	public void basics() {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		assertNotNull(typeRegistry);
		typeRegistry = TypeRegistry.getTypeRegistryFor(null);
		assertNull(typeRegistry);
	}

	/**
	 * Same instance for two different calls passing the same classloader.
	 */
	@Test
	public void sameInstance() {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		assertNotNull(typeRegistry);
		TypeRegistry typeRegistry2 = TypeRegistry.getTypeRegistryFor(binLoader);
		assertNotNull(typeRegistry2);
		assertTrue(typeRegistry == typeRegistry2);
	}

	@Test
	public void loadingDescriptors() {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		TypeDescriptor jloDescriptor = typeRegistry.getDescriptorFor("java/lang/Object");
		assertNotNull(jloDescriptor);
		assertEquals("java/lang/Object", jloDescriptor.getName());
	}

	@Test
	public void descriptorsWithCatchers() {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		TypeDescriptor dscDescriptor = typeRegistry.getDescriptorFor("data/SimpleClass");
		assertNotNull(dscDescriptor);
		assertEquals("data/SimpleClass", dscDescriptor.getName());
		// check for a catcher
		assertNotNull(findMethod("0x1 toString()Ljava/lang/String;", dscDescriptor));
	}

	@Test
	public void descriptorsWithCatchers2() {
		// more complicated hierarchy
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		TypeDescriptor topDescriptor = typeRegistry.getDescriptorFor("catchers/Top");
		assertNotNull(topDescriptor);
		// Checking no toString() catcher because Top defines a toString()
		assertEquals(5, topDescriptor.getMethods().length);

		// if 'Top' is not considered reloadable we will get an entry for 'foo' that is inherited from it
		TypeDescriptor middleDescriptor = typeRegistry.getDescriptorFor("catchers/Middle");
		assertNotNull(middleDescriptor);
		assertEquals(5, middleDescriptor.getMethods().length);

		TypeDescriptor bottomDescriptor = typeRegistry.getDescriptorFor("catchers/Bottom");
		assertNotNull(bottomDescriptor);
		System.out.println(bottomDescriptor.toString());
		assertEquals(5, bottomDescriptor.getMethods().length);
	}

	@Test
	public void includesExcludes() {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		assertNotNull(typeRegistry);

		Properties p = new Properties();
		p.setProperty(TypeRegistry.Key_Inclusions, "com.foo.Bar");
		typeRegistry.configure(p);
		List<TypePattern> tps = typeRegistry.getInclusionPatterns();
		assertEquals(1, tps.size());
		assertEquals("text:com.foo.Bar", tps.get(0).toString());

		p.setProperty(TypeRegistry.Key_Inclusions, "com.foo.Bar,org.springsource..*");
		typeRegistry.configure(p);
		tps = typeRegistry.getInclusionPatterns();
		System.out.println(tps);
		assertEquals(2, tps.size());
		assertEquals("text:com.foo.Bar", tps.get(0).toString());
		assertEquals("text:org.springsource..*", tps.get(1).toString());
		assertTrue(typeRegistry.isReloadableTypeName("com/foo/Bar"));
		assertFalse(typeRegistry.isReloadableTypeName("com/foo/Garr"));
		assertTrue(typeRegistry.isReloadableTypeName("org/springsource/Garr"));
		assertTrue(typeRegistry.isReloadableTypeName("org/springsource/sub/Garr"));
		assertFalse(typeRegistry.isReloadableTypeName("Boo"));
	}

	@Test
	public void includesExcludes2() {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		assertNotNull(typeRegistry);

		Properties p = new Properties();
		p.setProperty(TypeRegistry.Key_Inclusions, "com.foo.Bar");
		typeRegistry.configure(p);
		List<TypePattern> tps = typeRegistry.getInclusionPatterns();
		assertEquals(1, tps.size());
		assertEquals("text:com.foo.Bar", tps.get(0).toString());
		assertFalse(tps.get(0).matches("com.foo.Gar"));
		assertTrue(tps.get(0).matches("com.foo.Bar"));

		p.setProperty(TypeRegistry.Key_Inclusions, "com.foo.Bar,org.springsource..*");
		typeRegistry.configure(p);
		tps = typeRegistry.getInclusionPatterns();
		assertEquals(2, tps.size());
		// exclude should be first
		assertEquals("text:com.foo.Bar", tps.get(0).toString());
		assertEquals("text:org.springsource..*", tps.get(1).toString());
		assertFalse(tps.get(0).matches("com.foo.Gar"));
		assertTrue(tps.get(0).matches("com.foo.Bar"));

		p.setProperty(TypeRegistry.Key_Inclusions, "com.foo..*");
		typeRegistry.configure(p);
		tps = typeRegistry.getInclusionPatterns();
		assertEquals(1, tps.size());
		assertFalse(tps.get(0).matches("com.goo.Bar"));
		assertTrue(tps.get(0).matches("com.foo.Bar"));
	}

	@Test
	public void includesExcludes3() {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		assertNotNull(typeRegistry);

		Properties p = new Properties();
		p.setProperty(TypeRegistry.Key_Inclusions, "*");
		typeRegistry.configure(p);
		List<TypePattern> tps = typeRegistry.getInclusionPatterns();
		assertEquals(1, tps.size());
		assertEquals("text:*", tps.get(0).toString());
		assertTrue(tps.get(0).matches("wibble"));

		p.setProperty(TypeRegistry.Key_Exclusions, "*");
		typeRegistry.configure(p);
		tps = typeRegistry.getExclusionPatterns();
		assertEquals("text:*", tps.get(0).toString());
		assertTrue(tps.get(0).matches("wibble"));
	}

	@Test
	public void loadTypeBadNames() {
		TypeRegistry typeRegistry = getTypeRegistry("data.SimpleClass002");
		assertFalse(typeRegistry.isReloadableTypeName("data/SimpleClass"));
		assertFalse(typeRegistry.isReloadableTypeName("com/bar"));
	}

	@Test
	public void loadType2() {
		TypeRegistry typeRegistry = getTypeRegistry("data.SimpleClass");
		assertTrue(typeRegistry.isReloadableTypeName("data/SimpleClass"));
		byte[] dsc = loadBytesForClass("data.SimpleClass");
		ReloadableType rtype = typeRegistry.addType("data.SimpleClass", dsc);
		assertNotNull(rtype);
	}

	@Test
	public void rebasePaths() {
		TypeRegistry typeRegistry = getTypeRegistry("data.SimpleClass");
		Properties p = new Properties();
		p.setProperty(TypeRegistry.Key_ReloadableRebase, "a/b/c=d/e/f,g/h=x/y");
		typeRegistry.configure(p);
		Map<String, String> rebases = typeRegistry.getRebasePaths();
		assertEquals(2, rebases.keySet().size());
		String value = rebases.get("a/b/c");
		assertEquals("d/e/f", value);
		assertEquals("x/y", rebases.get("g/h"));
	}

	/**
	 * Test that when the child classloader being managed by the type registry has reached the limit, it is recreated
	 * and types are then defined on the fly as it is used (dispatchers/executors).
	 */
	@Test
	public void classloaderRecreation() throws Exception {
		String one = "basic.Basic";
		String two = "basic.BasicB";

		GlobalConfiguration.maxClassDefinitions = 4;

		TypeRegistry typeRegistry = getTypeRegistry(one + "," + two);

		ReloadableType tOne = typeRegistry.addType(one, loadBytesForClass(one));
		ReloadableType tTwo = typeRegistry.addType(two, loadBytesForClass(two));

		result = runUnguarded(tOne.getClazz(), "getValue");
		assertEquals(5, result.returnValue);

		// Should be nothing defined in the child loader
		assertEquals(0, typeRegistry.getChildClassLoader().getDefinedCount());

		tOne.loadNewVersion("002", retrieveRename(one, one + "002"));
		// Should be dispatcher and executor for the reloaded type
		assertEquals(2, typeRegistry.getChildClassLoader().getDefinedCount());
		assertEquals(7, runUnguarded(tOne.getClazz(), "getValue").returnValue);

		tTwo.loadNewVersion("002", tTwo.bytesInitial);
		assertEquals(4, typeRegistry.getChildClassLoader().getDefinedCount());
		result = runUnguarded(tOne.getClazz(), "getValue");
		assertEquals(5, runUnguarded(tTwo.getClazz(), "getValue").returnValue);

		Class<?> cOneExecutor = tOne.getLatestExecutorClass();

		tOne.loadNewVersion("003", tOne.bytesInitial);

		// Now on this reload the child classloader should be recreated as it already has more
		// than 2 defined.
		// Note: this will currently cause us to redefine all the reloadable types
		// according to their most recent version. An optimization may be to only
		// define them on demand
		tTwo.loadNewVersion("002", tTwo.bytesInitial);
		assertEquals(4, typeRegistry.getChildClassLoader().getDefinedCount());
		assertEquals(5, runUnguarded(tTwo.getClazz(), "getValue").returnValue);

		// But what about calling the older types?
		assertEquals(5, runUnguarded(tOne.getClazz(), "getValue").returnValue);
		if (cOneExecutor == tOne.getLatestExecutorClass()) {
			fail("Why are we not using a new executor? the old one should have been removed, freeing up the classloader");
		}
	}

	/**
	 * Checking that the counting is working correctly for the managed classloader.
	 */
	@Test
	public void classloaderCounting() throws Exception {
		String one = "basic.Basic";
		String two = "basic.BasicB";
		String three = "basic.BasicC";

		TypeRegistry typeRegistry = getTypeRegistry(one + "," + two + "," + three);

		ReloadableType tOne = typeRegistry.addType(one, loadBytesForClass(one));
		ReloadableType tTwo = typeRegistry.addType(two, loadBytesForClass(two));
		ReloadableType tThree = typeRegistry.addType(three, loadBytesForClass(three));

		result = runUnguarded(tOne.getClazz(), "getValue");
		assertEquals(5, result.returnValue);

		// Should be nothing defined in the child loader
		assertEquals(0, typeRegistry.getChildClassLoader().getDefinedCount());

		tOne.loadNewVersion("002", retrieveRename(one, one + "002"));
		// Should be dispatcher and executor for the reloaded type
		assertEquals(2, typeRegistry.getChildClassLoader().getDefinedCount());

		tTwo.loadNewVersion("002", tTwo.bytesInitial);
		assertEquals(4, typeRegistry.getChildClassLoader().getDefinedCount());

		tThree.loadNewVersion("002", tThree.bytesInitial);
		assertEquals(6, typeRegistry.getChildClassLoader().getDefinedCount());
	}
}
