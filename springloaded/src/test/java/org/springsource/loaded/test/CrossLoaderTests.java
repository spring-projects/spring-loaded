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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.lang.ref.Reference;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.NameRegistry;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.SubLoader;
import org.springsource.loaded.test.infra.SuperLoader;


/**
 * These tests are going to load things across classloaders and the reloadable behaviour should work. Typical scenarios:
 * <ul>
 * <li>A supertype and its subtype are loaded by different loaders
 * <li>A type and the type it is calling are loaded by different loaders
 * </ul>
 * 
 * @author Andy Clement
 * @since 1.0
 */
public class CrossLoaderTests extends SpringLoadedTests {

	private SubLoader subLoader;

	@After
	public void teardown() throws Exception {
		super.teardown();
		GlobalConfiguration.directlyDefineTypes = true;
	}

	@Before
	public void setup() throws Exception {
		super.setup();
		binLoader = new SubLoader();
		subLoader = (SubLoader) binLoader;
		GlobalConfiguration.directlyDefineTypes = false;
	}

	/**
	 * Check the basics - does the SubLoader/SuperLoader mechanism work.
	 */
	@Test
	public void loadTypesAcrossLoaders() throws Exception {
		ReloadableType rtypeA = subLoader.loadAsReloadableType("superpkg.Top");
		result = runUnguarded(rtypeA.getClazz(), "m");
		assertEquals("Top.m() running", result.stdout);

		ReloadableType rtypeB = subLoader.loadAsReloadableType("subpkg.Bottom");
		result = runUnguarded(rtypeB.getClazz(), "m");
		assertEquals("Bottom.m() running", result.stdout);
		assertNotSame(rtypeA.getTypeRegistry(), rtypeB.getTypeRegistry());
	}

	@Test
	public void reloadSubtype() throws Exception {
		subLoader.loadAsReloadableType("superpkg.Top");
		ReloadableType rtypeB = subLoader.loadAsReloadableType("subpkg.Bottom");

		result = runUnguarded(rtypeB.getClazz(), "m");
		assertEquals("Bottom.m() running", result.stdout);

		rtypeB.loadNewVersion("2", retrieveRename("subpkg.Bottom", "subpkg.Bottom002", "superpkg.Top002:superpkg.Top"));
		result = runUnguarded(rtypeB.getClazz(), "m");
		assertEquals("Bottom002.m() running", result.stdout);
	}

	@Test
	public void reloadSupertype() throws Exception {
		ReloadableType rtypeA = subLoader.loadAsReloadableType("superpkg.Top");
		subLoader.loadAsReloadableType("subpkg.Bottom");

		result = runUnguarded(rtypeA.getClazz(), "m");
		assertEquals("Top.m() running", result.stdout);

		rtypeA.loadNewVersion("2", retrieveRename("superpkg.Top", "superpkg.Top002"));
		result = runUnguarded(rtypeA.getClazz(), "m");
		assertEquals("Top002.m() running", result.stdout);
	}

	/**
	 * hierarchy loaded across classloaders. <br>
	 * Top - all versions have a method 'm()'. v003 has method 'newMethodOnTop()'<br>
	 * Bottom - all versions have a method 'm()'. v003 version of m() calls 'super.newMethodOnTop()'
	 */
	@Ignore
	// test currently failing because we cache the reloadable type descriptors in TypeRegistry.getDescriptorFor()
	@Test
	public void reloadSupertypeCalledThroughSubtype() throws Exception {
		String top = "superpkg.Top";
		String bot = "subpkg.Bottom";

		ReloadableType rtypeA = subLoader.loadAsReloadableType(top);
		ReloadableType rtypeB = subLoader.loadAsReloadableType(bot);

		rtypeA.loadNewVersion("2", retrieveRename(top, top + "003"));
		rtypeB.loadNewVersion("2", retrieveRename(bot, bot + "003", top + "003:" + top));

		// Check the registry looks right for Top
		int topId = NameRegistry.getIdFor("superpkg/Top");
		TypeRegistry trTop = TypeRegistry.getTypeRegistryFor(subLoader.getParent());
		assertEquals(0, topId);
		assertEquals(top, trTop.getReloadableType(topId).getName());
		assertEquals(top, trTop.getReloadableType("superpkg/Top").getName());

		int bottomId = NameRegistry.getIdFor("subpkg/Bottom");
		TypeRegistry trBot = TypeRegistry.getTypeRegistryFor(subLoader);
		assertEquals(1, bottomId);
		assertEquals(bot, trBot.getReloadableType(bottomId).getName());
		assertEquals(bot, trBot.getReloadableType("subpkg/Bottom").getName());

		// Now call the m() in the Bottom003 type, which calls super.newMethodOnTop()
		result = runUnguarded(rtypeB.getClazz(), "m");
		assertEquals("newMethodOnTop() running", result.stdout);
	}

