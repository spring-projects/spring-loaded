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
package org.springsource.loaded.ri.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springsource.loaded.MethodMember;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.test.infra.ClassPrinter;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;


// JAVAP output for Class.class which shows the testcases that need writing...

// tested:
//public java.lang.reflect.Method getDeclaredMethod(java.lang.String, java.lang.Class[]) throws java.lang.NoSuchMethodException, java.lang.SecurityException;

// untested:
//public java.lang.reflect.Field getDeclaredField(java.lang.String)       throws java.lang.NoSuchFieldException, java.lang.SecurityException;
//public java.lang.reflect.Method[] getDeclaredMethods()       throws java.lang.SecurityException;
//public java.lang.reflect.Field[] getDeclaredFields()       throws java.lang.SecurityException;
//public boolean isAnnotationPresent(java.lang.Class); (defers to getAnnotation(...)!=null)
//public java.lang.annotation.Annotation getAnnotation(java.lang.Class);
//public java.lang.annotation.Annotation[] getDeclaredAnnotations();
//public java.lang.annotation.Annotation[] getAnnotations();
//public java.lang.reflect.Method getEnclosingMethod();
//public java.lang.reflect.Constructor getEnclosingConstructor();
//public native java.lang.Class getDeclaringClass();
//public java.lang.Class getEnclosingClass();
//public java.lang.String getSimpleName();
//public java.lang.String getCanonicalName();
//public boolean isAnonymousClass();
//public boolean isLocalClass();
//public boolean isMemberClass();
//public java.lang.Class[] getClasses();
//public java.lang.reflect.Field[] getFields()       throws java.lang.SecurityException;
//public java.lang.reflect.Method[] getMethods()       throws java.lang.SecurityException;
//public java.lang.reflect.Constructor[] getConstructors()       throws java.lang.SecurityException;
//public java.lang.reflect.Field getField(java.lang.String)       throws java.lang.NoSuchFieldException, java.lang.SecurityException;
//public java.lang.reflect.Method getMethod(java.lang.String, java.lang.Class[])       throws java.lang.NoSuchMethodException, java.lang.SecurityException;
//public java.lang.reflect.Constructor getConstructor(java.lang.Class[])       throws java.lang.NoSuchMethodException, java.lang.SecurityException;
//public java.lang.Class[] getDeclaredClasses()       throws java.lang.SecurityException;
//public java.lang.reflect.Constructor[] getDeclaredConstructors()       throws java.lang.SecurityException;
//public java.lang.reflect.Constructor getDeclaredConstructor(java.lang.Class[])       throws java.lang.NoSuchMethodException, java.lang.SecurityException;
//public java.io.InputStream getResourceAsStream(java.lang.String);
//public java.net.URL getResource(java.lang.String);
//public java.security.ProtectionDomain getProtectionDomain();
//public boolean desiredAssertionStatus();
//public boolean isEnum();
//public java.lang.Object[] getEnumConstants();
//public java.lang.Object cast(java.lang.Object);
//public java.lang.Class asSubclass(java.lang.Class);
public class ClassReflectionTests extends AbstractReflectionTests {

	public static final String TARGET_PACKAGE = "reflection.targets";
	private static final String TARGET_CLASS_NAME = TARGET_PACKAGE + ".ClassTarget";
	private static final String INVOKER_CLASS_NAME = "reflection.AdHocClassInvoker";

	private ReloadableType target = null;
	private Object callerInstance = null;
	private Class<?> callerClazz = null;

	@Before
	public void setup() throws Exception {
		super.setup();
		registry = getTypeRegistry(TARGET_PACKAGE + "..*");
		target = reloadableClass(TARGET_CLASS_NAME);

		callerClazz = nonReloadableClass(INVOKER_CLASS_NAME);
		callerInstance = callerClazz.newInstance();
	}

	private void reloadType(String version) {
		reloadType(target, version);
	}

	public static Method assertMethod(String expectedSignature, Result r) {
		Assert.assertEquals(expectedSignature, ((Method) r.returnValue).toString());
		return (Method) r.returnValue;
	}

	/**
	 * NoSuchMethodException should be thrown when trying to retrieve a Method object for a non-existent method.
	 */
	private void assertNoSuchMethodException(String expectedSignature, ResultException e) {
		InvocationTargetException ite = (InvocationTargetException) e.exception;
		Assert.assertTrue(ite.getCause() instanceof NoSuchMethodException);
		NoSuchMethodException nsme = (NoSuchMethodException) ite.getCause();
		Assert.assertEquals(expectedSignature, nsme.getMessage());
	}

	/**
	 * NoSuchMethodError is different from NoSuchMethodException! It should be thrown when trying to *call* a non-existent method.
	 */
	private void assertNoSuchMethodError(String expectedSignature, ResultException e) {
		Throwable cause = e.getDeepestCause();
		Assert.assertTrue(e.toString(), cause instanceof NoSuchMethodError);
		Assert.assertEquals(expectedSignature, cause.getMessage());
	}

	private Result getDeclaredMethod(String name, Class<?>... params) throws ResultException {
		return runOnInstance(callerClazz, callerInstance, "callGetDeclaredMethod", target.getClazz(), name, params);
	}

	private Result getDeclaredMethod(Class<?> forClass, String name, Class<?>... params) throws ResultException {
		return runOnInstance(callerClazz, callerInstance, "callGetDeclaredMethod", forClass, name, params);
	}

	private Result getDeclaredConstructor(Class<?>... params) throws ResultException {
		return runOnInstance(callerClazz, callerInstance, "callGetDeclaredConstructor", target.getClazz(), params);
	}

	private Result invokeConstructor(Constructor<?> constructor, Object... params) throws ResultException {
		return runOnInstance(callerClazz, callerInstance, "callNewInstance", constructor, params);
	}

	private Result getDeclaredConstructors() throws ResultException {
		return runOnInstance(callerClazz, callerInstance, "callGetDeclaredConstructors", target.getClazz());
	}

	/**
	 * Testing Class.getDeclaredMethod() - - first the method doesn't exist - then it does
	 */
	@Test
	public void test_getDeclaredLateMethod() throws Exception {
		try {
			getDeclaredMethod("lateMethod");
			Assert.fail("lateMethod shouldn't exist yet");
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + ".lateMethod()", e);
		}

		reloadType("002");

