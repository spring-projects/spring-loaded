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

package org.springsource.loaded.ri.test;

import static org.springsource.loaded.ri.ReflectiveInterceptor.jlClassGetConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Test;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;


/**
 * Variety of more "ad-hoc" non-generated tests for very specific cases related to Constructor reflection.
 * 
 * @author kdvolder
 */
public class ConstructorAdHocTest extends AbstractReflectionTests {

	private static final String INVOKER_CLASS_NAME = "reflection.ConstructorInvoker";

	private Class<?> callerClazz;

	private Object callerInstance;

	private ReloadableType targetClass; //One class chosen to focus test on

	//	/**
	//	 * When a Constructor is "regotten" the newly gotten Field will have to be a "fresh" object with its accessibility flag NOT
	//	 * set.
	//	 */
	//	private void doTestAccessibleFlagIsRefreshed(Constructor<?> cons) throws Exception {
	//		registry = getTypeRegistry("reflection.constructors..*");
	//		targetClass = reloadableClass("reflection.constructors.ClassForNewInstance");
	//
	//		callerClazz = nonReloadableClass(INVOKER_CLASS_NAME);
	//		callerInstance = newInstance(callerClazz);
	//		
	//		String fieldToAccess = scope + "Field";
	//		String expectMsg = "Class " + INVOKER_CLASS_NAME + " " + "can not access a member of class " + targetClass.dottedtypename + " "
	//				+ "with modifiers \"" + (scope.equals("default") ? "" : scope) + "\"";
	//
	//		//First... we do set the Access flag, it should work!
	//		Result r = runOnInstance(callerClazz, callerInstance, "getFieldWithAccess", targetClass.getClazz(), fieldToAccess, true);
	//		Assert.assertEquals(r.returnValue, fieldToAccess + " value");
	//
	//		//Then... we do not set the Access flag, it should fail!
	//		try {
	//			r = runOnInstance(callerClazz, callerInstance, "getFieldWithAccess", targetClass.getClazz(), fieldToAccess, false);
	//			Assert.fail("Without setting access flag shouldn't be allowed!");
	//		} catch (ResultException e) {
	//			assertIllegalAccess(expectMsg, e);
	//		}
	//
	//		//Finally, this should also still work... after we reload the type!
	//		reloadType(targetClass, "002");
	//
	//		//First... we do set the Access flag, it should work!
	//		r = runOnInstance(callerClazz, callerInstance, "getFieldWithAccess", targetClass.getClazz(), fieldToAccess, true);
	//		Assert.assertEquals("new "+fieldToAccess + " value", r.returnValue);
	//
	//		//Then... we do not set the Access flag, it should not be allowed!
	//		try {
	//			r = runOnInstance(callerClazz, callerInstance, "getFieldWithAccess", targetClass.getClazz(), fieldToAccess, false);
	//			Assert.fail("Without setting access flag shouldn't be allowed!");
	//		} catch (ResultException e) {
	//			assertIllegalAccess(expectMsg, e);
	//		}
	//	}

	//	@Test
	//	public void test_accessibleFlagIsRefreshedForPrivate() throws Exception {
	//		doTestAccessibleFlagIsRefreshed("private");
	//	}
	//
	//	@Test
	//	public void test_accessibleFlagIsRefreshedForProtected() throws Exception {
	//		doTestAccessibleFlagIsRefreshed("protected");
	//	}
	//
	//	@Test
	//	public void test_accessibleFlagIsRefreshedForDefault() throws Exception {
	//		doTestAccessibleFlagIsRefreshed("default");
	//	}