	/**
	 * In a class loaded by the subloader, calling a new method in a class loaded by the superloader. (ivicheck)
	 */
	@Test
	public void reloadTargetInSuperloader() throws Exception {
		String target = "superpkg.Target";
		String invoker = "subpkg.Invoker";

		ReloadableType targetR = subLoader.loadAsReloadableType(target);
		ReloadableType invokerR = subLoader.loadAsReloadableType(invoker);

		targetR.loadNewVersion("2", retrieveRename(target, target + "002"));
		invokerR.loadNewVersion("2", retrieveRename(invoker, invoker + "002", target + "002:" + target));

		// Check the registry looks right for target
		int targetId = NameRegistry.getIdFor(toSlash(target));
		assertEquals(0, targetId);
		TypeRegistry trtarget = TypeRegistry.getTypeRegistryFor(subLoader.getParent());
		assertEquals(target, trtarget.getReloadableType(targetId).getName());
		assertEquals(target, trtarget.getReloadableType(toSlash(target)).getName());

		int invokerId = NameRegistry.getIdFor(toSlash(invoker));
		TypeRegistry trinvokerR = TypeRegistry.getTypeRegistryFor(subLoader);
		assertEquals(1, invokerId);
		assertEquals(invoker, trinvokerR.getReloadableType(invokerId).getName());
		assertEquals(invoker, trinvokerR.getReloadableType(toSlash(invoker)).getName());

		// Now call the run() in the Invoker type, which calls 'Target.m()' where Target is in a different loader
		// and has been reloaded
		result = runUnguarded(invokerR.getClazz(), "run");
		assertEquals("Target002.m() running", result.stdout);
	}

	/**
	 * In a class loaded by the subloader, calling a new STATIC method in a class loaded by the superloader.
	 */
	@Test
	public void reloadTargetInSuperloaderCallingStaticMethod() throws Exception {
		String target = "superpkg.TargetB";
		String invoker = "subpkg.InvokerB";

		ReloadableType targetR = subLoader.loadAsReloadableType(target);
		ReloadableType invokerR = subLoader.loadAsReloadableType(invoker);

		targetR.loadNewVersion("2", retrieveRename(target, target + "002"));
		invokerR.loadNewVersion("2", retrieveRename(invoker, invoker + "002", target + "002:" + target));

		// Check the registry looks right for target
		int targetId = NameRegistry.getIdFor(toSlash(target));
		assertEquals(0, targetId);
		TypeRegistry trtarget = TypeRegistry.getTypeRegistryFor(subLoader.getParent());
		assertEquals(target, trtarget.getReloadableType(targetId).getName());
		assertEquals(target, trtarget.getReloadableType(toSlash(target)).getName());

		int invokerId = NameRegistry.getIdFor(toSlash(invoker));
		TypeRegistry trinvokerR = TypeRegistry.getTypeRegistryFor(subLoader);
		assertEquals(1, invokerId);
		assertEquals(invoker, trinvokerR.getReloadableType(invokerId).getName());
		assertEquals(invoker, trinvokerR.getReloadableType(toSlash(invoker)).getName());

		// Now call the run() in the Invoker type, which calls 'Target.m()' where Target is in a different loader
		// and has been reloaded
		result = runUnguarded(invokerR.getClazz(), "run");
		assertEquals("TargetB002.m() running", result.stdout);
	}

	@Test
	public void superdispatchers() throws Exception {
		String sub = "subpkg.Controller";

		ReloadableType subR = subLoader.loadAsReloadableType(sub);

		Result result = runOnInstance(subR.getClazz(), subR.getClazz().newInstance(), "foo");
		assertEquals("grails.Top.foo() running\nsubpkg.ControllerB.foo() running", result.stdout);

		// Reload the subtype
		subR.loadNewVersion("2", retrieveRename(sub, sub + "002"));

		result = runOnInstance(subR.getClazz(), subR.getClazz().newInstance(), "foo");
		assertEquals("grails.Top.foo() running\nsubpkg.ControllerB.foo() running again!", result.stdout);
	}

