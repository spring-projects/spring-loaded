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
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Assert;

import org.junit.Test;
import org.springsource.loaded.ClassRenamer;
import org.springsource.loaded.MethodInvokerRewriter;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.test.infra.Result;


/**
 * Tests morphing method invocations made on Reloadable types.
 * <p>
 * Morphing the method invocations allows us to achieve support for methods that are added or removed since the target type was
 * originally loaded. Most importantly is recognizing when they are added - and once added they must be dynamically invoked because
 * they will not exist on the extracted interface for the target.
 * 
 * @author Andy Clement
 */
public class MethodInvokerRewriterTests extends SpringLoadedTests {

	//	@Test
	//	public void accessingAnnotationTypeReflectively() throws Exception {
	//		String t = "annos.Play";
	//		TypeRegistry typeRegistry = getTypeRegistry(t);
	//		ReloadableType target = typeRegistry.addType(t, loadBytesForClass(t));
	//		//		Class<?> callerClazz = loadit(t, loadBytesForClass(t));
	//		// Run the initial version which does not define toString()
	//		Result result = runUnguarded(target.getClazz(), "run");
	//		target.loadNewVersion("2", target.bytesInitial);
	//		result = runUnguarded(target.getClazz(), "run");
	//		System.out.println(result);
	//		//		ClassPrinter.print(target.bytesLoaded);
	//	}

	@Test
	public void fieldOverloading() throws Exception {
		TypeRegistry r = getTypeRegistry("fields..*");

		ReloadableType one = loadType(r, "fields.One");
		ReloadableType two = loadType(r, "fields.Two");

		Class<?> oneClazz = one.getClazz();
		Object oneInstance = oneClazz.newInstance();
		Class<?> twoClazz = two.getClazz();
		Object twoInstance = twoClazz.newInstance();

		// Field 'a' is only defined in One and 'inherited' by Two
		assertEquals("a from One", runOnInstance(oneClazz, oneInstance, "getOneA").returnValue);
		assertEquals("a from One", runOnInstance(twoClazz, twoInstance, "getTwoA").returnValue);

		runOnInstance(oneClazz, oneInstance, "setOneA", "abcde");
		assertEquals("abcde", runOnInstance(oneClazz, oneInstance, "getOneA").returnValue);

		runOnInstance(twoClazz, twoInstance, "setOneA", "abcde");
		assertEquals("abcde", runOnInstance(twoClazz, twoInstance, "getTwoA").returnValue);

		// Field 'b' is defined in One and Two
		assertEquals("b from One", runOnInstance(oneClazz, oneInstance, "getOneB").returnValue);
		assertEquals("b from Two", runOnInstance(twoClazz, twoInstance, "getTwoB").returnValue);

		// Field 'c' is private in One and public in Two
		assertEquals("c from One", runOnInstance(oneClazz, oneInstance, "getOneC").returnValue);
		assertEquals("c from Two", runOnInstance(twoClazz, twoInstance, "getTwoC").returnValue);

		// Now... set the private field 'c' in One then try to access the field c in both One and Two
		// Should be different if the FieldAccessor is preserving things correctly
		runOnInstance(twoClazz, twoInstance, "setOneC", "abcde");
		assertEquals("abcde", runOnInstance(twoClazz, twoInstance, "getOneC").returnValue);
		assertEquals("c from Two", runOnInstance(twoClazz, twoInstance, "getTwoC").returnValue);
	}

	@Test
	public void invokevirtual() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("virtual.CalleeOne");

		// The first target does not define toString()
		ReloadableType target = typeRegistry.addType("virtual.CalleeOne", loadBytesForClass("virtual.CalleeOne"));

		Class<?> callerClazz = loadit("virtual.CallerOne", loadBytesForClass("virtual.CallerOne"));

		// Run the initial version which does not define toString()
		Result result = runUnguarded(callerClazz, "run");
		// something like virtual.CalleeOne@4cee32
		assertTrue(((String) result.returnValue).startsWith("virtual.CalleeOne@"));

		// Load a version that does define toString()
		target.loadNewVersion("002", retrieveRename("virtual.CalleeOne", "virtual.CalleeOne002"));
		result = runUnguarded(callerClazz, "run");
		assertEquals("abcd", result.returnValue);