		Result r = getDeclaredMethod("lateMethod");
		assertMethod("public int " + TARGET_CLASS_NAME + ".lateMethod()", r);
	}

	/**
	 * Testing Class.getDeclaredMethod() - the method exists from the start and stays
	 */
	@Test
	public void test_getDeclaredMethodStays() throws Exception {
		Result r = getDeclaredMethod("methodStays");
		assertMethod("public int " + TARGET_CLASS_NAME + ".methodStays()", r);

		reloadType("002");

		r = getDeclaredMethod("methodStays");
		assertMethod("public int " + TARGET_CLASS_NAME + ".methodStays()", r);
	}

	@Test
	public void test_getDeclaredConstructor() throws Exception {

		// Let's call the constructor, nothing reloaded
		Result r = getDeclaredConstructor();
		assertNotNull(r.returnValue);
		Constructor<?> ctor = (Constructor<?>) r.returnValue;
		r = invokeConstructor(ctor);
		assertNotNull(r.returnValue);

		Constructor<?> ctorFloat = (Constructor<?>) getDeclaredConstructor(Float.TYPE).returnValue;
		r = invokeConstructor(ctorFloat, 44f);
		assertNotNull(r.returnValue);

		reloadType("002");

		// now call it again, first using the one we still have, 
		// then retrieving and using a new one
		r = invokeConstructor(ctor);
		assertNotNull(r.returnValue);

		ctorFloat = (Constructor<?>) getDeclaredConstructor(Float.TYPE).returnValue;
		r = invokeConstructor(ctorFloat, 22f);
		assertNotNull(r.returnValue);

		r = getDeclaredConstructor();
		assertNotNull(r.returnValue);
		ctor = (Constructor<?>) r.returnValue;
		r = invokeConstructor(ctor);
		assertNotNull(r.returnValue);

		// Access a constructor that isn't there
		try {
			r = getDeclaredConstructor(String.class);
		} catch (ResultException re) {
			assertTrue(re.getCause() instanceof InvocationTargetException);
			assertTrue(re.getCause().getCause().toString(), re.getCause().getCause() instanceof NoSuchMethodException);
		}

		// Access a new constructor (added in reload)
		r = getDeclaredConstructor(Integer.TYPE);
		assertNotNull(r.returnValue);
		ctor = (Constructor<?>) r.returnValue;
		r = invokeConstructor(ctor, 4);
		assertNotNull(r.returnValue);

		// Load new version with modified ctor
		reloadType("003");
		r = invokeConstructor(ctor, 4);
		assertContains("modified!", r.stdout);

	}

	@Test
	public void test_getDeclaredConstructors() throws Exception {

		// Let's call the constructor, nothing reloaded
		Result r = getDeclaredConstructors();
		assertNotNull(r.returnValue);
		Constructor<?>[] ctors = (Constructor<?>[]) r.returnValue;
		assertEquals(2, ctors.length);
		for (Constructor<?> ctor : ctors) {
			if (ctor.getParameterTypes().length == 1 && ctor.getParameterTypes()[0] == Integer.TYPE) {
				r = invokeConstructor(ctor, 4);
			} else if (ctor.getParameterTypes().length == 1 && ctor.getParameterTypes()[0] == Float.TYPE) {
				r = invokeConstructor(ctor, 4f);
			} else {
				r = invokeConstructor(ctor);
			}
			assertNotNull(r.returnValue);
		}

		reloadType("002");

		// now call it again, first using the one we still have, 
		// then retrieving and using a new one
		for (Constructor<?> ctor : ctors) {
			if (ctor.getParameterTypes().length == 1 && ctor.getParameterTypes()[0] == Integer.TYPE) {
				r = invokeConstructor(ctor, 4);
			} else if (ctor.getParameterTypes().length == 1 && ctor.getParameterTypes()[0] == Float.TYPE) {
				r = invokeConstructor(ctor, 4f);
			} else {
				r = invokeConstructor(ctor);
			}
			assertNotNull(r.returnValue);
		}

		r = getDeclaredConstructors();
		assertNotNull(r.returnValue);
		ctors = (Constructor<?>[]) r.returnValue;
		assertEquals(3, ctors.length);
		for (Constructor<?> ctor : ctors) {
			if (ctor.getParameterTypes().length == 1 && ctor.getParameterTypes()[0] == Integer.TYPE) {
				r = invokeConstructor(ctor, 4);
			} else if (ctor.getParameterTypes().length == 1 && ctor.getParameterTypes()[0] == Float.TYPE) {
				r = invokeConstructor(ctor, 4f);
			} else {
				r = invokeConstructor(ctor);
			}
			assertNotNull(r.returnValue);
		}

	}

	/**
	 * Testing Class.getDeclaredMethod() - the method exists from the start, but its implementation changed (really this test is not
	 * different from test_getDeclaredMethodStays unless we do something special when method isn't changed...)
	 */
	@Test
	public void test_getDeclaredMethodChanged() throws Exception {
		Result r = getDeclaredMethod("methodChanged");
		assertMethod("public int " + TARGET_CLASS_NAME + ".methodChanged()", r);

		reloadType("002");

		r = getDeclaredMethod("methodChanged");
		assertMethod("public int " + TARGET_CLASS_NAME + ".methodChanged()", r);
	}

	/**
	 * Testing Class.getDeclaredMethod() - first the method exists - then it is deleted
	 */
	@Test
	public void test_getDeclaredMethodDeleted() throws Exception {
		Result r = getDeclaredMethod("methodDeleted");
		assertMethod("public int " + TARGET_CLASS_NAME + ".methodDeleted()", r);

		reloadType("002");

		//		assertMethod("***DELETED:public int "+TARGET_CLASS_NAME+".methodDeleted()", r);

		try {
			r = getDeclaredMethod("methodDeleted");
			Assert.fail("The method shouldn't exist anymore!");
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + ".methodDeleted()", e);
		}
	}

	/**
	 * Testing Class.getDeclaredMethod() - method with parameters should fail before reloading, work after
	 */
	@Test
	public void test_getDeclaredMethodWithParams() throws Exception {
		try {
			getDeclaredMethod("doubleIt", String.class);
			Assert.fail("Method shouldn't exist yet");
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + ".doubleIt(java.lang.String)", e);
		}

		try {
			getDeclaredMethod("custard", String.class, int[].class, int.class);
			Assert.fail("Method shouldn't exist");
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + ".custard(java.lang.String, [I, int)", e);
		}

		reloadType("002");

		Result r = getDeclaredMethod("doubleIt", String.class);
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".doubleIt(java.lang.String)", r);
	}

	/**
	 * Test to see if Method objects retrieved by getDeclaredMethod remain identical even after class gets reloaded.
	 */
	@Test
	public void test_getDeclaredMethodsIdentical2() throws Exception {

		reloadType("002");

		Method m1 = (Method) (getDeclaredMethod("methodStays").returnValue);
		Method m2 = (Method) (getDeclaredMethod("methodStays").returnValue);

		Assert.assertEquals(m1, m2);

		m1 = (Method) (getDeclaredMethod("lateMethod").returnValue);
		m2 = (Method) (getDeclaredMethod("lateMethod").returnValue);

		Assert.assertEquals(m1, m2);
	}

	/**
	 * Test to see if Method objects for 'protected' methods are correct (w.r.t. to modifier flags)
	 */
	@Test
	public void test_getDeclaredProtectedMethod() throws Exception {
		Result r = getDeclaredMethod("protectedMethod");
		assertMethod("protected java.lang.String " + TARGET_CLASS_NAME + ".protectedMethod()", r);

		reloadType("002");

		r = getDeclaredMethod("protectedMethod");
		assertMethod("protected java.lang.String " + TARGET_CLASS_NAME + ".protectedMethod()", r);
	}

	/**
	 * Test getting a declared method with multiple params that doesn't exist.
	 */
	@Test
	public void test_NoSuchMethodException() throws Exception {
		try {
			//			Result r = 
			getDeclaredMethod("bogus", String.class, int.class);
			fail("getting bogus method should fail");
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + ".bogus(java.lang.String, int)", e);
		}
		Result r = getDeclaredMethod("deleteThem", String.class, int.class);
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".deleteThem(java.lang.String,int)", r);

		reloadType("002");

		try {
			r = getDeclaredMethod("deleteThem", String.class, int.class);
			fail("getting deleted method should fail");
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + ".deleteThem(java.lang.String, int)", e);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	// Testing "invoke"

	private Result getDeclaredMethodAndInvoke(String name) throws Exception {
		Result r = getDeclaredMethod(name);
		return invoke((Method) r.returnValue);
	}

	private Result invoke(Method m, Object... params) throws Exception {
		return runOnInstance(callerClazz, callerInstance, "runThisMethodWithParam", m, params);
	}

	private Result invoke(Method m) throws Exception {
		return runOnInstance(callerClazz, callerInstance, "runThisMethod", m);
	}

	private Result invokeOn(Object receiver, Method m, Object... args) throws Exception {
		return runOnInstance(callerClazz, callerInstance, "runThisMethodOn", receiver, m, args);
	}

	/**
	 * Testing Class.getDeclaredMethod and Method.invoke
	 * 
	 * Can we retrieve and invoke a method that was added in a reloaded type?
	 */
	@Test
	public void test_getDeclaredLateMethodAndInvoke() throws Exception {
		try {
			getDeclaredMethodAndInvoke("lateMethod");
			fail("getting/invoking a method that hs not yet been defined should fail");
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + ".lateMethod()", e);
		}

		reloadType("002");

		Result r = getDeclaredMethodAndInvoke("lateMethod");
		Assert.assertEquals(42, r.returnValue);
	}

	/**
	 * Testing Class.getDeclaredMethod and Method.invoke
	 * 
	 * Can we retrieve and invoke a method that existed before we reloaded the type?
	 */
	@Test
	public void test_getDeclaredMethodStaysAndInvoke() throws Exception {
		Result r = getDeclaredMethodAndInvoke("methodStays");
		Assert.assertEquals(99, r.returnValue);

		reloadType("002");

		r = getDeclaredMethodAndInvoke("methodStays");
		Assert.assertEquals(99, r.returnValue);
	}

	/**
	 * Testing Class.getDeclaredMethod and Method.invoke
	 * 
	 * Can we retrieve and invoke a method that existed before we reloaded the type, and was deleted later.
	 */
	@Test
	public void test_getDeclaredMethodDeletedAndInvoke() throws Exception {
		Result r = getDeclaredMethodAndInvoke("methodDeleted");
		Assert.assertEquals(37, r.returnValue);

		reloadType("002");

		try {
			getDeclaredMethodAndInvoke("methodDeleted");
			fail("getting/invoking a deleted method should fail");
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + ".methodDeleted()", e);
		}
	}

	/**
	 * Testing Class.getDeclaredMethod and Method.invoke
	 * 
	 * What happens if we invoke a cached Method object when the method it refers to was deleted?
	 */
	@Test
	public void test_cacheGetDeclaredMethodDeletedAndInvoke() throws Exception {
		Method m = (Method) getDeclaredMethod("methodDeleted").returnValue;
		Result r = invoke(m);
		Assert.assertEquals(37, r.returnValue);

		reloadType("002");

		try {
			invoke(m);
			fail("Invoking a deleted method should fail");
		} catch (ResultException e) {
			assertNoSuchMethodError(TARGET_CLASS_NAME + ".methodDeleted()I", e);
		}
	}

	/**
	 * Testing Class.getDeclaredMethod and Method.invoke
	 * 
	 * Can we retrieve and invoke a method that existed before we reloaded the type, and was changed later.
	 */
	@Test
	public void test_getDeclaredMethodChangedAndInvoke() throws Exception {
		Result r = getDeclaredMethodAndInvoke("methodChanged");
		Assert.assertEquals(1, r.returnValue);

		reloadType("002");

		r = getDeclaredMethodAndInvoke("methodChanged");
		Assert.assertEquals(2, r.returnValue);
	}

	/**
	 * Testing Class.getDeclaredMethod and Method.invoke
	 * 
	 * A method with a parameter - first it didn't exist - then is was added
	 */
	@Test
	public void test_getDeclaredMethod_param_Added() throws Exception {
		try {
			getDeclaredMethod("doubleIt", String.class);
			Assert.fail("Method shouldn't have existed");
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + ".doubleIt(java.lang.String)", e);
		}

		reloadType("002");

		Result r = getDeclaredMethod("doubleIt", String.class);
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".doubleIt(java.lang.String)", r);

		r = invoke((Method) r.returnValue, "hi");
		Assert.assertEquals("hihi", r.returnValue);
	}

	/**
	 * Testing Class.getDeclaredMethod and Method.invoke
	 * 
	 * A method with a parameter - existed at first - then it was changed
	 */
	@Test
	public void test_getDeclaredMethod_param_Changed() throws Exception {
		Result r = getDeclaredMethod("changeIt", String.class);
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".changeIt(java.lang.String)", r);
		r = invoke((Method) r.returnValue, "hoho");
		Assert.assertEquals("hohoho!", r.returnValue);

		reloadType("002");

		r = getDeclaredMethod("changeIt", String.class);
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".changeIt(java.lang.String)", r);

		r = invoke((Method) r.returnValue, "hoho");
		Assert.assertEquals("hoho hoho!", r.returnValue);
	}

	/**
	 * Testing Class.getDeclaredMethod and Method.invoke
	 * 
	 * A method with a parameter - existed at first - then it was changed: DIFFERENT RETURN TYPE
	 */
	@Test
	public void test_getDeclaredMethod_param_ChangedReturnType() throws Exception {
		Result r = getDeclaredMethod("changeReturn", String.class);
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".changeReturn(java.lang.String)", r);
		r = invoke((Method) r.returnValue, "hoho");
		Assert.assertEquals("hohoho!", r.returnValue);

		reloadType("002");

		Result m = getDeclaredMethod("changeReturn", String.class);
		assertMethod("public int " + TARGET_CLASS_NAME + ".changeReturn(java.lang.String)", m);

		r = invoke((Method) m.returnValue, "hoho");
		Assert.assertEquals(4, r.returnValue);
	}

	/**
	 * Testing Class.getDeclaredMethod and Method.invoke
	 * 
	 * A method with a parameter - existed at first - then it was changed: DIFFERENT RETURN TYPE
	 * 
	 * If we held on to the original method object, we should NOT be able to invoke it anymore (different return type should be
	 * treated as different method!
	 */
	@Test
	public void test_getDeclaredMethodCachedChangedReturnType() throws Exception {
		Result r = getDeclaredMethod("changeReturn", String.class);
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".changeReturn(java.lang.String)", r);
		Method m = (Method) r.returnValue;

		r = invoke(m, "hoho");
		Assert.assertEquals("hohoho!", r.returnValue);

		reloadType("002");

		try {
			r = invoke(m, "hoho");
			Assert.fail("Method return type changed, shouldn't be able to call it anymore");
		} catch (ResultException e) {
			assertNoSuchMethodError(TARGET_CLASS_NAME + ".changeReturn(Ljava/lang/String;)Ljava/lang/String;", e);
		}
	}

	/**
	 * Testing Class.getDeclaredMethod and Method.invoke
	 * 
	 * A method with two paramters - existed at first - then it was changed
	 */
	@Test
	public void test_getDeclaredMethodTwoParamsChanged() throws Exception {
		Result r = getDeclaredMethod("changeThem", String.class, int.class);
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".changeThem(java.lang.String,int)", r);
		r = invoke((Method) r.returnValue, "ho", 2);
		Assert.assertEquals("ho2", r.returnValue);

		reloadType("002");

		r = getDeclaredMethod("changeThem", String.class, int.class);
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".changeThem(java.lang.String,int)", r);
		r = invoke((Method) r.returnValue, "ho", 2);
		Assert.assertEquals("hoho", r.returnValue);
	}

	/**
	 * Invoking a private method by means of "invoke" call, from within the class containing the target method should be allowed.
	 */
	@Test
	public void test_invokePrivateMethodAllowed() throws Exception {
		Result r = getDeclaredMethodAndInvoke("callPrivateMethod");
		Assert.assertEquals("privateMethod result", r.returnValue);

		reloadType("002");

		r = getDeclaredMethodAndInvoke("callPrivateMethod");
		Assert.assertEquals("new privateMethod result", r.returnValue);
	}

	/**
	 * Invoking a public method inside a default class by means of "invoke" call, from within a class in the same package as the
	 * target class should be allowed.
	 */
	@Test
	public void test_invokePublicMethodInDefaultClassAllowed() throws Exception {
		ReloadableType defaultClass = reloadableClass(TARGET_PACKAGE + "." + "DefaultClass");

		Result r = getDeclaredMethodAndInvoke("callPublicMethodOnDefaultClass");
		Assert.assertEquals(82, r.returnValue);

		reloadType(defaultClass, "002");

		r = getDeclaredMethodAndInvoke("callPublicMethodOnDefaultClass");
		Assert.assertEquals(999, r.returnValue);
	}

	/**
	 * Invoking a public method inside a default class by means of "invoke" call, from within a class in the same package as the
	 * target class should be allowed.
	 */
	@Test
	public void test_invokePublicMethodInNonReloadableDefaultClassAllowed() throws Exception {
		//		Class<?> defaultClass = 
		nonReloadableClass(TARGET_PACKAGE + "." + "DefaultClass");

		Result r = getDeclaredMethodAndInvoke("callPublicMethodOnDefaultClass");
		Assert.assertEquals(82, r.returnValue);
	}

	/**
	 * When a Method is "regotten" the newly gotten Method will have to be a "fresh" method object with its accessibility flag NOT
	 * set. (This is the behaviour on Sun JVM, I'm not sure if this specified or accidental though, but some comments in the Method
	 * class source code suggest that it is required... see code of the 'copy' method and its comments)
	 */
	private void doTestAccessibleFlagIsRefreshed(String scope) throws ResultException {
		String methodToCall = scope + "Method";
		String expectMsg = "Class " + INVOKER_CLASS_NAME + " " + "can not access a member of class " + TARGET_CLASS_NAME + " "
				+ "with modifiers \"" + (scope.equals("default") ? "" : scope) + "\"";

		//First... we do set the Access flag, it should work!
		Result r = runOnInstance(callerClazz, callerInstance, "callMethodWithAccess", methodToCall, true);
		Assert.assertEquals(r.returnValue, methodToCall + " result");

		//Then... we do not set the Access flag, it should fail!
		try {
			r = runOnInstance(callerClazz, callerInstance, "callMethodWithAccess", methodToCall, false);
			Assert.fail("Without setting access flag shouldn't be allowed!");
		} catch (ResultException e) {
			assertIllegalAccess(expectMsg, e);
		}

		//Finally, this should also still work... after we reload the type!
		reloadType("002");

		//First... we do set the Access flag, it should work!
		r = runOnInstance(callerClazz, callerInstance, "callMethodWithAccess", methodToCall, true);
		Assert.assertEquals(r.returnValue, "new " + methodToCall + " result");

		//Then... we do not set the Access flag, it should not be allowed!
		try {
			r = runOnInstance(callerClazz, callerInstance, "callMethodWithAccess", methodToCall, false);
			Assert.fail("Without setting access flag shouldn't be allowed!");
		} catch (ResultException e) {
			assertIllegalAccess(expectMsg, e);
		}
	}

	//Run the above 'test template' few times, for different kinds of scopes

	@Test
	public void test_accessibleFlagIsRefreshedForPrivate() throws Exception {
		doTestAccessibleFlagIsRefreshed("private");
	}

	@Test
	public void test_accessibleFlagIsRefreshedForProtected() throws Exception {
		doTestAccessibleFlagIsRefreshed("protected");
	}

	@Test
	public void test_accessibleFlagIsRefreshedForDefault() throws Exception {
		doTestAccessibleFlagIsRefreshed("default");
	}

	/**
	 * Test getDeclaredMethod and invoke for protected inherited method.
	 */
	@Test
	public void test_invokeProtectedInheritedMethod() throws Exception {

		String subClassName = TARGET_PACKAGE + ".SubClassTarget";
		ReloadableType subTarget = registry.addType(subClassName, loadBytesForClass(subClassName));

		try {
			getDeclaredMethod(subTarget.getClazz(), "protectedMethod");
			Assert.fail("A protected inherited method should not be considered 'declared' in the subclass");
		} catch (ResultException e) {
			assertNoSuchMethodException(subClassName + ".protectedMethod()", e);
		}

		//Next check if we can call the super class method on the sub instance
		Object subInstance = subTarget.getClazz().newInstance();

		Method m = (Method) getDeclaredMethod("protectedMethod").returnValue;
		m.setAccessible(true); // because call is from different package!
		Result r = invokeOn(subInstance, m);
		Assert.assertEquals("protectedMethod result", r.returnValue);

		//Reload the subclass, method should now be defined on the subclass
		reloadType(subTarget, "002");

		r = getDeclaredMethod(subTarget.getClazz(), "protectedMethod");
		assertMethod("protected java.lang.String " + subClassName + ".protectedMethod()", r);

		m = (Method) r.returnValue;
		try {
			invokeOn(subInstance, m);
			Assert.fail("invoker class is in different package than target class shouldn't be allowed to invoke protected method!");
		} catch (ResultException e) {
			assertIllegalAccess("Class " + callerClazz.getName() + " can not access a member of class " + subTarget.dottedtypename
					+ " with modifiers \"protected\"", e);
		}

		m.setAccessible(true);
		r = invokeOn(subInstance, m);
		Assert.assertEquals("SubClassTarget002.protectedMethod", r.returnValue);
	}

	/**
	 * Test for a suspected 'stale' executor map cache bug. Turns out there wasn't actually a 'staleness' bug in the cache... but
	 * this test is useful nevertheless.
	 * <p>
	 * Scenario: user keeps a method object from a reloaded type. Which will be associated to a cached executor. When the type is
	 * subsequently reloaded again, is the correct method executed?
	 */
	@Test
	public void test_cacheReloadedMethod() throws Exception {
		reloadType("002");

		Method m = (Method) getDeclaredMethod("methodChanged").returnValue;
		Result r = invoke(m);
		assertEquals(2, r.returnValue);

		reloadType("003");
		r = invoke(m);
		assertEquals(3, r.returnValue);
	}

	/**
	 * Test related to calling 'invoke' on a method declared in superclass and overridden in the subclass (via a method object
	 * gotten from the superclass).
	 */
	@Test
	public void test_callInheritedOverridenMethod() throws Exception {

		Result r = getDeclaredMethod("overrideMethod");
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".overrideMethod()", r);
		Method m = (Method) r.returnValue;

		// Setup subClass and an instance of the subclass
		String subClassName = TARGET_PACKAGE + ".SubClassTarget";
		ReloadableType subTarget = registry.addType(subClassName, loadBytesForClass(subClassName));
		Object subInstance = subTarget.getClazz().newInstance();

		// Calling the superclass method on the subinstance should execute the subclass method!
		r = invokeOn(subInstance, m);
		assertEquals("SubClassTarget.overrideMethod", r.returnValue);

		// Now try what happens if we reload the subclass type, changing the method
		reloadType(subTarget, "002");
		r = invokeOn(subInstance, m);
		assertEquals("SubClassTarget002.overrideMethod", r.returnValue);

		// Now try what happens if we reload the subclass type, DELETING the method
		reloadType(subTarget, "003");
		r = invokeOn(subInstance, m);
		assertEquals("ClassTarget.overrideMethod", r.returnValue);
	}

	/**
	 * Test related to calling 'invoke' on a method declared in superclass and overridden in the subclass (via a method object
	 * gotten from the superclass).
	 * <p>
	 * Variant of the previous test, where the class containing the overridden method is reloaded. (This case is different because
	 * it will use CLV to determine an executor method).
	 */
	@Test
	public void test_callInheritedOverridenMethod2() throws Exception {
		reloadType(target, "002");

		Result r = getDeclaredMethod("overrideMethod");
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".overrideMethod()", r);
		Method m = (Method) r.returnValue;

		// Double check if we are using the right version...
		r = invoke(m);
		assertEquals("ClassTarget002.overrideMethod", r.returnValue);

		// Setup subClass and an instance of the subclass
		String subClassName = TARGET_PACKAGE + ".SubClassTarget";
		ReloadableType subTarget = registry.addType(subClassName, loadBytesForClass(subClassName));
		Object subInstance = subTarget.getClazz().newInstance();

		// Calling the superclass method on the subinstance should execute the subclass method!
		r = invokeOn(subInstance, m);
		assertEquals("SubClassTarget.overrideMethod", r.returnValue);

		// Now try what happens if we reload the subclass type, changing the method
		reloadType(subTarget, "002");
		r = invokeOn(subInstance, m);
		assertEquals("SubClassTarget002.overrideMethod", r.returnValue);

		// Now try what happens if we reload the subclass type, DELETING the method
		reloadType(subTarget, "003");
		r = invokeOn(subInstance, m);
		assertEquals("ClassTarget002.overrideMethod", r.returnValue);
	}

	/**
	 * Test related to calling 'invoke' on a method declared in superclass and overriden in the subclass (via a method object gotten
	 * from the superclass).
	 * <p>
	 * What if the super method is deleted in v002?
	 */
	@Test
	public void test_callInheritedOverridenDeletedMethod() throws Exception {
		Result r = getDeclaredMethod("overrideMethodDeleted");
		assertMethod("public java.lang.String " + TARGET_CLASS_NAME + ".overrideMethodDeleted()", r);
		Method m = (Method) r.returnValue;

		// Setup subClass and an instance of the subclass
		String subClassName = TARGET_PACKAGE + ".SubClassTarget";
		ReloadableType subTarget = registry.addType(subClassName, loadBytesForClass(subClassName));
		Object subInstance = subTarget.getClazz().newInstance();

		// Calling the superclass method on the subinstance should execute the subclass method!
		r = invokeOn(subInstance, m);
		assertEquals("SubClassTarget.overrideMethodDeleted", r.returnValue);

		// Now try what happens if we reload the superclass type, deleting the method
		reloadType("002");
		try {
			r = invokeOn(subInstance, m);
			fail("The method was deleted, should fail!");
		} catch (ResultException e) {
			assertNoSuchMethodError("reflection.targets.ClassTarget.overrideMethodDeleted()Ljava/lang/String;", e);
		}
	}

	/**
	 * Test invoking a static method.
	 */
	@Test
	public void test_invokeStaticMethod() throws Exception {
		Result r = getDeclaredMethod("staticMethod");
		assertMethod("public static java.lang.String " + TARGET_CLASS_NAME + ".staticMethod()", r);
		Method m = (Method) r.returnValue;

		// Calling the static method
		r = invokeOn(null, m); //pass in null, it shouldn't need an instance since it's static!
		assertEquals("ClassTarget.staticMethod", r.returnValue);

		reloadType("002");

		// Invoke again, using 'cached' copy of the method
		r = invokeOn(null, m); //pass in null, it shouldn't need an instance since it's static!
		assertEquals("ClassTarget002.staticMethod", r.returnValue);

		// Invoke again, using 'fresh' copy of the method
		m = (Method) getDeclaredMethod("staticMethod").returnValue;
		r = invokeOn(null, m); //pass in null, it shouldn't need an instance since it's static!
		assertEquals("ClassTarget002.staticMethod", r.returnValue);

		reloadType("003"); // static method deleted now
		// Invoke again, using 'cached' copy of the method
		try {
			r = invokeOn(null, m); //pass in null, it shouldn't need an instance since it's static!
			fail("The method was deleted, should not be able to call it");
		} catch (ResultException e) {
			assertNoSuchMethodError(TARGET_CLASS_NAME + ".staticMethod()Ljava/lang/String;", e);
		}
	}

	/**
	 * Test invoking a static method that didn't initialy exist.
	 */
	@Test
	public void test_invokeStaticMethodAdded() throws Exception {
		String methodName = "staticMethodAdded";
		try {
			Result r = getDeclaredMethod(methodName);
			fail("Method shouldn't exist at first!\n" + r.toString());
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + "." + methodName + "()", e);
		}

		reloadType("002");

		Result r = getDeclaredMethod(methodName);
		assertMethod("public static int " + TARGET_CLASS_NAME + "." + methodName + "()", r);
		Method m = (Method) r.returnValue;

		// Calling the static method
		r = invokeOn(null, m); //pass in null, it shouldn't need an instance since it's static!
		assertEquals(2, r.returnValue);

		reloadType("003");

		// Invoke again, using 'cached' copy of the method
		r = invokeOn(null, m); //pass in null, it shouldn't need an instance since it's static!
		assertEquals(3, r.returnValue);

		// Invoke again, using 'fresh' copy of the method
		m = (Method) getDeclaredMethod(methodName).returnValue;
		r = invokeOn(null, m); //pass in null, it shouldn't need an instance since it's static!
		assertEquals(3, r.returnValue);
	}

	/**
	 * Test invoking a static method that didn't initialy exist.
	 */
	@Test
	public void test_invokeStaticMethodAddedWithNullParams() throws Exception {
		String methodName = "staticMethodAdded";
		try {
			Result r = getDeclaredMethod(methodName);
			fail("Method shouldn't exist at first!\n" + r.toString());
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + "." + methodName + "()", e);
		}

		reloadType("002");

		Result r = getDeclaredMethod(methodName);
		assertMethod("public static int " + TARGET_CLASS_NAME + "." + methodName + "()", r);
		Method m = (Method) r.returnValue;

		Object[] params = null; // This should be ok, since the method expects no params (should not cause an NPE)

		// Calling the static method
		r = invokeOn(null, m, params); //pass in null, it shouldn't need an instance since it's static!
		assertEquals(2, r.returnValue);

		reloadType("003");

		// Invoke again, using 'cached' copy of the method
		r = invokeOn(null, m, params); //pass in null, it shouldn't need an instance since it's static!
		assertEquals(3, r.returnValue);

		// Invoke again, using 'fresh' copy of the method
		m = (Method) getDeclaredMethod(methodName).returnValue;
		r = invokeOn(null, m, params); //pass in null, it shouldn't need an instance since it's static!
		assertEquals(3, r.returnValue);
	}

	/**
	 * Test invoking a static method that didn't initialy exist.
	 */
	@Test
	public void test_invokeStaticMethodAddedWithArgs() throws Exception {
		String methodName = "staticMethodAddedWithArgs";
		try {
			Result r = getDeclaredMethod(methodName, int.class, String.class);
			fail("Method should not exist initially\n" + r.toString());
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + "." + methodName + "(int, java.lang.String)", e);
		}

		reloadType("002");

		Result r = getDeclaredMethod(methodName, int.class, String.class);
		assertMethod("public static java.lang.String " + TARGET_CLASS_NAME + "." + methodName + "(int,java.lang.String)", r);
		Method m = (Method) r.returnValue;

		// Calling the static method
		r = invokeOn(null, m, 123, "Hello"); //pass in null, it shouldn't need an instance since it's static!
		assertEquals("123Hello002", r.returnValue);

		reloadType("003");

		// Invoke again, using 'cached' copy of the method
		r = invokeOn(null, m, 456, "Hi"); //pass in null, it shouldn't need an instance since it's static!
		assertEquals("456Hi003", r.returnValue);

		// Invoke again, using 'fresh' copy of the method
		r = getDeclaredMethod(methodName, int.class, String.class);
		r = invokeOn(null, m, 456, "Hi"); //pass in null, it shouldn't need an instance since it's static!
		assertEquals("456Hi003", r.returnValue);
	}

	/**
	 * Test invoking a static... does it truly use static dispatch?
	 */
	@Test
	public void test_invokeStaticMethodOverriden() throws Exception {
		String methodName = "staticMethodAdded";
		try {
			Result r = getDeclaredMethod(methodName);
			fail("Method shouldn't exist at first!\n" + r.toString());
		} catch (ResultException e) {
			assertNoSuchMethodException(TARGET_CLASS_NAME + "." + methodName + "()", e);
		}

		reloadType("002");

		// Setup subClass and an instance of the subclass
		String subClassName = TARGET_PACKAGE + ".SubClassTarget";
		ReloadableType subTarget = registry.addType(subClassName, loadBytesForClass(subClassName));
		Object subInstance = subTarget.getClazz().newInstance();

		//Double check, the subclass 'overrides' the static method
		assertMethod("public static int " + subClassName + "." + methodName + "()",
				getDeclaredMethod(subTarget.getClazz(), methodName));

		Result r = getDeclaredMethod(methodName);
		assertMethod("public static int " + TARGET_CLASS_NAME + "." + methodName + "()", r);
		Method m = (Method) r.returnValue;

		// Calling the static method seemingly on a 'subinstance' the instance should be ignored!
		r = invokeOn(subTarget, m);
		assertEquals(2, r.returnValue);

		reloadType("003");

		// Invoke again, using 'cached' copy of the method
		r = invokeOn(subInstance, m);
		assertEquals(3, r.returnValue);

		// Invoke again, using 'fresh' copy of the method
		m = (Method) getDeclaredMethod(methodName).returnValue;
		r = invokeOn(subInstance, m);
		assertEquals(3, r.returnValue);
	}

	/**
	 * We should be able to invoke methods inherited from non-reloadable types on instances of reloadable types.
	 */
	@Test
	public void test_callInheritedNonReloadableMethod() throws Exception {
		//We need a method that is inherited from a non-reloadable type for this scenario
		Result r = getDeclaredMethod(Object.class, "toString");
		assertMethod("public java.lang.String java.lang.Object.toString()", r);
		Method m = (Method) r.returnValue;

		// Setup subClass and an instance of the subclass
		String subClassName = TARGET_PACKAGE + ".SubClassTarget";
		ReloadableType subTarget = registry.addType(subClassName, loadBytesForClass(subClassName));
		Object subInstance = subTarget.getClazz().newInstance();

		//Try invoking on instance of the reloadable class 
		Object instance = target.getClazz().newInstance();
		r = invokeOn(instance, m);
		assertEquals(instance.toString(), r.returnValue);

		//Try invoking it also on the subclass (so we get a lookup spanning more than one
		// level of going up in the hierarchy in this test case as well)
		r = invokeOn(subInstance, m);
		assertEquals(subInstance.toString(), r.returnValue);

		//Try all of this again after the types have been reloaded 
		reloadType("002");
		reloadType(subTarget, "002");

		//Try invoking on instance of the reloadable class 
		r = invokeOn(instance, m);
		assertEquals(instance.toString(), r.returnValue);

		//Try invoking it also on the subclass (so we get a lookup spanning more than one
		// level of going up in the hierarchy in this test case as well)
		r = invokeOn(subInstance, m);
		assertEquals(subInstance.toString(), r.returnValue);

		//In version 003, we added our own toString method that should capture the invocation
		reloadType("003");

		//Try invoking on instance of the reloadable class 
		r = invokeOn(instance, m);
		assertEquals("ClassTarget003.toString", r.returnValue);

		//Try invoking it also on the subclass (so we get a lookup spanning more than one
		// level of going up in the hierarchy in this test case as well)
		r = invokeOn(subInstance, m);
		assertEquals("ClassTarget003.toString", r.returnValue);
	}

	/**
	 * Scenario where method lookup spills over from the
	 * reloadable world into the non-reloadable world. This can only happen if are looking for a method that is declared on a
	 * reloadable type, but we need to find the implementation in a non-reloadable one.
	 */
	@Test
	public void test_callReloadableMethodWithNonReloadableImplementation() throws Exception {
		//Scenario requires a method that is reloadable but not implemented by a reloadable
		//class. The followig circumstances trigger this condition:

		// Situation involves three types:

		// A non reloadable superClass
		String superClassName = "reflection.NonReloadableSuperClass";
		Class<?> superClass = nonReloadableClass(superClassName);

		// A reloadable interface
		String interfaceName = TARGET_PACKAGE + ".InterfaceTarget";
		ReloadableType interfaceTarget = reloadableClass(interfaceName);

		ClassPrinter.print(interfaceTarget.getBytesLoaded());

		// A reloadable class
		String subclassName = TARGET_PACKAGE + ".SubClassImplementsInterface";
		ReloadableType subClass = reloadableClass(subclassName);

		// These types relate to one another as follows:

		//1) the method 'm' must be declared in the reloadable interface
		String interfaceMethodName = "interfaceMethod";
		Result r = getDeclaredMethod(interfaceTarget.getClazz(), interfaceMethodName);
		assertMethod("public abstract java.lang.String " + interfaceName + "." + interfaceMethodName + "()", r);
		Method m = (Method) r.returnValue;

		//2) The reloadable type implements this interface (without providing its own implementation of m)
		assertTrue(interfaceTarget.getClazz().isAssignableFrom(subClass.getClazz()));
		try {
			r = getDeclaredMethod(subClass.getClazz(), interfaceMethodName);
			fail("Assuming that interface implementation is inherited, not directly implemented");
		} catch (ResultException e) {
			assertNoSuchMethodException(subclassName + "." + interfaceMethodName + "()", e);
		}

		//3) A non-reloadable superclass provides the actual implementation of m via inheritance
		r = getDeclaredMethod(superClass, interfaceMethodName);
		assertMethod("public java.lang.String " + superClassName + "." + interfaceMethodName + "()", r);

		//4) Invoke the interface method on an instance of the subclass... should cause lookup
		//   to find implementation in non-reloadable superclass
		Object subInstance = subClass.getClazz().newInstance();
		r = invokeOn(subInstance, m);
		assertEquals("NonReloadableSuperClass.interfaceMethod", r.returnValue);

	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// getDeclaredField tests
	//
	// We don't do reloadable fields at the moment, but fields should nevertheless work, if not changed.

	private Result getDeclaredField(String fieldName) throws ResultException {
		return runOnInstance(callerClazz, callerInstance, "callGetDeclaredField", target.getClazz(), fieldName);
	}

	private void assertField(String expectedSignature, Result r) {
		Assert.assertEquals(Field.class, r.returnValue.getClass());
		Field f = (Field) r.returnValue;
		Assert.assertEquals(expectedSignature, f.toString());
	}

	@Test
	public void test_getDeclaredField() throws Exception {
		Result r = getDeclaredField("myField");
		assertField("public int " + TARGET_CLASS_NAME + ".myField", r);

		reloadType("002");

		r = getDeclaredField("myField");
		assertField("public int " + TARGET_CLASS_NAME + ".myField", r);
	}

	/**
	 * Does getDeclaredMethod/invoke work as expected on non-reloadable types?
	 */
	@Test
	public void test_getDeclaredMethodNonReloadable() throws Exception {
		Result r = getDeclaredMethod(String.class, "indexOf", String.class, int.class);
		assertMethod("public int java.lang.String.indexOf(java.lang.String,int)", r);
		r = invokeOn("Some text", (Method) r.returnValue, "ex", 0);
		Assert.assertEquals("Some text".indexOf("ex"), r.returnValue);
	}

	/**
	 * Test invoke with 'null' params
	 */
	@Test
	public void test_InvokeWithNullParams() throws Exception {
		Method m = (Method) getDeclaredMethod("methodChanged").returnValue;
		Object[] params = null;
		Result r = invoke(m, params);
		Assert.assertEquals(1, r.returnValue);

		reloadType("002");

		r = invoke(m, params);
		Assert.assertEquals(2, r.returnValue);
	}

	// Test below is disabled: we aren't (yet) supporting changing modifiers on classes.
	//	/**
	//	 * Do we pick up on changed modifiers in reloaded class?
	//	 */
	//	@Test
	//	public void test_ClassGetModifiers() throws Exception {
	//		ReloadableType targetClass = reloadableClass(TARGET_PACKAGE + ".ChangeModClass");
	//		Result r = runOnInstance(callerClazz, callerInstance, "callClassGetModifiers", targetClass.getClazz());
	//		Assert.assertEquals(Modifier.PUBLIC, r.returnValue);
	//
	//		reloadType(targetClass, "002");
	//		r = runOnInstance(callerClazz, callerInstance, "callClassGetModifiers", targetClass.getClazz());
	//		Assert.assertEquals(Modifier.FINAL, r.returnValue);
	//	}

	/**
	 * Test invoke with an explict params that is the empty array
	 */
	@Test
	public void test_InvokeWithEmptyParams() throws Exception {
		Method m = (Method) getDeclaredMethod("methodChanged").returnValue;
		Object[] params = new Object[0];
		Result r = invoke(m, params);
		Assert.assertEquals(1, r.returnValue);

		reloadType("002");

		r = invoke(m, params);
		Assert.assertEquals(2, r.returnValue);
	}

	/**
	 * Test to see if we pick up the correct method if there are multiple methods differing only in return type. The typical case
	 * would be when a class overrides a method while narrowing the return type. The compiler will in this case introdycue a bridge
	 * method, giving the class two methods differing only in return type. The bridge method has the 'wider' return type type and is
	 * a synthetic method.
	 */
	@Test
	public void test_GetMethodWithBridgeMethods() throws Exception {
		registry = getTypeRegistry("reflection.bridgemethods..*");
		ReloadableType targetClass = reloadableClass("reflection.bridgemethods.ClassWithBridgeMethod");

		Result r = getDeclaredMethod(targetClass.getClazz(), "clone");
		assertMethod("protected java.lang.Object reflection.bridgemethods.ClassWithBridgeMethod.clone()"
				+ " throws java.lang.CloneNotSupportedException", r); // In the first version of the class, there's only one method and it returns object

		reloadType(targetClass, "002");
		r = getDeclaredMethod(targetClass.getClazz(), "clone");
		assertMethod("protected " + targetClass.getName() + " " + targetClass.getName() + ".clone()"
				+ " throws java.lang.CloneNotSupportedException", r); // In the first version of the class, there's only one method and it returns object
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Testing Class.getMethod
	//
	// javap:
	//     public java.lang.reflect.Method getMethod(java.lang.String, java.lang.Class[])       
	//     throws java.lang.NoSuchMethodException, java.lang.SecurityException;

	private Result getMethod(Class<?> clazz, String name, Class<?>... params) throws ResultException {
		return runOnInstance(callerClazz, callerInstance, "callGetMethod", clazz, name, params);
	}

	/**
	 * Try Class.getMethod first without reloading...
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetMethod() throws Exception {
		String subClassName = TARGET_PACKAGE + ".SubClassTarget";
		ReloadableType subClass = reloadableClass(subClassName);

		//Try getting and calling method that is declared in this class
		Result r = getMethod(subClass.getClazz(), "subMethod");
		Method m = assertMethod("public java.lang.String " + subClassName + ".subMethod()", r);

		Object instance = subClass.getClazz().newInstance();
		r = invokeOn(instance, m);
		assertEquals("SubClassTarget.subMethod", r.returnValue);

		//Try getting and calling method that is declared in the reloadable superclass
		r = getMethod(subClass.getClazz(), "methodChanged");
		m = assertMethod("public int " + TARGET_CLASS_NAME + ".methodChanged()", r);
		r = invokeOn(instance, m);
		assertEquals(1, r.returnValue);

		//Try getting and calling method that is inherited from non-reloadable superclass
		r = getMethod(subClass.getClazz(), "toString");
		m = assertMethod("public java.lang.String java.lang.Object.toString()", r);
		r = invokeOn(instance, m);
		assertEquals(instance.toString(), r.returnValue);
	}

	@Test
	public void testSomeBug() throws Exception {
		reloadableClass(TARGET_PACKAGE + ".GetMethodInterface");

		String className = TARGET_PACKAGE + ".GetMethodClass";
		ReloadableType rtype = reloadableClass(className);
		reloadType(rtype, "002");

		TypeDescriptor descriptor = rtype.getLatestTypeDescriptor();
		for (MethodMember m : descriptor.getMethods()) {
			System.out.println(m);
			if (m.getName().equals("im2")) {
				return; //Fine!
			}
		}
		fail("There should be an im2 method");
	}

}
