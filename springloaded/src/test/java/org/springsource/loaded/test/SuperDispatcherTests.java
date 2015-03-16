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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.objectweb.asm.tree.MethodNode;
import org.springsource.loaded.ClassRenamer;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils;

/**
 * Checking the computation of superdispatcher methods. Super dispatchers exist to access methods from a supertype that
 * can not normally be seen beyond the subtype. For example if a class has a protected method, that method needs a
 * superdispatcher in any reloadable subtypes so that they can call through it should any reloaded version of that
 * subtype make a super call.
 * 
 * @author Andy Clement
 * @since 1.1.5
 */
@SuppressWarnings("unused")
public class SuperDispatcherTests extends SpringLoadedTests {

	/**
	 * A reloadable type extends a type and overrides a protected method from that type.
	 */
	@Test
	public void basic() throws Exception {
		String t = "foo.ControllerB"; // supertype is in the grails/ package and so not reloadable
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		String rtypeDisassembled = toStringClass(rtype.bytesLoaded);
		// Should be one superdispatcher here
		assertEquals(16, countMethods(rtype.bytesLoaded));
		assertEquals(1, filter(getMethods(rtype.bytesLoaded), methodSuffixSuperDispatcher).size());
		String expectedName = "foo" + methodSuffixSuperDispatcher;
		assertContains("METHOD: 0x0001(public) " + expectedName + "()V", rtypeDisassembled);
		assertContains(
				"    ALOAD 0\n" +
						"    INVOKESPECIAL grails/TopB.foo()V\n" +
						"    RETURN\n",
				toStringMethod(rtype.bytesLoaded, expectedName, false));
		String stdout = runOnInstance(rtype.getClazz(), rtype.getClazz().newInstance(), "foo").stdout;
		assertEquals("TopB.foo() running\nControllerB.foo() running", stdout);
		Assert.assertTrue(rtype.loadNewVersion("2", retrieveRename(t, t + "2")));
		stdout = runOnInstance(rtype.getClazz(), rtype.getClazz().newInstance(), "foo").stdout;
		assertEquals("TopB.foo() running\nControllerB.foo() running again!", stdout);
	}

	/**
	 * A reloadable type extends a type and overrides a protected method from that type, then a further subtype extends
	 * the reloadable type.
	 */
	@Test
	public void twolevels() throws Exception {
		String t0 = "foo.ControllerB";
		String t = "foo.SubControllerB";
		TypeRegistry typeRegistry = getTypeRegistry("foo..*");
		ReloadableType rtype0 = typeRegistry.addType(t0, loadBytesForClass(t0));
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		String rtypeDisassembled = toStringClass(rtype.bytesLoaded);
		String stdout = runOnInstance(rtype.getClazz(), rtype.getClazz().newInstance(), "foo").stdout;
		assertEquals("TopB.foo() running\nControllerB.foo() running\nSubControllerB.foo() running", stdout);
		assertEquals(1, filter(getMethods(rtype.bytesLoaded), methodSuffixSuperDispatcher).size());
		Assert.assertTrue(rtype0.loadNewVersion("2", retrieveRename(t0, t0 + "2")));
		stdout = runOnInstance(rtype.getClazz(), rtype.getClazz().newInstance(), "foo").stdout;
		assertEquals("TopB.foo() running\nControllerB.foo() running again!\nSubControllerB.foo() running", stdout);
		Assert.assertTrue(rtype.loadNewVersion("2", retrieveRename(t, t + "2")));
		stdout = runOnInstance(rtype.getClazz(), rtype.getClazz().newInstance(), "foo").stdout;
		assertEquals("TopB.foo() running\nControllerB.foo() running again!\nSubControllerB.foo() running again!",
				stdout);
		// Why does this work?
		// The invokespecials that were targetting the supertypes have been modified to call the super dispatchers
		// and these superdispatchers will call catchers in the supertype if the original method isn't there or the
		// the original method if it did exist (but that itself may have been reloaded - the original method will call
		// the relevant executor if necessary)
	}

	/**
	 * A reloadable type extends a type and overrides a method from that type. This time the overridden method is not
	 * protected so no superdispatcher is needed.
	 */
	@Test
	public void noSuperDispatcher() throws Exception {
		String t = "foo.ControllerC"; // supertype is in the grails/ package and so not reloadable
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		String rtypeDisassembled = toStringClass(rtype.bytesLoaded);
		// Should be zero superdispatchers here
		assertEquals(15, countMethods(rtype.bytesLoaded));
		assertEquals(0, filter(getMethods(rtype.bytesLoaded), methodSuffixSuperDispatcher).size());
		String expectedName = "foo" + methodSuffixSuperDispatcher;
		assertDoesNotContain("METHOD: 0x0001(public) " + expectedName + "()V", rtypeDisassembled);
		String stdout = runOnInstance(rtype.getClazz(), rtype.getClazz().newInstance(), "foo").stdout;
		assertEquals("TopC.foo() running\nControllerC.foo() running", stdout);
		Assert.assertTrue(rtype.loadNewVersion("2", retrieveRename(t, t + "2")));
		stdout = runOnInstance(rtype.getClazz(), rtype.getClazz().newInstance(), "foo").stdout;
		assertEquals("TopC.foo() running\nControllerC.foo() running again!", stdout);
	}

	/**
	 * A reloadable type extends a type and overrides a protected method from that type. There are also private method
	 * calls within the reloadable type which should *not* be accidentally sent to super dispatchers.
	 */
	@Test
	public void privatesWithDispatchers() throws Exception {
		String t = "foo.ControllerD"; // supertype is in the grails/ package and so not reloadable
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		String rtypeDisassembled = toStringClass(rtype.bytesLoaded);
		// Should be one superdispatcher here
		assertEquals(17, countMethods(rtype.bytesLoaded));
		assertEquals(1, filter(getMethods(rtype.bytesLoaded), methodSuffixSuperDispatcher).size());
		String expectedName = "foo" + methodSuffixSuperDispatcher;
		assertContains("METHOD: 0x0001(public) " + expectedName + "()V", rtypeDisassembled);
		assertContains(
				"    ALOAD 0\n" +
						"    INVOKESPECIAL grails/TopD.foo()V\n" +
						"    RETURN\n",
				toStringMethod(rtype.bytesLoaded, expectedName, false));
		String stdout = runOnInstance(rtype.getClazz(), rtype.getClazz().newInstance(), "foo").stdout;
		assertEquals("TopD.foo() running\nControllerD.foo() running", stdout);
		Assert.assertTrue(rtype.loadNewVersion("2", retrieveRename(t, t + "2")));
		stdout = runOnInstance(rtype.getClazz(), rtype.getClazz().newInstance(), "foo").stdout;
		assertEquals("TopD.foo() running\nControllerD.foo() running again!", stdout);
	}

}