		// Load a version that does not define toString()
		target.loadNewVersion("003", retrieveRename("virtual.CalleeOne", "virtual.CalleeOne003"));
		result = runUnguarded(callerClazz, "run");
		// something like virtual.CalleeOne@4cee32
		assertTrue(((String) result.returnValue).startsWith("virtual.CalleeOne@"));
	}

	/**
	 * Testing what happens when reloading introduces a new method in the supertype that is called from the subtype.
	 */
	@Test
	public void invokevirtualNonCatchers() throws Exception {
		String top = "virtual.FourTop";
		String bot = "virtual.FourBot";
		TypeRegistry typeRegistry = getTypeRegistry(top + "," + bot);

		// The first top does not define foo()
		ReloadableType topR = typeRegistry.addType(top, loadBytesForClass(top));
		ReloadableType botR = typeRegistry.addType(bot, loadBytesForClass(bot));

		result = runUnguarded(botR.getClazz(), "run");
		assertEquals(42, result.returnValue);

		topR.loadNewVersion(retrieveRename(top, top + "2"));
		botR.loadNewVersion(retrieveRename(bot, bot + "2", top + "2:" + top));

		// now 'run()' should be invoking super.bar() where bar() is a new method 
		// introduced into FourTop
		result = runUnguarded(botR.getClazz(), "run");
		assertEquals(77, result.returnValue);
	}

	@Test
	public void invokevirtualNonCatchersErrorScenario() throws Exception {
		String top = "virtual.FourTop";
		String bot = "virtual.FourBot";
		TypeRegistry typeRegistry = getTypeRegistry(top + "," + bot);

		// The first top does not define foo()
		//		ReloadableType topR = 
		typeRegistry.addType(top, loadBytesForClass(top));
		ReloadableType botR = typeRegistry.addType(bot, loadBytesForClass(bot));

		result = runUnguarded(botR.getClazz(), "run");
		assertEquals(42, result.returnValue);

		// Dont load new Top, which means the new method that the subtype will call
		// will not exist
		// topR.loadNewVersion(retrieveRename(top, top + "2"));
		botR.loadNewVersion(retrieveRename(bot, bot + "2", top + "2:" + top));

		// now 'run()' should be invoking super.bar() where bar() is a new method 
		// introduced into FourTop
		try {
			result = runUnguarded(botR.getClazz(), "run");
			fail("Should have failed!");
		} catch (InvocationTargetException ite) {
			assertTrue(ite.getCause() instanceof NoSuchMethodError);
			assertEquals("FourBot.bar()I", ite.getCause().getMessage());
		}
	}

	/**
	 * Similar to previous test but 3 classes in the hierarchy.
	 */
	@Test
	public void invokevirtualNonCatchers2() throws Exception {
		String top = "virtual.FourTopB";
		String mid = "virtual.FourMidB";
		String bot = "virtual.FourBotB";
		TypeRegistry typeRegistry = getTypeRegistry(top + "," + bot);

		ReloadableType topR = typeRegistry.addType(top, loadBytesForClass(top));
		//		ReloadableType midR =
		typeRegistry.addType(mid, loadBytesForClass(mid));
		ReloadableType botR = typeRegistry.addType(bot, loadBytesForClass(bot));

		result = runUnguarded(botR.getClazz(), "run");
		assertEquals(42, result.returnValue);

		topR.loadNewVersion(retrieveRename(top, top + "2"));
		botR.loadNewVersion(retrieveRename(bot, bot + "2", mid + "2:" + mid));

		// now 'run()' should be invoking super.bar() where bar() is a new method 
		// introduced into FourTop
		result = runUnguarded(botR.getClazz(), "run");
		assertEquals(77, result.returnValue);
	}

	/**
	 * Similar to previous test but now new method is in the middle of the hierarchy (and in the top, but the middle one should be
	 * answering the request)
	 */
	@Test
	public void invokevirtualNonCatchers3() throws Exception {
		String top = "virtual.FourTopC";
		String mid = "virtual.FourMidC";
		String bot = "virtual.FourBotC";
		TypeRegistry typeRegistry = getTypeRegistry(top + "," + bot);

		ReloadableType topR = typeRegistry.addType(top, loadBytesForClass(top));
		ReloadableType midR = typeRegistry.addType(mid, loadBytesForClass(mid));
		ReloadableType botR = typeRegistry.addType(bot, loadBytesForClass(bot));

		result = runUnguarded(botR.getClazz(), "run");
		assertEquals(42, result.returnValue);

		topR.loadNewVersion(retrieveRename(top, top + "2"));
		midR.loadNewVersion(retrieveRename(mid, mid + "2", top + "2:" + top));
		botR.loadNewVersion(retrieveRename(bot, bot + "2", mid + "2:" + mid));

		// now 'run()' should be invoking super.bar() where bar() is a new method 
		// introduced into FourTop
		result = runUnguarded(botR.getClazz(), "run");
		assertEquals(99, result.returnValue);
	}

	/**
	 * Two classes in a hierarchy, both reloadable. Neither of them defines a toString(), what happens as we reload versions of them
	 * adding then removing toString().
	 */
	@Test
	public void invokevirtual2() throws Exception {
		String caller = "virtual.CallerTwo";
		String top = "virtual.CalleeTwoTop";
		String bottom = "virtual.CalleeTwoBottom";
		TypeRegistry typeRegistry = getTypeRegistry(top + "," + bottom);

		// The first target does not define toString()
		ReloadableType reloadableTop = typeRegistry.addType(top, loadBytesForClass(top));
		//		ReloadableType reloadableBottom = 
		typeRegistry.addType(bottom, loadBytesForClass(bottom));

		Class<?> callerClazz = loadit(caller, loadBytesForClass(caller));
		Result result = null;

		result = runUnguarded(callerClazz, "runTopToString");
		assertTrue(((String) result.returnValue).startsWith("virtual.CalleeTwoTop@"));

		result = runUnguarded(callerClazz, "runBottomToString");
		assertTrue(((String) result.returnValue).startsWith("virtual.CalleeTwoBottom@"));

		reloadableTop.loadNewVersion("002", retrieveRename(top, top + "002"));
		result = runUnguarded(callerClazz, "runTopToString");
		assertTrue(((String) result.returnValue).startsWith("topToString"));

		result = runUnguarded(callerClazz, "runBottomToString");
		// still no impl in bottom, so hits top
		assertTrue(((String) result.returnValue).startsWith("topToString"));

		// remove it again
		reloadableTop.loadNewVersion("003", retrieveRename(top, top + "003"));
		result = runUnguarded(callerClazz, "runBottomToString");
		assertTrue(((String) result.returnValue).startsWith("virtual.CalleeTwoBottom@"));
	}

	/**
	 * Variant of the above test that this time uses a 3 class hierarchy, checks we can get to the top.toString() that is added when
	 * toString() is invoked on bottom.
	 */
	@Test
	public void invokevirtual3() throws Exception {
		String caller = "virtual.CallerThree";

		String top = "virtual.CalleeThreeTop";
		String middle = "virtual.CalleeThreeMiddle";
		String bottom = "virtual.CalleeThreeBottom";
		TypeRegistry typeRegistry = getTypeRegistry(top + "," + bottom + "," + middle);

		// The first target does not define toString()
		ReloadableType reloadableTop = typeRegistry.addType(top, loadBytesForClass(top));
		typeRegistry.addType(middle, loadBytesForClass(middle));
		typeRegistry.addType(bottom, loadBytesForClass(bottom));

		Class<?> callerClazz = loadit(caller, loadBytesForClass(caller));
		Result result = null;

		result = runUnguarded(callerClazz, "runTopToString");
		assertTrue(((String) result.returnValue).startsWith("virtual.CalleeThreeTop@"));

		result = runUnguarded(callerClazz, "runBottomToString");
		assertTrue(((String) result.returnValue).startsWith("virtual.CalleeThreeBottom@"));

		// adds toString() to top class
		reloadableTop.loadNewVersion("002", retrieveRename(top, top + "002"));
		result = runUnguarded(callerClazz, "runTopToString");
		assertTrue(((String) result.returnValue).startsWith("topToString"));

		result = runUnguarded(callerClazz, "runBottomToString");
		assertTrue(((String) result.returnValue).startsWith("topToString"));

		// remove it again
		reloadableTop.loadNewVersion("003", retrieveRename(top, top + "003"));
		result = runUnguarded(callerClazz, "runBottomToString");
		assertTrue(((String) result.returnValue).startsWith("virtual.CalleeThreeBottom@"));
	}

	/**
	 * Here an interface is changed (reloaded) to include a new method, the implementing class already provides an implementation.
	 */
	@Test
	public void rewriteInvokeInterface1() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleIClass,target.SimpleI");
		ReloadableType intface = typeRegistry.addType("target.SimpleI", loadBytesForClass("target.SimpleI"));
		//		ReloadableType impl =
		typeRegistry.addType("target.SimpleIClass", loadBytesForClass("target.SimpleIClass"));

		byte[] callerbytes = loadBytesForClass("target.StaticICaller");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("target.StaticICaller", rewrittenBytes);

		// run the original
		Result result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		intface.loadNewVersion("2", retrieveRename("target.SimpleI", "target.SimpleI002"));

		// run the original working thing post-reload - check it is still ok
		result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		callerbytes = loadBytesForClass("target.StaticICaller002");
		callerbytes = ClassRenamer.rename("target.StaticICaller002", callerbytes, "target.SimpleI002:target.SimpleI");
		rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz002 = loadit("target.StaticICaller002", rewrittenBytes);

		result = runUnguarded(callerClazz002, "run");
		assertEquals("42", result.returnValue);
	}

	/**
	 * Here an interface and the implementation are changed (to add a new method to both).
	 */
	@Test
	public void rewriteInvokeInterface3() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleIClass,target.SimpleI");
		ReloadableType intface = typeRegistry.addType("target.SimpleI", loadBytesForClass("target.SimpleI"));
		ReloadableType impl = typeRegistry.addType("target.SimpleIClass", loadBytesForClass("target.SimpleIClass"));

		byte[] callerbytes = loadBytesForClass("target.StaticICaller");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("target.StaticICaller", rewrittenBytes);
		Result result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		// new interface on method and new implementation in the implementing class
		intface.loadNewVersion("2", retrieveRename("target.SimpleI", "target.SimpleI003"));
		impl.loadNewVersion("2",
				retrieveRename("target.SimpleIClass", "target.SimpleIClass003", "target.SimpleI003:target.SimpleI"));

		// run the original working thing post-reload - check it is still ok
		result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		callerbytes = loadBytesForClass("target.StaticICaller003");
		callerbytes = ClassRenamer.rename("target.StaticICaller003", callerbytes, "target.SimpleI003:target.SimpleI",
				"target.SimpleIClass003:target.SimpleIClass");
		rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz003 = loadit("target.StaticICaller003", rewrittenBytes);

		result = runUnguarded(callerClazz003, "run");
		assertEquals("2.01232768false", result.returnValue);
	}

	/**
	 * A method is removed from an interface.
	 */
	@Test
	public void rewriteInvokeInterface4_methodDeletion() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleIClass,target.SimpleI");
		ReloadableType intface = typeRegistry.addType("target.SimpleI", loadBytesForClass("target.SimpleI"));
		typeRegistry.addType("target.SimpleIClass", loadBytesForClass("target.SimpleIClass"));

		byte[] callerbytes = loadBytesForClass("target.StaticICaller");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("target.StaticICaller", rewrittenBytes);
		Result result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		// new interface version has method removed
		intface.loadNewVersion("2", retrieveRename("target.SimpleI", "target.SimpleI004"));

		try {
			// run the original working thing post-reload - check it is still ok
			result = runUnguarded(callerClazz, "run");
			fail("Method no longer exists, should not have been callable");
		} catch (InvocationTargetException ite) {
			assertTrue(ite.getCause() instanceof NoSuchMethodError);
			assertEquals("SimpleI.toInt(Ljava/lang/String;)I", ite.getCause().getMessage());
		}

		// new interface version has method re-added
		intface.loadNewVersion("3", retrieveRename("target.SimpleI", "target.SimpleI"));

		result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);
	}

	/**
	 * A method is changed on an interface - parameter type change.
	 */
	@Test
	public void rewriteInvokeInterface5_paramsChanged() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleIClass,target.SimpleI");
		ReloadableType intface = typeRegistry.addType("target.SimpleI", loadBytesForClass("target.SimpleI"));
		ReloadableType impl = typeRegistry.addType("target.SimpleIClass", loadBytesForClass("target.SimpleIClass"));

		byte[] callerbytes = loadBytesForClass("target.StaticICaller");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("target.StaticICaller", rewrittenBytes);
		Result result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		// new interface version has method removed
		intface.loadNewVersion("2", retrieveRename("target.SimpleI", "target.SimpleI005"));
		impl.loadNewVersion("2",
				retrieveRename("target.SimpleIClass", "target.SimpleIClass005", "target.SimpleI005:target.SimpleI"));

		callerbytes = loadBytesForClass("target.StaticICaller005");
		callerbytes = ClassRenamer.rename("target.StaticICaller005", callerbytes, "target.SimpleI005:target.SimpleI",
				"target.SimpleIClass005:target.SimpleIClass", "target.SimpleIClass005:target.SimpleIClass");
		rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		callerClazz = loadit("target.StaticICaller005", rewrittenBytes);

		result = runUnguarded(callerClazz, "run");
		assertEquals(72, result.returnValue);
	}

	/**
	 * A method is changed on an interface - return type changed
	 */
	@Test
	public void rewriteInvokeInterface6_returnTypeChanged() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleIClass,target.SimpleI");
		ReloadableType intface = typeRegistry.addType("target.SimpleI", loadBytesForClass("target.SimpleI"));
		ReloadableType impl = typeRegistry.addType("target.SimpleIClass", loadBytesForClass("target.SimpleIClass"));

		byte[] callerbytes = loadBytesForClass("target.StaticICaller");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("target.StaticICaller", rewrittenBytes);
		Result result = runUnguarded(callerClazz, "run2");
		assertEquals(111, result.returnValue);
		assertTrue(result.returnValue instanceof Integer);

		// new interface version has method removed
		intface.loadNewVersion("2", retrieveRename("target.SimpleI", "target.SimpleI005"));
		impl.loadNewVersion("2",
				retrieveRename("target.SimpleIClass", "target.SimpleIClass005", "target.SimpleI005:target.SimpleI"));

		callerbytes = loadBytesForClass("target.StaticICaller005");
		callerbytes = ClassRenamer.rename("target.StaticICaller005", callerbytes, "target.SimpleI005:target.SimpleI",
				"target.SimpleIClass005:target.SimpleIClass", "target.SimpleIClass005:target.SimpleIClass");
		rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		callerClazz = loadit("target.StaticICaller005", rewrittenBytes);

		result = runUnguarded(callerClazz, "run2");
		assertEquals("abc", result.returnValue);
		assertTrue(result.returnValue instanceof String);
	}

	/**
	 * Rewrite of a simple INVOKESTATIC call.
	 */
	@Test
	public void rewriteInvokeStatic() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleClass");
		ReloadableType r = typeRegistry.addType("target.SimpleClass", loadBytesForClass("target.SimpleClass"));

		byte[] callerbytes = loadBytesForClass("target.StaticCaller");
		// @formatter:off
		checkMethod(callerbytes, 
				"run", 
				" L0\n"+
				"    LDC 123\n"+
				"    INVOKESTATIC target/SimpleClass.toInt(Ljava/lang/String;)I\n"+
				"    IRETURN\n"+
				" L1\n");
		// @formatter:on

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);

		// @formatter:off
		checkMethod(
				rewrittenBytes,
				"run",
				" L0\n"+
				"    LDC 123\n"+
				"    LDC "+r.getId()+"\n"+
				"    LDC toInt(Ljava/lang/String;)I\n"+
				"    INVOKESTATIC org/springsource/loaded/TypeRegistry.istcheck(ILjava/lang/String;)Ljava/lang/Object;\n"+
				"    DUP\n"+
				"    IFNULL L1\n"+
				"    CHECKCAST target/SimpleClass__I\n"+
				"    ASTORE 1\n"+ // store the dispatcher to call in 1
				
				// Can we reduce this by calling some kind of pack method, if we make a static call then whatever is
				// on the stack can be the 'input' and the output can be the packed array.  But primitives make that
				// hard because they massively increase the number of variants of the pack method, we can't just have
				// for example, 1-20 arguments of type Object.
				// Object[] TypeRegistry.pack(int) would be enough here since the input value is an int.
				
				// it would remove from here:
				"    LDC 1\n"+ // load 1
				"    ANEWARRAY java/lang/Object\n"+ // new array of size 1
				"    DUP_X1\n"+ // put it under the argument (it'll be under and on top)
				"    SWAP\n"+ // put it under and under, arg on top
				"    LDC 0\n"+ // load 0
				"    SWAP\n"+ // swap 
				"    AASTORE\n"+ // store that in the array at index 0
				// to here
				"    ALOAD 1\n"+ // load the target
				"    SWAP\n"+ // put the target at the bottom
				"    ACONST_NULL\n"+ // load the instance (static call so null)
				"    LDC toInt(Ljava/lang/String;)I\n"+ // load the name+descriptor
				"    INVOKEINTERFACE target/SimpleClass__I.__execute([Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;\n"+
				"    CHECKCAST java/lang/Integer\n"+
				"    INVOKEVIRTUAL java/lang/Integer.intValue()I\n"+
				"    GOTO L2\n"+
				" L1\n"+
				"    POP\n"+
				"    INVOKESTATIC target/SimpleClass.toInt(Ljava/lang/String;)I\n"+
				" L2\n"+
				"    IRETURN\n"+
				" L3\n");
		// @formatter:on
		Class<?> callerClazz = loadit("target.StaticCaller", rewrittenBytes);
		//		ClassPrinter.print(r.bytesLoaded);
		Result result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);
	}

	/**
	 * Variant of the above where there is a parameter.
	 */
	@Test
	public void rewriteInvokeInterface2() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleIClassTwo,target.SimpleITwo");
		ReloadableType intface = typeRegistry.addType("target.SimpleITwo", loadBytesForClass("target.SimpleITwo"));
		//		ReloadableType impl =
		typeRegistry.addType("target.SimpleIClassTwo", loadBytesForClass("target.SimpleIClassTwo"));

		byte[] callerbytes = loadBytesForClass("target.StaticICallerTwo");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("target.StaticICallerTwo", rewrittenBytes);

		// run the original
		Result result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		intface.loadNewVersion("2", retrieveRename("target.SimpleITwo", "target.SimpleITwo002"));

		result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		callerbytes = loadBytesForClass("target.StaticICallerTwo002");
		callerbytes = ClassRenamer.rename("target.StaticICallerTwo002", callerbytes, "target.SimpleITwo002:target.SimpleITwo");
		rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz002 = loadit("target.StaticICallerTwo002", rewrittenBytes);
		//		ClassPrinter.print(rewrittenBytes);

		//		callee.loadNewVersion("2", retrieveRename("target.SimpleClass", "target.SimpleClass002"));
		result = runUnguarded(callerClazz002, "run");
		assertEquals("27", result.returnValue);
	}

	//	@Test
	//	public void rewriteMethodAccessesGetIntNonStatic() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		configureForTesting(typeRegistry, "data.Apple");
	//
	//		byte[] caller = retrieveBytesForClass("data.Orange");
	//		checkMethod(caller, "accessFieldOnApple",
	//				" L0\n" +
	//				"    ALOAD 0\n" +
	//				"    GETFIELD data/Orange.appleLdata/Apple;\n" +
	//				"    GETFIELD data/Apple.intFieldI\n" +
	//				"    ISTORE 1\n" +
	//				" L1\n" +
	//				"    ILOAD 1\n" +
	//				"    IRETURN\n" +
	//				" L2\n");
	//
	//		byte[] rewrittenBytes = MethodCallAndFieldAccessRewriter.rewrite(typeRegistry, caller);
	//		checkMethod(rewrittenBytes, "accessFieldOnApple",
	//				" L0\n" +
	//				"    ALOAD 0\n" +
	//				"    GETFIELD data/Orange.appleLdata/Apple;\n" +
	//				"    LDC intField\n" +
	//				"    LDC I\n" +
	//				"    INVOKEVIRTUAL data/Apple.r$get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;\n" +
	//				"    CHECKCAST java/lang/Integer\n" +
	//				"    INVOKEVIRTUAL java/lang/Integer.intValue()I\n" +
	//				"    ISTORE 1\n" +
	//				" L1\n" +
	//				"    ILOAD 1\n" +
	//				"    IRETURN\n" +
	//				" L2\n");
	//
	//	}
	//
	//	@Test
	//	public void rewriteMethodAccessesGetIntStatic() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "data.Apple");
	//		typeRegistry.addType("data.Apple", retrieveBytesForClass("data.Apple"));
	//
	//		byte[] caller = retrieveBytesForClass("data.Orange");
	//		checkMethod(caller, "getStaticFieldOnApple",
	//				" L0\n" +
	//				"    GETSTATIC data/Apple.staticIntFieldI\n" +
	//				"    IRETURN\n" +
	//				" L1\n");
	//
	//		byte[] rewrittenBytes = MethodCallAndFieldAccessRewriter.rewrite(typeRegistry, caller);
	//		checkMethod(rewrittenBytes, "getStaticFieldOnApple",
	//				" L0\n" +
	//				"    LDC staticIntField\n" +
	//				"    LDC I\n" +
	//				"    INVOKESTATIC data/Apple.r$gets(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;\n" +
	//				"    CHECKCAST java/lang/Integer\n" +
	//				"    INVOKEVIRTUAL java/lang/Integer.intValue()I\n" +
	//				"    IRETURN\n" +
	//				" L1\n");
	//
	//		Class<?> callerClass = loadit("data.Orange", rewrittenBytes);
	//		Object o = callerClass.newInstance();
	//		runOnInstance(callerClass, o, "setStaticFieldOnApple");
	//		Result result = runOnInstance(callerClass, o, "getStaticFieldOnApple");
	//		assertEquals(35, result.returnValue);
	//
	//		// calling it again on a different instance (static so should give same result)
	//		result = runOnInstance(callerClass, callerClass.newInstance(), "getStaticFieldOnApple");
	//		assertEquals(35, result.returnValue);
	//	}
	//
	//	@Test
	//	public void rewriteMethodAccessesSetIntNonStatic() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "data.Apple");
	//		typeRegistry.addType("data.Apple", retrieveBytesForClass("data.Apple"));
	//
	//		byte[] caller = retrieveBytesForClass("data.Orange");
	//		checkMethod(caller, "setFieldOnApple",
	//				" L0\n" +
	//				"    ALOAD 0\n" +
	//				"    GETFIELD data/Orange.appleLdata/Apple;\n" +
	//				"    BIPUSH 35\n" +
	//				"    PUTFIELD data/Apple.intFieldI\n" +
	//				" L1\n" +
	//				"    RETURN\n" +
	//				" L2\n");
	//
	//		byte[] rewrittenBytes = MethodCallAndFieldAccessRewriter.rewrite(typeRegistry, caller);
	//		checkMethod(rewrittenBytes, "setFieldOnApple",
	//				" L0\n" +
	//				"    ALOAD 0\n" +
	//				"    GETFIELD data/Orange.appleLdata/Apple;\n" +
	//				"    BIPUSH 35\n" +
	//				"    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;\n" +
	//				"    LDC intField\n" +
	//				"    LDC I\n" +
	//				"    INVOKEVIRTUAL data/Apple.r$set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V\n" +
	//				" L1\n" +
	//				"    RETURN\n" +
	//				" L2\n");
	//
	//		checkMethod(caller, "accessFieldOnApple",
	//				" L0\n" +
	//				"    ALOAD 0\n" +
	//				"    GETFIELD data/Orange.appleLdata/Apple;\n" +
	//				"    GETFIELD data/Apple.intFieldI\n" +
	//				"    ISTORE 1\n" +
	//				" L1\n" +
	//				"    ILOAD 1\n" +
	//				"    IRETURN\n" +
	//				" L2\n");
	//
	//		checkMethod(rewrittenBytes, "accessFieldOnApple",
	//				" L0\n" +
	//				"    ALOAD 0\n" +
	//				"    GETFIELD data/Orange.appleLdata/Apple;\n" +
	//				"    LDC intField\n" +
	//				"    LDC I\n" +
	//				"    INVOKEVIRTUAL data/Apple.r$get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;\n" +
	//				"    CHECKCAST java/lang/Integer\n" +
	//				"    INVOKEVIRTUAL java/lang/Integer.intValue()I\n" +
	//				"    ISTORE 1\n" +
	//				" L1\n" +
	//				"    ILOAD 1\n" +
	//				"    IRETURN\n" +
	//				" L2\n");
	//
	//		Class<?> callerClass = loadit("data.Orange", rewrittenBytes);
	//		Object o = callerClass.newInstance();
	//		runOnInstance(callerClass, o, "setFieldOnApple");
	//		Result result = runOnInstance(callerClass, o, "accessFieldOnApple");
	//		assertEquals(35, result.returnValue);
	//
	//		// and again on a different instance - should not be set this time
	//		result = runOnInstance(callerClass, callerClass.newInstance(), "accessFieldOnApple");
	//		assertEquals(0, result.returnValue);
	//	}

	// change GETFIELD <RELOADABLETYPE>.name<TYPE>
	// Change it to use a field accessor
	// we need to allow for calls to a field that gets removed
	// and to a field that is being added
	// Apple.s$get("intField","I") (will return a boxed Integer)
	// (with an unbox on the client side - as the requested type is int)

	// TODO review optimization of having static 'pack' methods with a variety of input params, returning an Object[] - will save a bunch of instructions
	// TODO review optimization of calling to a generated method (synthetic) that can do the packing (so synthetic has same params/return as invokestatic site)
	// this second optimization would greatly reduce generated code
	// TODO review optimization: extending 2 could even pull the invokestatic of anyChanges out into that helper too

	/**
	 * Rewrite of a simple INVOKESTATIC call - change the callee (to exercise the dispatching through the interface). This checks
	 * the behaviour of the TypeRegistry.anyChanges(int, String) method which determines whether we have to dispatch to something
	 * different due to a reload.
	 */
	@Test
	public void rewriteInvokeStatic2() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleClass");
		ReloadableType callee = typeRegistry.addType("target.SimpleClass", loadBytesForClass("target.SimpleClass"));

		byte[] callerbytes = loadBytesForClass("target.StaticCaller");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("target.StaticCaller", rewrittenBytes);
		//		ClassPrinter.print(callee.bytesLoaded);
		// run the original
		Result result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		callerbytes = loadBytesForClass("target.StaticCaller002");
		callerbytes = ClassRenamer.rename("target.StaticCaller002", callerbytes, "target.SimpleClass002:target.SimpleClass");
		rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz002 = loadit("target.StaticCaller002", rewrittenBytes);

		callee.loadNewVersion("2", retrieveRename("target.SimpleClass", "target.SimpleClass002"));
		result = runUnguarded(callerClazz002, "run2");
		assertEquals("456", result.returnValue);
	}

	/**
	 * Reloading target with a new static method that takes no parameters.
	 */
	@Test
	public void rewriteInvokeStatic3() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleClass");
		ReloadableType callee = typeRegistry.addType("target.SimpleClass", loadBytesForClass("target.SimpleClass"));

		byte[] callerbytes = loadBytesForClass("target.StaticCaller");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("target.StaticCaller", rewrittenBytes);

		// run the original
		Result result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		callerbytes = loadBytesForClass("target.StaticCaller003");
		callerbytes = ClassRenamer.rename("target.StaticCaller003", callerbytes, "target.SimpleClass003:target.SimpleClass");
		rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz002 = loadit("target.StaticCaller003", rewrittenBytes);
		callee.loadNewVersion("3", retrieveRename("target.SimpleClass", "target.SimpleClass003"));
		result = runUnguarded(callerClazz002, "run3");
		assertEquals("42", result.returnValue);
	}

	/**
	 * Reloading target with a modified static method.
	 */
	@Test
	public void rewriteInvokeStatic4() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleClass");
		ReloadableType callee = typeRegistry.addType("target.SimpleClass", loadBytesForClass("target.SimpleClass"));

		byte[] callerbytes = loadBytesForClass("target.StaticCaller");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("target.StaticCaller", rewrittenBytes);

		// run the original
		Result result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		// new version of SimpleClass always returns 256
		callee.loadNewVersion("4", retrieveRename("target.SimpleClass", "target.SimpleClass004"));
		result = runUnguarded(callerClazz, "run");
		assertEquals(256, result.returnValue);
	}

	/**
	 * Reloading target where the method to call has been deleted.
	 * <p>
	 * Here is what happens in the Java case (class A calling static method B.foo that has been deleted):
	 * 
	 * <pre>
	 * Exception in thread "main" java.lang.NoSuchMethodError: B.foo()V
	 *   at A.main(A.java:3)
	 * </pre>
	 */
	@Test
	public void rewriteInvokeStatic5() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleClass");
		ReloadableType callee = typeRegistry.addType("target.SimpleClass", loadBytesForClass("target.SimpleClass"));

		byte[] callerbytes = loadBytesForClass("target.StaticCaller");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("target.StaticCaller", rewrittenBytes);

		// run the original
		Result result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		// new version of SimpleClass where target static method has been removed
		callee.loadNewVersion("5", retrieveRename("target.SimpleClass", "target.SimpleClass005"));
		try {
			result = runUnguarded(callerClazz, "run");
			Assert.fail();
		} catch (InvocationTargetException ite) {
			Throwable t = ite.getCause();
			NoSuchMethodError icce = (NoSuchMethodError) t;
			assertEquals("SimpleClass.toInt(Ljava/lang/String;)I", icce.getMessage());
		}
	}

	/**
	 * If the static method is made non-static, here is what happens in the java case:
	 * 
	 * <pre>
	 * Exception in thread "main" java.lang.IncompatibleClassChangeError: Expected static method B.foo()V
	 *         at A.main(A.java:3)
	 * </pre>
	 */
	@Test
	public void rewriteInvokeStatic6() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleClass");
		ReloadableType callee = typeRegistry.addType("target.SimpleClass", loadBytesForClass("target.SimpleClass"));

		byte[] callerbytes = loadBytesForClass("target.StaticCaller");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("target.StaticCaller", rewrittenBytes);

		// run the original
		Result result = runUnguarded(callerClazz, "run");
		assertEquals(123, result.returnValue);

		// new version of SimpleClass where target static method has been made non-static
		callee.loadNewVersion("6", retrieveRename("target.SimpleClass", "target.SimpleClass006"));
		try {
			result = runUnguarded(callerClazz, "run");
			Assert.fail();
		} catch (InvocationTargetException ite) {
			Throwable t = ite.getCause();
			IncompatibleClassChangeError icce = (IncompatibleClassChangeError) t;
			assertEquals("SpringLoaded: Target of static call is no longer static 'SimpleClass.toInt(Ljava/lang/String;)I'",
					icce.getMessage());
		}
	}

	// TODO review visibility runtime checking.  In this next test a static method is changed from public to private.  It does
	// not currently trigger an error - whether we need to check kind of depends on if we support deployment of broken code.  A
	// compiler could not create code like this, it can only happen when one end of a call has been deployed but the other end hasnt

	//	/**
	//	 * If the static method is made non-visible (private), here is what happens in the java case:
	//	 * 
	//	 * <pre>
	//	 * Exception in thread "main" java.lang.IllegalAccessError: tried to access method B.foo()V from class A
	//	 *         at A.main(A.java:3)
	//	 * </pre>
	//	 */
	//	@Test
	//	public void rewriteInvokeStatic7() throws Exception {
	//		TypeRegistry typeRegistry = getTypeRegistry("target.SimpleClass");
	//		ReloadableType callee = typeRegistry.addType("target.SimpleClass", loadBytesForClass("target.SimpleClass"));
	//
	//		byte[] callerbytes = loadBytesForClass("target.StaticCaller");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
	//		Class<?> callerClazz = loadit("target.StaticCaller", rewrittenBytes);
	//
	//		Result result = runUnguarded(callerClazz, "run");
	//		assertEquals(123, result.returnValue);
	//
	//		// new version of SimpleClass where target static method has been made private
	//		callee.loadNewVersion("7", retrieveRename("target.SimpleClass", "target.SimpleClass007"));
	//
	//		try {
	//			ClassPrinter.print(rewrittenBytes);
	//			result = runUnguarded(callerClazz, "run");
	//			System.out.println(result.returnValue);
	//  fail here because the visibility of the changed static method has not been policed
	//			Assert.fail();
	//		} catch (RuntimeException rt) {
	//			rt.printStackTrace();
	//			InvocationTargetException ite = (InvocationTargetException) rt.getCause();
	//			Throwable t = ite.getCause();
	//			IncompatibleClassChangeError icce = (IncompatibleClassChangeError) t;
	//			assertEquals("Expected static method SimpleClass.toInt(Ljava/lang/String;)I", icce.getMessage());
	//		}
	//	}

	/**
	 * The simplest thing - calling a method with no params and no return (keeps generated code short!)
	 */
	@Test
	public void rewriteInvokeVirtual1() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("invokevirtual.B");
		ReloadableType b = loadType(typeRegistry, "invokevirtual.B");

		byte[] callerbytes = loadBytesForClass("invokevirtual.A");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("invokevirtual.A", rewrittenBytes);

		Result result = runUnguarded(callerClazz, "run");
		Assert.assertNull(result.returnValue);

		callerbytes = loadBytesForClass("invokevirtual.A2");
		callerbytes = ClassRenamer.rename("invokevirtual.A2", callerbytes, "invokevirtual.B2:invokevirtual.B");
		rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz002 = loadit("invokevirtual.A2", rewrittenBytes);

		b.loadNewVersion("2", retrieveRename("invokevirtual.B", "invokevirtual.B2"));
		result = runUnguarded(callerClazz002, "run");
		Assert.assertNull(result.returnValue);
	}

	/**
	 * The simplest thing - method now returns a string
	 */
	@Test
	public void rewriteInvokeVirtual2() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("invokevirtual.BB");
		ReloadableType b = typeRegistry.addType("invokevirtual.BB", loadBytesForClass("invokevirtual.BB"));

		byte[] callerbytes = loadBytesForClass("invokevirtual.AA");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("invokevirtual.AA", rewrittenBytes);

		Result result = runUnguarded(callerClazz, "callfoo");
		assertEquals("hi from BB.foo", result.returnValue);

		callerbytes = loadBytesForClass("invokevirtual.AA2");
		callerbytes = ClassRenamer.rename("invokevirtual.AA2", callerbytes, "invokevirtual.BB2:invokevirtual.BB");
		rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz002 = loadit("invokevirtual.AA2", rewrittenBytes);

		//		ClassPrinter.print(rewrittenBytes);
		b.loadNewVersion("2", retrieveRename("invokevirtual.BB", "invokevirtual.BB2"));

		//		result = runUnguarded(callerClazz002, "callfoo");
		//		assertEquals("hi from BB2.foo", result.returnValue);
		//
		//		result = runUnguarded(callerClazz002, "callbar");
		//		assertEquals("hi from BB2.bar", result.returnValue);

		// Now BB3 is loaded, it doesn't implement foo(), instead foo() from the supertype CC should run
		b.loadNewVersion("3", retrieveRename("invokevirtual.BB", "invokevirtual.BB3"));

		result = runUnguarded(callerClazz002, "callfoo");
		assertEquals("hi from CC.foo", result.returnValue);
	}