	@Test
	public void test_accessDeletedConstructor() throws Exception {
		registry = getTypeRegistry("reflection.constructors..*");
		targetClass = reloadableClass("reflection.constructors.ClassForNewInstance");
		//		Object targetInstance = 
		newInstance(targetClass.getClazz());

		Constructor<?> c = jlClassGetConstructor(targetClass.getClazz(), char.class, char.class);
		Assert.assertEquals("public reflection.constructors.ClassForNewInstance(char,char)", c.toString());

		// First try to access the Constructor before reloading
		callerClazz = nonReloadableClass(INVOKER_CLASS_NAME);
		callerInstance = newInstance(callerClazz);
		Result r = runOnInstance(callerClazz, callerInstance, "callNewInstance", c, (Object) new Object[] { 'a', 'b' });
		Assert.assertEquals(targetClass.getClazz(), r.returnValue.getClass());

		// Now reload the class... constructor is deleted
		reloadType(targetClass, "002");

		try {
			r = runOnInstance(callerClazz, callerInstance, "callNewInstance", c, (Object) new Object[] { 'a', 'b' });
			Assert.fail("Expected an error");
		}
		catch (ResultException re) {
			Throwable e = re.getCause();
			//			e.printStackTrace();
			Assert.assertEquals(InvocationTargetException.class, e.getClass());

			//Nested exception
			e = e.getCause();
			e.printStackTrace();
			Assert.assertEquals(NoSuchMethodError.class, e.getClass());

			//Example error message from Sun JVM:
			//			Exception in thread "main" java.lang.NoSuchMethodError: Target.<init>(C)V
			//			at Main.main(Main.java:10)

			Assert.assertEquals("reflection.constructors.ClassForNewInstance.<init>(CC)V", e.getMessage());
		}
	}