	/**
	 * In a class loaded by the subloader, calling a new STATIC method in a class loaded by the superloader. (istcheck)
	 */
	@Test
	public void reloadTargetInSuperloaderCallingStaticMethod2() throws Exception {
		// start out same as previous test, then loads a further version:
		String target = "superpkg.TargetB";
		String invoker = "subpkg.InvokerB";

		ReloadableType targetR = subLoader.loadAsReloadableType(target);
		ReloadableType invokerR = subLoader.loadAsReloadableType(invoker);

		targetR.loadNewVersion("2", retrieveRename(target, target + "002"));
		invokerR.loadNewVersion("2", retrieveRename(invoker, invoker + "002", target + "002:" + target));

		// Now call the run() in the Invoker type, which calls 'Target.m()' where Target is in a different loader
		// and has been reloaded
		result = runUnguarded(invokerR.getClazz(), "run");
		assertEquals("TargetB002.m() running", result.stdout);

		// now new: load new version of target that is missing the method

		targetR.loadNewVersion("3", targetR.bytesInitial);
		try {
			result = runUnguarded(invokerR.getClazz(), "run");
			fail("");
		}
		catch (InvocationTargetException ite) {
			assertTrue(ite.getCause() instanceof NoSuchMethodError);
			assertEquals("TargetB.m()V", ite.getCause().getMessage());
		}
	}

	/**
	 * This is testing field access when the value of the field is being checked against what is allowed to be returned.
	 * With multiple classloaders around this can get a little messy, say the method returns 'Serializable' but the
	 * actual method returns a type that the reloadable types classloader can't see (something from a subloader).<br>
	 * Heres a trace when things go wrong:
	 * 
	 * Caused by: org.springsource.loaded.UnableToLoadClassException: Unable to find data for class 'subpkg/Subby' at
	 * org.springsource.loaded.Utils.loadClassAsBytes2(Utils.java:763) <br>
	 * at org.springsource.loaded.TypeRegistry.getDescriptorFor(TypeRegistry.java:246) <br>
	 * at org.springsource.loaded.Utils.isAssignableFrom(Utils.java:1480) <br>
	 * at org.springsource.loaded.Utils.checkCompatibility(Utils.java:1460) <br>
	 * at org.springsource.loaded.ISMgr.getValue(ISMgr.java:125) <br>
	 * at superpkg.TargetD.r$get(TargetD.java) <br>
	 * at superpkg.TargetD$$E2.getOne(TargetD002.java:17) <br>
	 * at superpkg.TargetD$$D2.getOne(Unknown Source) <br>
	 * at superpkg.TargetD.getOne(TargetD.java) <br>
	 * at subpkg.InvokerD.run(InvokerD.java:8)
	 */
	@Test
	public void reloadCheckingCompatibilityForReturnedFields() throws Exception {
		// start out same as previous test, then loads a further version:
		String target = "superpkg.TargetD";
		String invoker = "subpkg.InvokerD";

		ReloadableType targetR = subLoader.loadAsReloadableType(target);
		ReloadableType invokerR = subLoader.loadAsReloadableType(invoker);

		result = runUnguardedWithCCL(invokerR.getClazz(), subLoader, "run");
		assertEquals("null", result.stdout);

		targetR.loadNewVersion("2", retrieveRename(target, target + "002"));
		//		invokerR.loadNewVersion("2", retrieveRename(invoker, invoker + "002", target + "002:" + target));

		// Now call the run() in the Invoker type, which calls 'Target.m()' where Target is in a different loader
		// and has been reloaded
		result = runUnguardedWithCCL(invokerR.getClazz(), subLoader, "run");
		assertEquals("a subby", result.stdout);
	}

	/**
	 * In a class loaded by the subloader, calling a new method in a class loaded by the superloader using super<dot>.
	 * (ispcheck)
	 */
	@Test
	public void reloadTargetInSuperLoaderCallingSuper() throws Exception {
		String top = "superpkg.TopB";
		String bot = "subpkg.BottomB";

		ReloadableType rtypeA = subLoader.loadAsReloadableType(top);
		ReloadableType rtypeB = subLoader.loadAsReloadableType(bot);

		rtypeA.loadNewVersion("2", retrieveRename(top, top + "002"));
		rtypeB.loadNewVersion("2", retrieveRename(bot, bot + "002", top + "002:" + top));

		// Check the registry looks right for Top
		int topId = NameRegistry.getIdFor("superpkg/TopB");
		TypeRegistry trTop = TypeRegistry.getTypeRegistryFor(subLoader.getParent());
		assertEquals(0, topId);
		assertEquals(top, trTop.getReloadableType(topId).getName());
		assertEquals(top, trTop.getReloadableType("superpkg/TopB").getName());

		int bottomId = NameRegistry.getIdFor("subpkg/BottomB");
		TypeRegistry trBot = TypeRegistry.getTypeRegistryFor(subLoader);
		assertEquals(1, bottomId);
		assertEquals(bot, trBot.getReloadableType(bottomId).getName());
		assertEquals(bot, trBot.getReloadableType("subpkg/BottomB").getName());

		// Now call the m() in the Bottom003 type, which calls super.newMethodOnTop()
		result = runUnguarded(rtypeB.getClazz(), "m");
		assertEquals("TopB002.m() running", result.stdout);
	}