//		// @formatter:off
//		checkMethod(callerbytes, 
//				"one", 
//				" L0\n" + 
//				"    ALOAD 0\n" + 
//				"    GETFIELD data/Orange.appleLdata/Apple;\n"+
//				"    INVOKEVIRTUAL data/Apple.run()V\n" + 
//				" L1\n" + 
//				"    RETURN\n" + 
//				" L2\n");
//		// @formatter:on
	//
	//		// the call to Apple.run() will be rewritten
	//
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
//		// @formatter:off
//		checkMethod(
//				rewrittenBytes,
//				"one",
//				" L0\n" + 
//				"    ALOAD 0\n" + 
//				"    GETFIELD data/Orange.appleLdata/Apple;\n" + 
//				"    LDC "
//						+ typeRegistry.getId()
//						+ "\n"
//						+ "    LDC 1\n"
//						+ "    LDC 2\n"
//						+ "    LDC run\n"
//						+ "    LDC ()V\n"
//						+ "    INVOKESTATIC org/springsource/loaded/TypeRegistry.anyChanges(IIILjava/lang/String;Ljava/lang/String;)Ljava/lang/Object;\n"
//						+ "    DUP\n"
//						+ "    IFNULL L1\n"
//						+ "    ASTORE 1\n"
//						+ "    ALOAD 1\n"
//						+ "    SWAP\n"
//						+ "    ACONST_NULL\n"
//						+ "    LDC run\n"
//						+ "    LDC ()V\n"
//						+ "    INVOKEINTERFACE data/Apple$I.s$execute(Ldata/Apple;[Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;\n"
//						+ "    POP\n" + 
//						"    GOTO L2\n" + 
//						" L1\n" + 
//						"    POP\n" + 
//						"    INVOKEVIRTUAL data/Apple.run()V\n" + " L2\n"+
//						"    RETURN\n" + " L3\n");
//		// @formatter:on

	/**
	 * This test is interesting. It loads a type 'AspectReceiver' that has been advised by an aspect 'AnAspect'. The receiver gets
	 * its method calls rewritten and is then invoked. The aspect is not actually loaded up front and so at the point the rewritten
	 * method logic executes it will:<br>
	 * - call anyChanges() to see if the receiver is still OK<br>
	 * - call the dynamic dispatch execute method on the result of anyChanges()<br>
	 * 
	 * On the first run there is no new version but still under verification
	 * 
	 * @throws Exception
	 */
	@Test
	public void basicRewriteAspectReceiver() throws Exception {

		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		// ReloadableType apple = typeRegistry.recordType("data.Apple", retrieveClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.AspectReceiver");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.AspectReceiver", rewrittenBytes);

		// run the original
		// Result result =
		runUnguarded(callerClazz, "main2");
		// assertEquals("Apple.run() is running", result.stdout);
		//
		// apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		// result = runUnguarded(callerClazz, "one");
		// assertEquals("Apple002.run() is running", result.stdout);

	}

	// Exercising the rewritten code
	@Test
	public void basicRewriteSingleNonStaticMethodCallNoArgumentsNoReturn2() throws Exception {

		TypeRegistry typeRegistry = getTypeRegistry("data.Apple");
		ReloadableType target = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		// run the original
		result = runUnguarded(callerClazz, "one");
		assertEquals("Apple.run() is running", result.stdout);

		target.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		result = runUnguarded(callerClazz, "one");
		assertEquals("Apple002.run() is running", result.stdout);

		// run a modified version

		// remove the target method - should fail

		// replace the target method - should recover

		// run(orangeClazz,"oneCodeBefore");
		// run(orangeClazz,"oneCodeAfter");
		// run(orangeClazz,"oneCodeBeforeAndAfter");
	}

	/**
	 * Target method here takes (string,integer,string,integer) and return a string
	 */
	@Test
	public void rewriteCallArguments() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		try {
			runUnguarded(callerClazz, "callApple1", new Object[] { "a", 1, "b", 2 });
			Assert.fail("should not work, Apple doesn't have that method in it!");
		} catch (InvocationTargetException ite) {
			String cause = ite.getCause().toString();
			if (!cause.startsWith("java.lang.NoSuchMethodError")) {
				ite.printStackTrace();
				Assert.fail("Should be a NoSuchMethodError, but got " + ite.getCause());
			}
		}

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callApple1", new Object[] { "a", 1, "b", 2 });
		assertEquals("a 1 b 2", result.returnValue);

		// Load a version of Apple that doesn't define it
		apple.loadNewVersion("003", loadBytesForClass("data.Apple"));
		try {
			result = runUnguarded(callerClazz, "callApple1", new Object[] { "a", 1, "b", 2 });
			Assert.fail("should not work, Apple doesn't have that method in it!");
		} catch (InvocationTargetException ite) {
			String cause = ite.getCause().toString();
			if (!cause.startsWith("java.lang.NoSuchMethodError")) {
				ite.printStackTrace();
				Assert.fail("Should be a NoSuchMethodError, but got " + ite);
			}
		}
	}

	// Method is 'String run2(String a, int b, String c, int d)' which does not exist in Apple but exists in
	/**
	 * Targets for the calls here are: Apple then Apple002 Caller is Orange002
	 */
	@Test
	public void callingMethodIntroducedLaterPrimitiveParams() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.Apple");
		ReloadableType target = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerb = ClassRenamer.rename("data.Orange", loadBytesForClass("data.Orange002"), "data.Apple002:data.Apple");
		byte[] rewrittencallerb = MethodInvokerRewriter.rewrite(typeRegistry, callerb);
		Class<?> callerClazz = loadit("data.Orange", rewrittencallerb);

		// Method does not exist yet
		runExpectNoSuchMethodException(callerClazz, "callApple2", 3);

		// Load a version of Apple that does define that method
		target.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callApple2", 4);
		assertEquals("run2 4", result.returnValue);

		// Load a version of Apple that doesn't define it
		target.loadNewVersion("003", loadBytesForClass("data.Apple"));
		runExpectNoSuchMethodException(callerClazz, "callApple2", new Object[] { 5 });
	}

	@Test
	public void callingMethodIntroducedLaterPrimitiveParamsLongDouble() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		// public String callApple3(String s, int i, double d, String t, int[] is) {
		runExpectNoSuchMethodException(callerClazz, "callApple3x", new Object[] { "abc", 1, 2.0d, "def", new int[] { 42, 53 } });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callApple3x", new Object[] { "abc", 1, 2.0d, "def", new int[] { 42, 53 } });
		assertEquals("abc12.0def42", result.returnValue);

		// Load a version of Apple that doesn't define it
		apple.loadNewVersion("003", loadBytesForClass("data.Apple"));
		runExpectNoSuchMethodException(callerClazz, "callApple3x", new Object[] { "abc", 1, 2.0d, "def", new int[] { 42, 53 } });

	}

	@Test
	public void callingMethodIntroducedLaterReturningPrimitiveInt() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callApple3", new Object[] { 3 });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callApple3", new Object[] { 4 });
		assertEquals(8, result.returnValue);

		// Load a version of Apple that doesn't define it
		apple.loadNewVersion("003", loadBytesForClass("data.Apple"));
		runExpectNoSuchMethodException(callerClazz, "callApple3", new Object[] { 5 });
	}

	@Test
	public void callingMethodIntroducedLaterReturningPrimitiveFloat() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callAppleRetFloat", new Object[] { 3.0f });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callAppleRetFloat", new Object[] { 4.0f });
		assertEquals(8.0f, result.returnValue);
	}

	@Test
	public void callingMethodIntroducedLaterReturningPrimitiveBoolean() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callAppleRetBoolean", new Object[] { true });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callAppleRetBoolean", new Object[] { true });
		assertEquals(false, result.returnValue);
	}

	/**
	 * This is the 'control' testcase that loads a pair of types in a hierarchy, and calls methods on the subtype that simply make
	 * super calls to the supertype. Different variants are tested - with/without parameters, double slot parameters and methods
	 * that access private instance state. There is no reloading here, it is basically checking that the format of the rewritten
	 * super calls is OK.
	 */
	@Test
	public void superCallsControlCheck() throws Exception {

		TypeRegistry tr = getTypeRegistry("invokespecial..*");

		loadType(tr, "invokespecial.Able");
		ReloadableType rt = loadType(tr, "invokespecial.Simple");
		Object object = rt.getClazz().newInstance();

		Method method = rt.getClazz().getMethod("superCaller");
		String string = (String) method.invoke(object);
		assertEquals("abc", string);

		method = rt.getClazz().getMethod("withParamSuperCaller");
		string = (String) method.invoke(object);
		assertEquals("23", string);

		method = rt.getClazz().getMethod("withDoubleSlotParamSuperCaller");
		string = (String) method.invoke(object);
		assertEquals("30", string);

		method = rt.getClazz().getMethod("withParamSuperCallerPrivateVariable");
		string = (String) method.invoke(object);
		assertEquals("1", string);
	}

	/**
	 * This is similar to the first case except the hierarchy is split such that a middle type exists which does not initially
	 * implement the methods, they are added in a reload. This variant of the testcase is checking dispatch through the dynamic
	 * dispatch __execute method will work.
	 */
	@Test
	public void superCallsDynamicDispatcher() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokespecial..*");

		loadType(tr, "invokespecial.Top");
		ReloadableType rt = loadType(tr, "invokespecial.Able2");
		ReloadableType st = loadType(tr, "invokespecial.Simple2");
		rt.loadNewVersion("002", this.retrieveRename("invokespecial.Able2", "invokespecial.Able2002"));
		Object object = st.getClazz().newInstance();
		Method method = null;
		String string = null;

		//		ClassPrinter.print(rt.bytesLoaded);
		method = st.getClazz().getMethod("withParamSuperCaller");
		string = (String) method.invoke(object);
		assertEquals("2323", string);

		method = st.getClazz().getMethod("withDoubleSlotParamSuperCaller");
		string = (String) method.invoke(object);
		assertEquals("3030", string);

		// this call is checking the private field access in the reloaded method has been
		// changed to use the accessors into the type that can access the field from outside
		method = st.getClazz().getMethod("withParamSuperCallerPrivateVariable");
		string = (String) method.invoke(object);
		assertEquals("44", string);
	}

	/**
	 * This is similar to the first case except the hierarchy is split such that a middle type exists where the methods initially
	 * exist but then they are removed in a reload. We should end up at the top level methods.
	 */
	@Test
	public void superCallsRemovingMethods() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokespecial..*");

		ReloadableType a = loadType(tr, "invokespecial.A");
		ReloadableType b = loadType(tr, "invokespecial.B");
		ReloadableType c = loadType(tr, "invokespecial.C");

		Object object = c.getClazz().newInstance();
		Method method = null;
		String string = null;

		// class B implements it right now
		method = c.getClazz().getMethod("run1");
		string = (String) method.invoke(object);
		assertEquals("66", string);

		method = c.getClazz().getMethod("run2");
		string = (String) method.invoke(object);
		assertEquals("66falseabc", string);

		// Load new version of B where the methods are no longer there...
		b.loadNewVersion("002", retrieveRename("invokespecial.B", "invokespecial.B002"));

		// these calls should drop through to the super class A
		method = c.getClazz().getMethod("run1");
		string = (String) method.invoke(object);
		assertEquals("65", string);

		method = c.getClazz().getMethod("run2");
		string = (String) method.invoke(object);
		assertEquals("65falseabc", string);

		// Load new version of A where they aren't there either - how do we fail?
		a.loadNewVersion("002", retrieveRename("invokespecial.A", "invokespecial.A002"));

		// these calls should drop through to the super class A
		method = c.getClazz().getMethod("run1");
		try {
			string = (String) method.invoke(object);
			fail();
		} catch (InvocationTargetException ite) {
			assertEquals("java.lang.NoSuchMethodError", ite.getCause().getClass().getName());
			assertEquals("invokespecial.A.getInt()I", ite.getCause().getMessage());
		}
	}

	/**
	 * Starting all empty and filling things in on reloads - will the super calls be right?
	 */
	@Test
	public void superCallsFillingEmptyHierarchy() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokespecial..*");

		ReloadableType x = loadType(tr, "invokespecial.X");
		ReloadableType y = loadType(tr, "invokespecial.Y");
		ReloadableType z = loadType(tr, "invokespecial.Z");

		Object object = z.getClazz().newInstance();
		Method method = null;
		String string = null;

		// does nothing:  X and Y are completely empty and all Z.run() does is return ""
		method = z.getClazz().getMethod("run");
		string = (String) method.invoke(object);
		assertEquals("", string);

		// load new version of x with a method in it  'String foo()' that returns "X002.foo"
		x.loadNewVersion("002", retrieveRename("invokespecial.X", "invokespecial.X002"));

		// no difference, no-one is calling foo()!
		string = (String) method.invoke(object);
		assertEquals("", string);

		// load new version of Z, this will be calling super.foo() and be accessing the one in X002. Y002 is no different
		z.loadNewVersion(
				"002",
				retrieveRename("invokespecial.Z", "invokespecial.Z002", "invokespecial.X002:invokespecial.X",
						"invokespecial.Y002:invokespecial.Y"));

		// run() now calls 'super.foo()' so should return "X002.foo"
		string = (String) method.invoke(object);
		assertEquals("X002.foo", string);
		//		ClassPrinter.print(z.getLatestExecutorBytes());
		// Now reload Y, should make no difference.  Y002 is no different
		y.loadNewVersion("002", retrieveRename("invokespecial.Y", "invokespecial.Y002", "invokespecial.X002:invokespecial.X"));

		string = (String) method.invoke(object);
		assertEquals("X002.foo", string);
		// I see it is Ys dispatcher that isn't dispatching to the X.foo() method

		// Now reload Y, Y003 does provide an implementation
		y.loadNewVersion("003", retrieveRename("invokespecial.Y", "invokespecial.Y003", "invokespecial.X002:invokespecial.X"));

		string = (String) method.invoke(object);
		assertEquals("Y003.foo", string);

		// Now remove it from Y
		y.loadNewVersion("004", retrieveRename("invokespecial.Y", "invokespecial.Y"));
		string = (String) method.invoke(object);
		assertEquals("X002.foo", string);

		// Now remove it from X
		x.loadNewVersion("004", retrieveRename("invokespecial.X", "invokespecial.X"));
		try {
			string = (String) method.invoke(object);
			fail();
		} catch (InvocationTargetException ite) {
			assertEquals("java.lang.NoSuchMethodError", ite.getCause().getClass().getName());
			assertEquals("invokespecial.Y.foo()Ljava/lang/String;", ite.getCause().getMessage());
		}
	}

	// TODO could create copy of test above but start including private methods in reload variants and check we can't get to them

	@Test
	public void superCallsAccessingSuperOverSub() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokespecial..*");

		loadType(tr, "invokespecial.XX");
		ReloadableType y = loadType(tr, "invokespecial.YY");

		Object object = y.getClazz().newInstance();
		Method method = null;
		String string = null;

		// the run method calls local foo method initially
		method = y.getClazz().getMethod("run");
		string = method.invoke(object).toString();
		assertEquals("2", string);

		// the reloaded run method calls 'super.foo()'
		y.loadNewVersion("002", retrieveRename("invokespecial.YY", "invokespecial.YY002"));

		string = method.invoke(object).toString();
		assertEquals("1", string);
	}

	/**
	 * Call an existing super method through a super call then remove it, check an NSME occurs.
	 */
	@Test
	public void superCallsMethodDeletion() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokespecial..*");
		ReloadableType p = loadType(tr, "invokespecial.P");
		ReloadableType q = loadType(tr, "invokespecial.Q");

		// the run method calls foo in P via super call
		Method method = q.getClazz().getMethod("run");
		Object object = q.getClazz().newInstance();
		String string = method.invoke(object).toString();
		assertEquals("1", string);

		// reload p and remove the method
		p.loadNewVersion("002", retrieveRename("invokespecial.P", "invokespecial.P002"));
		//		ClassPrinter.print(p.bytesLoaded);
		try {
			string = method.invoke(object).toString();
			fail();
		} catch (InvocationTargetException ite) {
			assertEquals("java.lang.NoSuchMethodError", ite.getCause().getClass().getName());
			assertEquals("invokespecial.P.foo()I", ite.getCause().getMessage());
		}
	}

	/**
	 * Rewriting invokespecial when used for private method access.
	 */
	@Test
	public void privateMethodCallsAndInvokeSpecial() throws Exception {
		registry = getTypeRegistry("invokespecial..*");
		ReloadableType t = loadType(registry, "invokespecial.ContainsPrivateCalls");
		//			ReloadableType rt = loadType(tr, "invokespecial.Able2");
		//			ReloadableType st = loadType(tr, "invokespecial.Simple2");
		Class<?> clazz = t.getClazz();
		Object o = clazz.newInstance();
		Method m = clazz.getDeclaredMethod("callMyPrivates");

		// Straightforward call, no reloading:
		assertEquals("12123abctruez", m.invoke(o));

		// Reload itself, to cause executor creation:
		reload(t, "001");

		// Now the executor is being used.  The INVOKESPECIALs in the code will be rewritten to invokestatic 
		// calls in the executor that is built
		assertEquals("12123abctruez", m.invoke(o));
	}

	/**
	 * Rewriting invokespecial when used for private method access. Similar to the previous test but now we are deleting some of the
	 * methods on reload.
	 */
	@Test
	public void privateMethodCallsAndInvokeSpecial2() throws Exception {
		registry = getTypeRegistry("invokespecial..*");
		ReloadableType t = loadType(registry, "invokespecial.ContainsPrivateCalls");
		Class<?> clazz = t.getClazz();
		Object o = clazz.newInstance();
		Method m = clazz.getDeclaredMethod("callMyPrivates");

		// Straightforward call, no reloading:
		assertEquals("12123abctruez", m.invoke(o));

		// Reload a version where a private method has been deleted
		t.loadNewVersion("002", retrieveRename("invokespecial.ContainsPrivateCalls", "invokespecial.ContainsPrivateCalls002"));

		// With the removal of the private method the code won't even compile without the call to it being removed,
		// so it just works...
		assertEquals("123abctruez", m.invoke(o));
	}

	/**
	 * Rewriting invokespecial when used for private method access. Similar to the previous test but now changing the visibility of
	 * the private method.
	 */
	@Test
	public void privateMethodCallsAndInvokeSpecial3() throws Exception {
		registry = getTypeRegistry("invokespecial..*");
		ReloadableType t = loadType(registry, "invokespecial.ContainsPrivateCalls");
		Class<?> clazz = t.getClazz();
		Object o = clazz.newInstance();
		Method m = clazz.getDeclaredMethod("callMyPrivates");

		// Straightforward call, no reloading:
		assertEquals("12123abctruez", m.invoke(o));

		// Reload a version where a private method has been deleted
		t.loadNewVersion("002", retrieveRename("invokespecial.ContainsPrivateCalls", "invokespecial.ContainsPrivateCalls003"));

		// private method is promoted to public, invokespecial won't be getting used, so just works...
		assertEquals("12123abctruez", m.invoke(o));
	}

	/**
	 * Rewriting invokespecial when used for private method access. Similar to the previous test but now we change a public method
	 * to private
	 * 
	 */
	@Test
	public void privateMethodCallsAndInvokeSpecial4() throws Exception {
		registry = getTypeRegistry("invokespecial..*");
		ReloadableType t = loadType(registry, "invokespecial.ContainsPrivateCallsB");
		Class<?> clazz = t.getClazz();
		Object o = clazz.newInstance();
		Method m = clazz.getDeclaredMethod("callMyPrivates");

		// Straightforward call, no reloading:
		assertEquals("12123abctruez", m.invoke(o));

		// Reload a version where a private method has been deleted
		t.loadNewVersion("002", retrieveRename("invokespecial.ContainsPrivateCallsB", "invokespecial.ContainsPrivateCallsB002"));

		// public method is made private, changes from invokevirtual to invokespecial which is then rewritten to use
		// executor method
		assertEquals("12123abctruez", m.invoke(o));
	}

	@Test
	public void callingMethodIntroducedLaterReturningPrimitiveShort() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callAppleRetShort", new Object[] { (short) 3 });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callAppleRetShort", new Object[] { (short) 5 });
		assertEquals((short) 10, result.returnValue);
	}

	@Test
	public void callingMethodIntroducedLaterReturningPrimitiveLong() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callAppleRetLong", new Object[] { 5L });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callAppleRetLong", new Object[] { 5L });
		assertEquals(10L, result.returnValue);
	}

	@Test
	public void callingMethodIntroducedLaterReturningPrimitiveDouble() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callAppleRetDouble", new Object[] { 5.0d });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callAppleRetDouble", new Object[] { 3.0d });
		assertEquals(6.0d, result.returnValue);
	}

	@Test
	public void callingMethodIntroducedLaterReturningPrimitiveChar() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callAppleRetChar", new Object[] { (char) 'a' });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callAppleRetChar", new Object[] { (char) 'a' });
		assertEquals('b', result.returnValue);
	}

	@Test
	public void callingMethodIntroducedLaterReturningPrimitiveByte() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callAppleRetByte", new Object[] { (byte) 54 });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callAppleRetByte", new Object[] { (byte) 32 });
		assertEquals((byte) 64, result.returnValue);
	}

	@Test
	public void callingMethodIntroducedLaterReturningPrimitiveArray() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callAppleRetArrayInt", new Object[] { new int[] { 3 } });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callAppleRetArrayInt", new Object[] { new int[] { 3 } });
		assertEquals(3, ((int[]) result.returnValue)[0]);
	}

	@Test
	public void callingMethodIntroducedLaterReturningReferenceArray() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callAppleRetArrayString", new Object[] { new String[] { "abc" } });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callAppleRetArrayString", new Object[] { new String[] { "abc", "def" } });
		assertEquals("abc", ((String[]) result.returnValue)[0]);
	}

	@Test
	public void callingStaticMethodIntroducedLater() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callApple4", new Object[] { 3 });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callApple4", new Object[] { 4 });
		assertEquals(8, result.returnValue);

		// Load a version of Apple that doesn't define it
		apple.loadNewVersion("003", loadBytesForClass("data.Apple"));
		runExpectNoSuchMethodException(callerClazz, "callApple4", new Object[] { 5 });
	}

	@Test
	public void callingMethodChangedFromNonStaticToStatic() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data.Apple");
		ReloadableType apple = typeRegistry.addType("data.Apple", loadBytesForClass("data.Apple"));

		byte[] callerbytes = loadBytesForClass("data.Orange002");
		callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("data.Orange", rewrittenBytes);

		runExpectNoSuchMethodException(callerClazz, "callApple4", new Object[] { 3 });

		// Load a version of Apple that does define that method
		apple.loadNewVersion("002", retrieveRename("data.Apple", "data.Apple002"));
		Result result = runUnguarded(callerClazz, "callApple4", new Object[] { 4 });
		assertEquals(8, result.returnValue);

		// Load a version of Apple that doesn't define it
		apple.loadNewVersion("003", loadBytesForClass("data.Apple"));
		runExpectNoSuchMethodException(callerClazz, "callApple4", new Object[] { 5 });
	}

	/**
	 * Tests introduction of a change that causes us to invoke a private method. Tests that these calls are specially rewritten to
	 * be local executor calls.
	 */
	@Test
	public void testCallingNewCodeWithPrivateVisibility() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.Plum");
		ReloadableType plum = typeRegistry.addType("data.Plum", loadBytesForClass("data.Plum"));

		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, loadBytesForClass("data.Kiwi"));
		Class<?> kiwiClazz = loadit("data.Kiwi", rewrittenBytes);

		// Kiwi.run() calls Plum.run()
		run(kiwiClazz, "run");

		// This version of Plum changes Plum.run() so that it calls a private method
		plum.loadNewVersion("002", retrieveRename("data.Plum", "data.Plum002"));

		// Now *thats* magic:
		// The INVOKESPECIAL was recognized as invocation of a private method and redirected to the right
		// method in the executor (INVOKESTATIC data/Plum_E002.callPrivate(Ldata/Plum;)V)
		// Done by ExecutorBuilder.visitMethodInsn()
		run(kiwiClazz, "run");
	}

	/**
	 * Calling a method on a target that is initially satisfied by the subtype but is then added to the subtype with a different
	 * implementation.
	 */
	@Test
	public void virtualDispatchCallingSubMethodIntroducedLater() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokevirtual..*");

		ReloadableType x = loadType(tr, "invokevirtual.X");
		ReloadableType y = loadType(tr, "invokevirtual.Y");

		Method method = null;
		String string = null;

		Object object = x.getClazz().newInstance();
		method = x.getClazz().getMethod("run");

		// First call to run() will dispatch on 'Y' but as Y doesn't implement foo, X.foo will be used
		string = method.invoke(object).toString();
		assertEquals("1111", string);

		// Load a new version of Y that now implements foo
		y.loadNewVersion("002", this.retrieveRename("invokevirtual.Y", "invokevirtual.Y002"));
		string = method.invoke(object).toString();
		assertEquals("2222", string);

	}

	/**
	 * Calling a method on a target that is initially satisfied by the subtype but is then added to the subtype with a different
	 * implementation. This is a 3 level hierarchy unlike the previous one, and the new method is added to the 'middle' type.
	 */
	@Test
	public void virtualDispatchCallingSubMethodIntroducedLater2() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokevirtual..*");

		ReloadableType x = loadType(tr, "invokevirtual.XX");
		ReloadableType y = loadType(tr, "invokevirtual.YY");
		//		ReloadableType z = 
		loadType(tr, "invokevirtual.ZZ");

		Method method1 = null;
		Method method2 = null;
		Method method3 = null;
		String string = null;

		Object object = x.getClazz().newInstance();
		method1 = x.getClazz().getMethod("run1");
		method2 = x.getClazz().getMethod("run2");
		method3 = x.getClazz().getMethod("run3");

		// First call to run() will dispatch on 'ZZ' but as ZZ doesn't implement foo, and neither does YY, then XX.foo will be used
		string = method1.invoke(object).toString();
		assertEquals("1111", string);
		string = method2.invoke(object).toString();
		assertEquals("1111", string);
		string = method3.invoke(object).toString();
		assertEquals("1111", string);

		// Load a new version of Y that now implements foo
		y.loadNewVersion("002", this.retrieveRename("invokevirtual.YY", "invokevirtual.YY002"));
		string = method1.invoke(object).toString();
		assertEquals("3333", string);
		string = method2.invoke(object).toString();
		assertEquals("3333", string);
		string = method3.invoke(object).toString();
		assertEquals("3333", string);
	}

}