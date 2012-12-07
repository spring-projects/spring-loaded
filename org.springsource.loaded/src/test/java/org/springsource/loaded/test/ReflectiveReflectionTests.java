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

import org.junit.Test;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;


/**
 * These tests verify the correct behaviour for reflective calls made using reflection. They should all be intercepted by the
 * method.invoke() that runs and in the handler for that (ReflectiveInterceptor.jlrMethodInvoke) they should be recognized and
 * dispatched to their intercepting function in the ReflectiveInterceptor.
 * 
 * @author Andy Clement
 * @since 0.7.3
 */
public class ReflectiveReflectionTests extends SpringLoadedTests {

	// java.lang.reflect.Constructor

	@Test
	public void testJLRCGetAnnotations() throws Exception {
		String t = "iri.JLRCGetAnnotations";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@reflection.AnnoT()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@java.lang.Deprecated()", result.returnValue);
	}

	@Test
	public void testJLRCGetDecAnnotations() throws Exception {
		String t = "iri.JLRCGetDecAnnotations";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@reflection.AnnoT()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@java.lang.Deprecated()", result.returnValue);
	}

	@Test
	public void testJLRCGetAnnotation() throws Exception {
		String t = "iri.JLRCGetAnnotation";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("@reflection.AnnoT() null", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("null @java.lang.Deprecated()", result.returnValue);
	}

	@Test
	public void testJLRCIsAnnotationPresent() throws Exception {
		String t = "iri.JLRCIsAnnotationPresent";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("truefalse", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("falsetrue", result.returnValue);
	}

	@Test
	public void testJLRCGetParameterAnnotations() throws Exception {
		String t = "iri.JLRCGetParameterAnnotations";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("[1:@reflection.AnnoT()][0:][1:@reflection.AnnoT2()]", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("[1:@reflection.AnnoT2()][1:@reflection.AnnoT()][0:]", result.returnValue);
	}

	@Test
	public void testJLRCNewInstance() throws Exception {
		String t = "iri.JLRCNewInstance";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("instance", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("instance", result.returnValue);
	}

	// java.lang.reflect.Method

	@Test
	public void testJLRMGetAnnotations() throws Exception {
		String t = "iri.JLRMGetAnnotations";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@reflection.AnnoT()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@java.lang.Deprecated()", result.returnValue);
	}

	@Test
	public void testJLRMGetDecAnnotations() throws Exception {
		String t = "iri.JLRMGetDecAnnotations";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@reflection.AnnoT()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@java.lang.Deprecated()", result.returnValue);
	}

	@Test
	public void testJLRMGetAnnotation() throws Exception {
		String t = "iri.JLRMGetAnnotation";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("@reflection.AnnoT() null", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("null @java.lang.Deprecated()", result.returnValue);
	}

	@Test
	public void testJLRMIsAnnotationPresent() throws Exception {
		String t = "iri.JLRMIsAnnotationPresent";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("truefalse", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("falsetrue", result.returnValue);
	}

	@Test
	public void testJLRMGetParameterAnnotations() throws Exception {
		String t = "iri.JLRMGetParameterAnnotations";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("[1:@reflection.AnnoT()][0:][1:@reflection.AnnoT2()]", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("[1:@reflection.AnnoT2()][1:@reflection.AnnoT()][0:]", result.returnValue);
	}

	@Test
	public void testJLRMInvoke() throws Exception {
		String t = "iri.JLRMInvoke";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("ran", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("alsoran", result.returnValue);
		rtype.loadNewVersion("3", retrieveRename(t, t + "3", t + "3:" + t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("abc3", result.returnValue);
	}

	// java.lang.reflect.Field

	@Test
	public void testJLRFGetAnnotation() throws Exception {
		String t = "iri.JLRFGetAnnotation";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("@reflection.AnnoT() null", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("null @java.lang.Deprecated()", result.returnValue);
	}

	@Test
	public void testJLRFIsAnnotationPresent() throws Exception {
		String t = "iri.JLRFIsAnnotationPresent";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("truefalse", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("falsetrue", result.returnValue);
	}

	@Test
	public void testJLRFGetAnnotations() throws Exception {
		String t = "iri.JLRFGetAnnotations";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@reflection.AnnoT()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@java.lang.Deprecated()", result.returnValue);
	}

	@Test
	public void testJLRFGetDecAnnotations() throws Exception {
		String t = "iri.JLRFGetDecAnnotations";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@reflection.AnnoT()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@java.lang.Deprecated()", result.returnValue);

	}

	@Test
	public void testJLRFGet() throws Exception {
		String t = "iri.JLRFGet";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		System.out.println(result);
		assertEquals("hello", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("goodbye", result.returnValue);
	}

	// All the other gets!
	@Test
	public void testJLRFGetTheRest() throws Exception {
		String t = "iri.JLRFGetTheRest";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		System.out.println(result);
		assertEquals("true 123 a 3.141 33.0 12345 444 99", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("true 23 b 4.141 43.0 22345 544 999", result.returnValue);
	}

	@Test
	public void testJLRFSet() throws Exception {
		String t = "iri.JLRFSet";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		System.out.println(result);
		assertEquals("hello", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("goodbye", result.returnValue);
	}

	// All the other sets!
	@Test
	public void testJLRFSetTheRest() throws Exception {
		String t = "iri.JLRFSetTheRest";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		System.out.println(result);
		assertEquals("true 123 a 3.14 6.5 32767 555 333", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("true 111 b 6.28 13.0 11122 222 777", result.returnValue);
	}

	@Test
	public void testJLRFSetTheRestVariant() throws Exception {
		String t = "iri.JLRFSetTheRestVariant";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		System.out.println(result);
		assertEquals("true 123 a 3.14 6.5 32767 555 333", result.returnValue);
		rtype.loadNewVersion(rtype.bytesInitial);//retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("true 123 a 3.14 6.5 32767 555 333", result.returnValue);
	}

	// java.lang.Class 

	@Test
	public void testJLClassGetField() throws Exception {
		String t = "iri.JLCGetField";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("foo", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("bar", result.returnValue);
	}

	@Test
	public void testJLClassGetFields() throws Exception {
		String t = "iri.JLCGetFields";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("0:", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		//	ClassPrinter.print(rtype.getLatestExecutorBytes());
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:anInt", result.returnValue);
	}

	@Test
	public void testJLClassGetConstructors() throws Exception {
		String t = "iri.JLCGetConstructors";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:iri.JLCGetConstructors()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("2:iri.JLCGetConstructors() iri.JLCGetConstructors(String)", result.returnValue);
	}

	@Test
	public void testJLClassGetConstructor() throws Exception {
		String t = "iri.JLCGetConstructor";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("iri.JLCGetConstructor()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("iri.JLCGetConstructor(String)", result.returnValue);
	}

	@Test
	public void testJLClassGetConstructorPrivate() throws Exception {
		String t = "iri.JLCGetConstructorB";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("iri.JLCGetConstructorB()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		try {
			result = runUnguarded(rtype.getClazz(), "run");
			fail();
		} catch (InvocationTargetException ite) {
			assertTrue(ite.getCause() instanceof NoSuchMethodException);
			assertEquals("iri.JLCGetConstructorB.<init>(java.lang.String)", ite.getCause().getMessage());
		}
	}

	@Test
	public void testJLClassGetDeclaredConstructors() throws Exception {
		String t = "iri.JLCGetDecConstructors";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:iri.JLCGetDecConstructors()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("2:iri.JLCGetDecConstructors() iri.JLCGetDecConstructors(String)", result.returnValue);
	}

	@Test
	public void testJLClassGetDeclaredConstructor() throws Exception {
		String t = "iri.JLCGetDecConstructor";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("iri.JLCGetDecConstructor()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("iri.JLCGetDecConstructor(String)", result.returnValue);
	}

	@Test
	public void testJLClassGetDeclaredField() throws Exception {
		String t = "iri.JLCGetDecField";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("foo", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("bar", result.returnValue);
	}

	@Test
	public void testJLClassGetDeclaredFields() throws Exception {
		String t = "iri.JLCGetDecFields";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:aString", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("2:aString anInt", result.returnValue);
	}

	@Test
	public void testJLClassGetDeclaredMethod() throws Exception {
		String t = "iri.JLCGetDecMethod";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("foo()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("bar()", result.returnValue);
		rtype.loadNewVersion("3", retrieveRename(t, t + "3", t + "3:" + t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("bar2(String,int)", result.returnValue);
	}

	@Test
	public void testJLClassGetDeclaredMethods() throws Exception {
		String t = "iri.JLCGetDecMethods";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("3:foo() main(String[]) run()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("4:bar(String) foo() main(String[]) run()", result.returnValue);
	}

	@Test
	public void testJLClassGetMethod() throws Exception {
		String t = "iri.JLCGetMethod";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("foo()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("bar()", result.returnValue);
		rtype.loadNewVersion("3", retrieveRename(t, t + "3", t + "3:" + t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("bar2(String,int)", result.returnValue);
	}

	@Test
	public void testJLClassGetMethods() throws Exception {
		String t = "iri.JLCGetMethods";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals(
				"20:equals(Object) foo() format(Annotation[]) format(Constructor) format(Constructor[]) format(Field) format(Field[]) format(Method) format(Method[]) getClass() hashCode() main(String[]) notify() notifyAll() run() sortAndPrintNames(List) toString() wait() wait(long) wait(long,int)",
				result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals(
				"21:bar(String) equals(Object) foo() format(Annotation[]) format(Constructor) format(Constructor[]) format(Field) format(Field[]) format(Method) format(Method[]) getClass() hashCode() main(String[]) notify() notifyAll() run() sortAndPrintNames(List) toString() wait() wait(long) wait(long,int)",
				result.returnValue);
	}

	@Test
	public void testJLClassGetModifiers() throws Exception {
		String t = "iri.JLCGetModifiers";
		TypeRegistry r = getTypeRegistry(t + ",iri.Helper");
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		ReloadableType rtypeh = r.addType("iri.Helper", loadBytesForClass("iri.Helper"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("0", result.returnValue);
		rtypeh.loadNewVersion(rtypeh.bytesInitial);
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("0", result.returnValue);
	}

	@Test
	public void testJLClassIsAnnotationPresent() throws Exception {
		String t = "iri.JLCIsAnnotationPresent";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("true", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("false", result.returnValue);
	}

	@Test
	public void testJLClassNewInstance() throws Exception {
		String t = "iri.JLCNewInstance";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("I am an instance", result.returnValue);
		rtype.loadNewVersion(rtype.bytesInitial);
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("I am an instance", result.returnValue);
	}

	@Test
	public void testJLClassGetDeclaredAnnotations() throws Exception {
		String t = "iri.JLCGetDecAnnotations";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@java.lang.Deprecated()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@reflection.AnnoT()", result.returnValue);
	}

	@Test
	public void testJLClassGetAnnotations() throws Exception {
		String t = "iri.JLCGetAnnotations";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@reflection.AnnoT()", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1:@java.lang.Deprecated()", result.returnValue);
	}

	@Test
	public void testJLClassGetAnnotation() throws Exception {
		String t = "iri.JLCGetAnnotation";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("@reflection.AnnoT() null", result.returnValue);
		rtype.loadNewVersion(retrieveRenameRetarget(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("null @java.lang.Deprecated()", result.returnValue);
	}

	// general

	@Test
	public void testConstructorReflectiveInvocation() throws Exception {
		String t = "iri.Ctor";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		//		rtype.loadNewVersion(rtype.bytesInitial);
		result = runStaticUnguarded(rtype.getClazz(), "run");
		assertEquals("instance", result.returnValue);
	}

	// ---

	private byte[] retrieveRenameRetarget(String t) {
		return retrieveRename(t, t + "2", t + "2:" + t);
	}

}