	/**
	 * Now calling through an interface loaded by the superloader (iincheck)
	 */
	@Test
	public void reloadTargetInterfaceIsInSuperloader() throws Exception {
		// start out same as previous test, then loads a further version:
		String target = "superpkg.TargetC";
		String targetImpl = "superpkg.TargetImplC";
		String invoker = "subpkg.InvokerC";

		ReloadableType targetR = subLoader.loadAsReloadableType(target);
		ReloadableType targetImplR = subLoader.loadAsReloadableType(targetImpl);
		ReloadableType invokerR = subLoader.loadAsReloadableType(invoker);

		targetR.loadNewVersion("2", retrieveRename(target, target + "002"));
		targetImplR.loadNewVersion("2", retrieveRename(targetImpl, targetImpl + "002", target + "002:" + target));
		invokerR.loadNewVersion("2",
				retrieveRename(invoker, invoker + "002", target + "002:" + target, targetImpl + "002:" + targetImpl));

		// Now call the run() in the Invoker type, which calls 'Target.m()' where Target is in a different loader
		// and has been reloaded
		result = runUnguarded(invokerR.getClazz(), "run");
		assertEquals("TargetImplC002.m() running", result.stdout);

		// now new: load new version of target that is missing the method

		targetR.loadNewVersion("3", targetR.bytesInitial);
		try {
			result = runUnguarded(invokerR.getClazz(), "run");
			fail("");
		}
		catch (InvocationTargetException ite) {
			assertTrue(ite.getCause() instanceof NoSuchMethodError);
			assertEquals("TargetC.m()V", ite.getCause().getMessage());
		}
	}

	/**
	 * Now calling through an interface loaded by the superloader (iincheck). This time a new method is added to the
	 * interface which is *already* implemented by the impl.
	 */
	@Test
	public void reloadTargetInterfaceIsInSuperloader2() throws Exception {
		// start out same as previous test, then loads a further version:
		String target = "superpkg.TargetC";
		String targetImpl = "superpkg.TargetImplC";
		String invoker = "subpkg.InvokerC";

		ReloadableType targetR = subLoader.loadAsReloadableType(target);
		ReloadableType targetImplR = subLoader.loadAsReloadableType(targetImpl);
		ReloadableType invokerR = subLoader.loadAsReloadableType(invoker);

		targetR.loadNewVersion("2", retrieveRename(target, target + "002"));
		targetImplR.loadNewVersion("2", retrieveRename(targetImpl, targetImpl + "002", target + "002:" + target));
		invokerR.loadNewVersion("2",
				retrieveRename(invoker, invoker + "003", target + "002:" + target, targetImpl + "002:" + targetImpl));

		// Now call the run() in the Invoker type, which calls 'Target.m()' where Target is in a different loader
		// and has been reloaded
		result = runUnguarded(invokerR.getClazz(), "run");
		assertEquals("TargetImplC002.n() running", result.stdout);

		// now new: load new version of target that is missing the method

		targetR.loadNewVersion("3", targetR.bytesInitial);
		try {
			result = runUnguarded(invokerR.getClazz(), "run");
			fail("");
		}
		catch (InvocationTargetException ite) {
			assertTrue(ite.getCause() instanceof NoSuchMethodError);
			assertEquals("TargetC.n()V", ite.getCause().getMessage());
		}
	}


	// Large scale test loading a bunch of types and verifying what happens in terms of tagging
	@Test
	public void verifyingAssociatedTypesInfo2() throws Exception {
		// associatedtypes (top and middle) are in the super loader
		// subassociatedtypes (bottom) are in the sub loader

		ReloadableType bm = subLoader.loadAsReloadableType("associatedtypes.CM");

		ReloadableType cm = subLoader.loadAsReloadableType("associatedtypes.CM");
		assertNotNull(cm);
		assertNotEquals(subLoader, cm.getClazz().getClassLoader());
		ReloadableType im1 = subLoader.loadAsReloadableType("associatedtypes.IM");
		assertNotNull(im1);

		runUnguarded(cm.getClazz(), "run"); // Cause clinit to run so associations are setup

		assertContains("associatedtypes.CM", toString(im1.getAssociatedSubtypes()));
		assertFalse(cm.isAffectedByReload());
		assertFalse(cm.isAffectedByReload());
		assertFalse(bm.isAffectedByReload());

		// Load CM again, should tag CM and IM
		cm.loadNewVersion("2", cm.bytesInitial);

		assertTrue(cm.isAffectedByReload());
		assertTrue(im1.isAffectedByReload());
		assertTrue(bm.isAffectedByReload());
	}

