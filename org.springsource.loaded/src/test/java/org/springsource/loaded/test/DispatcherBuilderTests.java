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

import junit.framework.Assert;

import org.junit.Test;
import org.springsource.loaded.MethodInvokerRewriter;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.test.infra.Result;


/**
 * Tests for creation of the dispatcher instances that forward to the executors
 * 
 * @author Andy Clement
 */
public class DispatcherBuilderTests extends SpringLoadedTests {

	@Test
	public void reload() throws Exception {
		String tclass = "builder.DispatcherTestOne";
		TypeRegistry typeRegistry = getTypeRegistry(tclass);

		ReloadableType rtype = typeRegistry.addType(tclass, loadBytesForClass(tclass));

		// Simply reloads itself to trigger new version handling code paths in both infrastructure and generated code
		rtype.loadNewVersion("2", rtype.bytesInitial);

		// if we made it here, hurrah, we didn't crash - let's call that success!
	}

	@Test
	public void staticMethod() throws Exception {
		String tclass = "dispatcher.Staticmethod";
		TypeRegistry typeRegistry = getTypeRegistry(tclass);

		ReloadableType rtype = typeRegistry.addType(tclass, loadBytesForClass(tclass));

		// Simply reloads itself to trigger new version handling code paths in both infrastructure and generated code
		rtype.loadNewVersion("2", rtype.bytesInitial);
		//		ClassPrinter.print(rtype.interfaceBytes);
		//		ClassPrinter.print(rtype.getLatestExecutorBytes());

		byte[] callerbytes = loadBytesForClass("dispatcher.StaticmethodCaller");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("dispatcher.StaticmethodCaller", rewrittenBytes);

		result = runUnguarded(callerClazz, "run");
		Assert.assertTrue(result.stdout.indexOf("abc") != -1);
	}

	@Test
	public void reloadToString() throws Exception {
		String tclass = "builder.DispatcherTestOne";
		TypeRegistry typeRegistry = getTypeRegistry(tclass);

		ReloadableType rtype = typeRegistry.addType(tclass, loadBytesForClass(tclass));

		Result r = runUnguarded(rtype.getClazz(), "toString");
		String firstToString = (String) r.returnValue; // default toString()
		// Simply reloads itself to trigger new version handling code paths in both infrastructure and generated code
		rtype.loadNewVersion("2", retrieveRename(tclass, "builder.DispatcherTestOne002"));

		r = runUnguarded(rtype.getClazz(), "toString");
		String secondToString = (String) r.returnValue;
		Assert.assertNotSame(firstToString, secondToString);
		Assert.assertEquals("abc", secondToString);
	}

	/**
	 * Test we can differentiate between methods that would clash (static method on the target that takes the instance as first
	 * parameter and an instance method) - these clash methods would normally create
	 */
	@Test
	public void callingClashingMethods() throws Exception {
		String tclass = "dispatcher.C";
		TypeRegistry typeRegistry = getTypeRegistry(tclass);

		ReloadableType rtype = typeRegistry.addType(tclass, loadBytesForClass(tclass));

		rtype.loadNewVersion("2", rtype.bytesInitial);
		byte[] callerbytes = loadBytesForClass("dispatcher.CallC");
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, callerbytes);
		Class<?> callerClazz = loadit("dispatcher.CallC", rewrittenBytes);

		result = runUnguarded(callerClazz, "runInstance");
		if (result.stdout.indexOf("instance foo running") == -1) {
			Assert.fail("Did not find 'instance foo running' in:\n" + result.stdout);
		}

		result = runUnguarded(callerClazz, "runStatic");
		if (result.stdout.indexOf("static foo running") == -1) {
			Assert.fail("Did not find 'static foo running' in:\n" + result.stdout);
		}
	}

	//	@Test
	//	public void checkDynamicDispatcher() throws Exception {
	//		String tclass = "builder.DispatcherTestOne";
	//		TypeRegistry typeRegistry = getTypeRegistry(tclass);
	//
	//		ReloadableType rtype = typeRegistry.addType(tclass, loadBytesForClass(tclass));
	//
	//		// Load a version that has a new method in it:  foo(I)Ljava/lang/String;
	//		rtype.loadNewVersion("2", retrieveRename(tclass, "builder.DispatcherTestOne003"));
	//
//		// @formatter:off
//		Assert.assertEquals(
//				// Build a string consisting of the method name and descriptor:
//				"    ALOAD 3\n"+
//				
//				// Is it foo(I)Ljava/lang/String; ?
//				"    LDC foo(I)Ljava/lang/String;\n"+
//				"    INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z\n"+
//				"    IFEQ L0\n"+
//				
//				// It is! So call the real method on the dispatcher after preparing the arguments
//				"    ALOAD 2\n"+
//				"    CHECKCAST builder/DispatcherTestOne\n"+
//				"    ALOAD 1\n"+
//				"    LDC 0\n"+
//				"    AALOAD\n"+
//				"    CHECKCAST java/lang/Integer\n"+
//				"    INVOKEVIRTUAL java/lang/Integer.intValue()I\n"+
//				"    INVOKESTATIC builder/DispatcherTestOne__E2.foo(Lbuilder/DispatcherTestOne;I)Ljava/lang/String;\n"+
//				"    ARETURN\n"+
//				
//				// Is isnt! So throw an exception
//				" L0\n"+
//				"    NEW java/lang/IllegalStateException\n"+
//				"    DUP\n"+
//				"    INVOKESPECIAL java/lang/IllegalStateException.<init>()V\n"+
//				"    ATHROW\n",
//				toStringMethod(rtype.getLatestDispatcherBytes(), Constants.mDynamicDispatchName, false));
//		// @formatter:on
	//	}

}