	// TODO: code below are tests from Andy, look through and decide what to do with them.
	//	@Test
	//	public void reflectiveFieldAccess_int() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//		// Read the field 'normally' (without reflection)
	//		Result r = runOnInstance(callerClazz, o, "getI");
	//		Assert.assertEquals(0, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using set)
	//		r = runOnInstance(callerClazz, o, "setI");
	//
	//		// Read the field 'normally' (without reflection)
	//		r = runOnInstance(callerClazz, o, "getI");
	//		Assert.assertEquals(42, r.returnValue);
	//
	//		// Read the field through reflection (using getInt)
	//		r = runOnInstance(callerClazz, o, "getReflectI");
	//		Assert.assertEquals(42, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using setInt)
	//		r = runOnInstance(callerClazz, o, "setIntI");
	//
	//		// Read the field through reflection (using get)
	//		r = runOnInstance(callerClazz, o, "getReflectObjectI");
	//		Assert.assertEquals(45, r.returnValue);
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_boolean() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//
	//		// Read the field 'normally' (without reflection)
	//		Result r = runOnInstance(callerClazz, o, "getZ");
	//		Assert.assertEquals(false, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using set)
	//		r = runOnInstance(callerClazz, o, "setZ");
	//
	//		// Read the field 'normally' (without reflection)
	//		r = runOnInstance(callerClazz, o, "getZ");
	//		Assert.assertEquals(true, r.returnValue);
	//
	//		// Read the field through reflection (using getInt)
	//		r = runOnInstance(callerClazz, o, "getReflectZ");
	//		Assert.assertEquals(true, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using setInt)
	//		r = runOnInstance(callerClazz, o, "setIntZ");
	//
	//		// Read the field through reflection (using get)
	//		r = runOnInstance(callerClazz, o, "getReflectObjectZ");
	//		Assert.assertEquals(false, r.returnValue);
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_byte() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//
	//		// Read the field 'normally' (without reflection)
	//		Result r = runOnInstance(callerClazz, o, "getB");
	//		Assert.assertEquals((byte) 0, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using set)
	//		r = runOnInstance(callerClazz, o, "setB");
	//
	//		// Read the field 'normally' (without reflection)
	//		r = runOnInstance(callerClazz, o, "getB");
	//		Assert.assertEquals((byte) 65, r.returnValue);
	//
	//		// Read the field through reflection (using getByte)
	//		r = runOnInstance(callerClazz, o, "getReflectB");
	//		Assert.assertEquals((byte) 65, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using setByte)
	//		r = runOnInstance(callerClazz, o, "setByteB");
	//
	//		// Read the field through reflection (using get)
	//		r = runOnInstance(callerClazz, o, "getReflectObjectB");
	//		Assert.assertEquals((byte) 70, r.returnValue);
	//
	//		try {
	//			r = runOnInstance(callerClazz, o, "setIllegalB");
	//		} catch (InvocationTargetException e) {
	//			Assert.assertEquals("Cannot set byte field reflection.Target.b to java.lang.Integer",
	//					e.getCause().getMessage());
	//			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
	//		}
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_char() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//
	//		// Read the field 'normally' (without reflection)
	//		Result r = runOnInstance(callerClazz, o, "getC");
	//		Assert.assertEquals((char) 0, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using set)
	//		r = runOnInstance(callerClazz, o, "setC");
	//
	//		// Read the field 'normally' (without reflection)
	//		r = runOnInstance(callerClazz, o, "getC");
	//		Assert.assertEquals((char) 66, r.returnValue);
	//
	//		// Read the field through reflection (using getChar)
	//		r = runOnInstance(callerClazz, o, "getReflectC");
	//		Assert.assertEquals((char) 66, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using setChar)
	//		r = runOnInstance(callerClazz, o, "setCharC");
	//
	//		// Read the field through reflection (using get)
	//		r = runOnInstance(callerClazz, o, "getReflectObjectC");
	//		Assert.assertEquals((char) 77, r.returnValue);
	//
	//		try {
	//			r = runOnInstance(callerClazz, o, "setIllegalC");
	//		} catch (InvocationTargetException e) {
	//			Assert.assertEquals("Cannot set char field reflection.Target.c to java.lang.Integer",
	//					e.getCause().getMessage());
	//			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
	//		}
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_short() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//
	//		// Read the field 'normally' (without reflection)
	//		Result r = runOnInstance(callerClazz, o, "getS");
	//		Assert.assertEquals((short) 0, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using set)
	//		r = runOnInstance(callerClazz, o, "setS");
	//
	//		// Read the field 'normally' (without reflection)
	//		r = runOnInstance(callerClazz, o, "getS");
	//		Assert.assertEquals((short) 660, r.returnValue);
	//
	//		// Read the field through reflection (using getChar)
	//		r = runOnInstance(callerClazz, o, "getReflectS");
	//		Assert.assertEquals((short) 660, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using setChar)
	//		r = runOnInstance(callerClazz, o, "setShortS");
	//
	//		// Read the field through reflection (using get)
	//		r = runOnInstance(callerClazz, o, "getReflectObjectS");
	//		Assert.assertEquals((short) 77, r.returnValue);
	//
	//		try {
	//			r = runOnInstance(callerClazz, o, "setIllegalS");
	//		} catch (InvocationTargetException e) {
	//			Assert.assertEquals("Cannot set short field reflection.Target.s to java.lang.Integer",
	//					e.getCause().getMessage());
	//			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
	//		}
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_long() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//
	//		// Read the field 'normally' (without reflection)
	//		Result r = runOnInstance(callerClazz, o, "getJ");
	//		Assert.assertEquals((long) 0, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using set)
	//		r = runOnInstance(callerClazz, o, "setJ");
	//
	//		// Read the field 'normally' (without reflection)
	//		r = runOnInstance(callerClazz, o, "getJ");
	//		Assert.assertEquals((long) 660, r.returnValue);
	//
	//		// Read the field through reflection (using getChar)
	//		r = runOnInstance(callerClazz, o, "getReflectJ");
	//		Assert.assertEquals((long) 660, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using setChar)
	//		r = runOnInstance(callerClazz, o, "setLongJ");
	//
	//		// Read the field through reflection (using get)
	//		r = runOnInstance(callerClazz, o, "getReflectObjectJ");
	//		Assert.assertEquals((long) 77, r.returnValue);
	//
	//		try {
	//			r = runOnInstance(callerClazz, o, "setIllegalJ");
	//		} catch (InvocationTargetException e) {
	//			Assert.assertEquals("Cannot set long field reflection.Target.j to java.lang.Integer",
	//					e.getCause().getMessage());
	//			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
	//		}
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_float() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//
	//		// Read the field 'normally' (without reflection)
	//		Result r = runOnInstance(callerClazz, o, "getF");
	//		Assert.assertEquals((float) 0, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using set)
	//		r = runOnInstance(callerClazz, o, "setF");
	//
	//		// Read the field 'normally' (without reflection)
	//		r = runOnInstance(callerClazz, o, "getF");
	//		Assert.assertEquals((float) 660, r.returnValue);
	//
	//		// Read the field through reflection (using getChar)
	//		r = runOnInstance(callerClazz, o, "getReflectF");
	//		Assert.assertEquals((float) 660, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using setChar)
	//		r = runOnInstance(callerClazz, o, "setFloatF");
	//
	//		// Read the field through reflection (using get)
	//		r = runOnInstance(callerClazz, o, "getReflectObjectF");
	//		Assert.assertEquals((float) 77, r.returnValue);
	//
	//		try {
	//			r = runOnInstance(callerClazz, o, "setIllegalF");
	//		} catch (InvocationTargetException e) {
	//			Assert.assertEquals("Cannot set float field reflection.Target.f to java.lang.Integer",
	//					e.getCause().getMessage());
	//			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
	//		}
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_double() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//
	//		// Read the field 'normally' (without reflection)
	//		Result r = runOnInstance(callerClazz, o, "getD");
	//		Assert.assertEquals((double) 0, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using set)
	//		r = runOnInstance(callerClazz, o, "setD");
	//
	//		// Read the field 'normally' (without reflection)
	//		r = runOnInstance(callerClazz, o, "getD");
	//		Assert.assertEquals((double) 660, r.returnValue);
	//
	//		// Read the field through reflection (using getDouble)
	//		r = runOnInstance(callerClazz, o, "getReflectD");
	//		Assert.assertEquals((double) 660, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using setDouble)
	//		r = runOnInstance(callerClazz, o, "setDoubleD");
	//
	//		// Read the field through reflection (using get)
	//		r = runOnInstance(callerClazz, o, "getReflectObjectD");
	//		Assert.assertEquals((double) 77, r.returnValue);
	//
	//		try {
	//			r = runOnInstance(callerClazz, o, "setIllegalD");
	//		} catch (InvocationTargetException e) {
	//			Assert.assertEquals("Cannot set double field reflection.Target.d to java.lang.Integer",
	//					e.getCause().getMessage());
	//			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
	//		}
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_booleanArray() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//
	//		// Read the field 'normally'
	//		Result r = runOnInstance(callerClazz, o, "getZArray");
	//		Assert.assertEquals(null, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using set)
	//		r = runOnInstance(callerClazz, o, "setZArray");
	//
	//		// Read the field 'normally'
	//		r = runOnInstance(callerClazz, o, "getZArray");
	//		Assert.assertEquals("[true false true]", toString((boolean[]) r.returnValue));
	//
	//		// Read the field through reflection
	//		r = runOnInstance(callerClazz, o, "getReflectObjectZArray");
	//		Assert.assertEquals("[true false true]", toString((boolean[]) r.returnValue));
	//
	//		try {
	//			r = runOnInstance(callerClazz, o, "setIllegalZArray");
	//			Assert.fail();
	//		} catch (InvocationTargetException e) {
	//			Assert.assertEquals("Cannot set [Z field reflection.Target.zs to java.lang.Integer",
	//					e.getCause().getMessage());
	//			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
	//		}
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_staticint() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//		ClassPrinter.print(rewrittenBytes);
	//		// Read the field 'normally'
	//		Result r = runOnInstance(callerClazz, o, "getISInteger");
	//		Assert.assertEquals(null, r.returnValue);
	//
	//		try {
	//			r = runOnInstance(callerClazz, o, "getIS");
	//			Assert.fail();
	//		} catch (InvocationTargetException ite) {
	//			// assert NPE internally
	//			// NPE because Integer not set
	//		}
	//
	//		// Call a method that will set the field reflectively (using set)
	//		r = runOnInstance(callerClazz, o, "setIS");
	//
	//		// Read the field 'normally'
	//		r = runOnInstance(callerClazz, o, "getISInteger");
	//		Assert.assertEquals(660, r.returnValue);
	//
	//		// Read the field through reflection
	//		r = runOnInstance(callerClazz, o, "getReflectObjectIS");
	//		Assert.assertEquals(660, r.returnValue);
	//
	//		try {
	//			r = runOnInstance(callerClazz, o, "setIllegalIS");
	//			Assert.fail();
	//		} catch (InvocationTargetException e) {
	//			Assert.assertEquals("Cannot set java.lang.Integer field reflection.Target.is to java.lang.String",
	//					e.getCause().getMessage());
	//			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
	//		}
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_reference() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//
	//		// Read the field 'normally'
	//		Result r = runOnInstance(callerClazz, o, "getReference");
	//		Assert.assertEquals(null, r.returnValue);
	//
	//		// Call a method that will set the field reflectively (using set)
	//		r = runOnInstance(callerClazz, o, "setReference");
	//
	//		// Read the field 'normally'
	//		r = runOnInstance(callerClazz, o, "getReference");
	//		Assert.assertEquals("abcde", r.returnValue);
	//
	//		// Read the field through reflection
	//		r = runOnInstance(callerClazz, o, "getReflectObjectReference");
	//		Assert.assertEquals("abcde", r.returnValue);
	//
	//		try {
	//			r = runOnInstance(callerClazz, o, "setIllegalReference");
	//			Assert.fail();
	//		} catch (InvocationTargetException e) {
	//			Assert.assertEquals("Cannot set java.lang.String field reflection.Target.l to java.lang.Integer",
	//					e.getCause().getMessage());
	//			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
	//		}
	//	}
	//
	//	private Object toString(boolean[] returnValue) {
	//		StringBuilder s = new StringBuilder("[");
	//		for (int i = 0; i < returnValue.length; i++) {
	//			if (i > 0) {
	//				s.append(" ");
	//			}
	//			s.append(returnValue[i]);
	//		}
	//		s.append("]");
	//		return s.toString();
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_getAnnotation() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		ReloadableType target =
	//				typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//
	//		Object o = callerClazz.newInstance();
	//
	//		Class<?> annoType = loadit("reflection.AnnoT", retrieveBytesForClass("reflection.AnnoT"));
	//		Class<?> annoType2 = loadit("reflection.AnnoT2", retrieveBytesForClass("reflection.AnnoT2"));
	//
	//		// Access an annotation
	//		Result r = runOnInstance(callerClazz, o, "getAnnotation", annoType);
	//		Assert.assertNotNull(r.returnValue);
	//		Assert.assertEquals("reflection.AnnoT", ((Annotation) r.returnValue).annotationType().getName());
	//
	//		// Now let's load a new version with different annotations:
	//		target.loadNewVersion("002", retrieveRename("reflection.Target", "reflection.Target002"));
	//		r = runOnInstance(callerClazz, o, "getAnnotation", annoType);
	//		Assert.assertNull(r.returnValue);
	//
	//		r = runOnInstance(callerClazz, o, "getAnnotation", annoType2);
	//		Assert.assertNotNull(r.returnValue);
	//		Assert.assertEquals("reflection.AnnoT2", ((Annotation) r.returnValue).annotationType().getName());
	//	}
	//
	//	@Test
	//	public void reflectiveFieldAccess_getAnnotations() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		ReloadableType target =
	//				typeRegistry.addType("reflection.Target", retrieveBytesForClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker", rewrittenBytes);
	//		Object o = callerClazz.newInstance();
	//
	//		// Access an annotation
	//		Result r = runOnInstance(callerClazz, o, "getDeclaredAnnotations");
	//		Assert.assertNotNull(r.returnValue);
	//		Assert.assertEquals(1, ((Annotation[]) r.returnValue).length);
	//		Assert.assertEquals("reflection.AnnoT", ((Annotation[]) r.returnValue)[0].annotationType().getName());
	//
	//		// Now let's load a new version with different annotations:
	//		target.loadNewVersion("002", retrieveRename("reflection.Target", "reflection.Target002"));
	//		r = runOnInstance(callerClazz, o, "getDeclaredAnnotations");
	//		Assert.assertNotNull(r.returnValue);
	//		Assert.assertEquals(1, ((Annotation[]) r.returnValue).length);
	//		Assert.assertEquals("reflection.AnnoT2", ((Annotation[]) r.returnValue)[0].annotationType().getName());
	//	}
	//
	//	@Test
	//	public void reflectiveFieldSetNonReloadableTarget() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		// Configure it directly such that data.Apple is considered reloadable
	//		configureForTesting(typeRegistry, "reflection.Target");
	//		// ReloadableType target =
	//		// typeRegistry.recordType("reflection.Target", retrieveClass("reflection.Target"));
	//
	//		byte[] invokerBytes = retrieveBytesForClass("reflection.Invoker2");
	//		// callerbytes = ClassRenamer.rename("data.Orange", callerbytes, "data.Apple002:data.Apple");
	//		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(typeRegistry, invokerBytes);
	//		Class<?> callerClazz = loadit("reflection.Invoker2", rewrittenBytes);
	//		Object o = callerClazz.newInstance();
	//
	//		// Call the setter and return the value set
	//		Result r = runOnInstance(callerClazz, o, "setString");
	//		Assert.assertEquals("wibble", r.returnValue);
	//
	//	}
}