	public String toString(List<Reference<ReloadableType>> list) {
		if (list == null) {
			return "null";
		}
		StringBuilder b = new StringBuilder();
		b.append("[");
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				b.append(",");
			}
			Reference<ReloadableType> ref = list.get(i);
			ReloadableType rtype = ref.get();
			if (rtype != null) {
				b.append(rtype.getName());
			}
		}
		b.append("]");
		return b.toString();
	}

	// TODO unfinished do I have to worry about proxies loaded by sub classloaders or not?

	// Avoiding fastclass in this test, one less thing to worry about
	@Test
	public void cglibProxiesAcrossLoader1() throws Exception {

		binLoader = new SubLoader(new String[] {}, new String[] { "../testdata/lib/cglib-nodep-2.2.jar" });
		subLoader = (SubLoader) binLoader;

		String t = "subpkg.ProxyTestcase";

		ReloadableType proxyTestcaseR = subLoader.loadAsReloadableType(t);

		result = runUnguarded(proxyTestcaseR.getClazz(), "run");
		System.out.println(result);

		result = runUnguarded(proxyTestcaseR.getClazz(), "getProxyLoader");
		System.out.println(result.returnValue);

		result = runUnguarded(proxyTestcaseR.getClazz(), "getSimpleLoader");
		System.out.println(result.returnValue);

		//		Class<?> clazz = binLoader.loadClass(t);
		//
		//		runMethodAndCollectOutput(clazz, "configureTest1");
		//
		//		String output = runMethodAndCollectOutput(clazz, "run");
		//		// interception should have occurred and original should not have been run
		//		assertContains("[void example.Simple.moo()]", output);
		//		assertDoesNotContain("Simple.moo() running", output);
		//
		//		// Check we loaded it as reloadable
		//		ReloadableType rtype = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash(t), false);
		//		assertNotNull(rtype);
		//
		//		// Check the incidental types were loaded as reloadable
		//		ReloadableType rtype2 = TypeRegistry.getTypeRegistryFor(binLoader).getReloadableType(toSlash("example.Simple"), false);
		//		assertNotNull(rtype2);
		//
		//		rtype.loadNewVersion(retrieveRename(t, t + "2", "example.Simple2:example.Simple"));
		//		rtype2.loadNewVersion(retrieveRename("example.Simple", "example.Simple2"));
		//
		//		// Now running 'boo()' which did not exist in the original. Remember this is invoked via proxy and so will only work
		//		// if the proxy was autoregenerated and reloaded!
		//		output = runMethodAndCollectOutput(clazz, "run");
		//		assertContains("[void example.Simple.boo()]", output);
		//		assertDoesNotContain("Simple2.boo running()", output);
	}

	// TODO tests:
	// - cglib representative tests?
	// - supertype not reloadable
	// - multiple types of the same name floating about?
	// - reflective invocation across classloaders
	// - crossloader field access

	// TODO catcher methods for interface methods where the class is an abstract class (it may not have a method in, so needs a catcher, in case it is
	// filled in later)

	// TODO optimizations:
	// set the superclass for a reloadabletype when the clinit runs for the subtype, for quicker lookup of exactly the type we need (getRegistryFor(clazz.getClassLoader())
	// use those sparse arrays and ID numbers more so that type lookups can be quicker.  Once we truly discover the right super type reference from a subtype, fill it in in the array
	// optimize for the case where there will only be one type around of a given name *usually*

	@Test
	public void github34() throws Exception {
		ReloadableType rtypeA = subLoader.loadAsReloadableType("issue34.Implementation3");
		result = runUnguarded(rtypeA.getClazz(), "run");
		assertEquals("Hello World!", result.stdout);

		//		ReloadableType rtypeB = subLoader.loadAsReloadableType("subpkg.Bottom");
		//		result = runUnguarded(rtypeB.getClazz(), "m");
		//		assertEquals("Bottom.m() running", result.stdout);
		//		assertNotSame(rtypeA.getTypeRegistry(), rtypeB.getTypeRegistry());
	}


}
