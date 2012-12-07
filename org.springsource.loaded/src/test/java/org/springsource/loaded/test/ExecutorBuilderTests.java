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
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils;
import org.springsource.loaded.test.infra.ClassPrinter;


/**
 * Tests for creation of the executor instances that run the code
 * 
 * @author Andy Clement
 */
public class ExecutorBuilderTests extends SpringLoadedTests {

	/**
	 * Check properties of the newly created executor.
	 */
	@Test
	public void basicExternals() throws Exception {
		String t = "executor.TestOne";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		reload(rtype, "37");
		Class<?> clazz = rtype.getLatestExecutorClass();
		Assert.assertEquals(Utils.getExecutorName(t, "37"), clazz.getName());
		Assert.assertEquals(3, clazz.getDeclaredMethods().length);
		Assert.assertEquals(1, clazz.getDeclaredFields().length);
	}

	/**
	 * Check internal structure of the newly created executor.
	 */
	@Test
	public void basicInternalsLocalVariables() throws Exception {
		String t = "executor.TestOne";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		reload(rtype, "37");
		checkLocalVariables(rtype.getLatestExecutorBytes(), "foo(Lexecutor/TestOne;Ljava/lang/String;)J",
				"thiz:Lexecutor/TestOne;", "s:Ljava/lang/String;");
	}

	@Test
	public void codeStructure() throws Exception {
		String tclass = "executor.TestOne";

		TypeRegistry typeRegistry = getTypeRegistry(tclass);

		ReloadableType rtype = typeRegistry.addType(tclass, loadBytesForClass(tclass));

		// Reload it (triggers creation of dispatcher/executor)
		rtype.loadNewVersion("2", rtype.bytesInitial);

		// @formatter:off
		checkType(rtype.getLatestExecutorBytes(),
				"CLASS: executor/TestOne$$E2 v50 0x0001(public) super java/lang/Object\n"+
				"SOURCE: TestOne.java null\n"+
				"FIELD 0x0001(public) i I\n"+
				"METHOD: 0x0009(public static) ___init___(Lexecutor/TestOne;)V\n"+
				"    CODE\n"+
				" L0\n"+
				"    ALOAD 0\n"+
				"    POP\n"+
				" L1\n"+
				"    ALOAD 0\n"+
				"    BIPUSH 101\n"+
				"    LDC 0\n"+
				"    LDC i\n"+
				"    INVOKESTATIC org/springsource/loaded/TypeRegistry.instanceFieldInterceptionRequired(ILjava/lang/String;)Z\n"+
				"    IFEQ L2\n"+
				"    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;\n"+
				"    SWAP\n"+
				"    DUP_X1\n"+
				"    LDC i\n"+
				"    INVOKESPECIAL executor/TestOne.r$set(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V\n"+
				"    GOTO L3\n"+
				" L2\n"+
				"    PUTFIELD executor/TestOne.i I\n"+
				" L3\n"+
				"    RETURN\n"+
				" L4\n"+
				"METHOD: 0x0009(public static) foo(Lexecutor/TestOne;Ljava/lang/String;)J\n"+
				"    CODE\n"+
				" L0\n"+
				"    ALOAD 1\n"+
				"    INVOKESTATIC java/lang/Long.parseLong(Ljava/lang/String;)J\n"+
				"    LRETURN\n"+
				" L1\n"+
				"METHOD: 0x0009(public static) hashCode(Lexecutor/TestOne;)I\n"+
				"    CODE\n"+
				" L0\n"+
				"    BIPUSH 37\n"+
				"    IRETURN\n"+
				" L1\n"+
				"\n");

		Assert.assertEquals(
				" L0\n"+
				"    ALOAD 1\n"+
				"    INVOKESTATIC java/lang/Long.parseLong(Ljava/lang/String;)J\n"+
				"    LRETURN\n"+
				" L1\n",
				toStringMethod(rtype.getLatestExecutorBytes(),"foo",false));
		// @formatter:on

		// @formatter:off
		Assert.assertEquals(
				" L0\n"+
				"    BIPUSH 37\n"+
				"    IRETURN\n"+
				" L1\n",
				toStringMethod(rtype.getLatestExecutorBytes(),"hashCode",false));
		// @formatter:on
	}

	@Test
	public void secondVersion() throws Exception {
		String tclass = "executor.TestOne";
		TypeRegistry typeRegistry = getTypeRegistry(tclass);

		ReloadableType rtype = typeRegistry.addType(tclass, loadBytesForClass(tclass));

		rtype.loadNewVersion("2", retrieveRename(tclass, tclass + "2"));

		// testing executor is for second version and not first

		// @formatter:off
		checkType(rtype.getLatestExecutorBytes(),
				"CLASS: executor/TestOne$$E2 v50 0x0001(public) super java/lang/Object\n"+
				"SOURCE: TestOne2.java null\n"+
				"FIELD 0x0001(public) i I\n"+
				"METHOD: 0x0009(public static) ___init___(Lexecutor/TestOne;)V\n"+
				"    CODE\n"+
				" L0\n"+
				"    ALOAD 0\n"+
				"    POP\n"+
				"    RETURN\n"+
				" L1\n"+
				"METHOD: 0x0009(public static) foo(Lexecutor/TestOne;Ljava/lang/String;)J\n"+
				"    CODE\n"+
				" L0\n"+
				"    ALOAD 1\n"+
				"    INVOKESTATIC java/lang/Long.parseLong(Ljava/lang/String;)J\n"+
				"    LRETURN\n"+
				" L1\n"+
				"METHOD: 0x0009(public static) hashCode(Lexecutor/TestOne;)I\n"+
				"    CODE\n"+
				" L0\n"+
				"    ALOAD 0\n"+
				"    LDC 0\n"+
				"    LDC i\n"+
				"    INVOKESTATIC org/springsource/loaded/TypeRegistry.instanceFieldInterceptionRequired(ILjava/lang/String;)Z\n"+
				"    IFEQ L1\n"+
				"    DUP\n"+
				"    LDC i\n"+
				"    INVOKESPECIAL executor/TestOne.r$get(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;\n"+
				"    CHECKCAST java/lang/Integer\n"+
				"    INVOKEVIRTUAL java/lang/Integer.intValue()I\n"+
				"    GOTO L2\n"+
				" L1\n"+
				"    GETFIELD executor/TestOne.i I\n"+
				" L2\n"+
				"    ICONST_2\n"+
				"    IMUL\n"+
				"    IRETURN\n"+
				" L3\n"+
				"\n");

		Assert.assertEquals(
				" L0\n"+
				"    ALOAD 1\n"+
				"    INVOKESTATIC java/lang/Long.parseLong(Ljava/lang/String;)J\n"+
				"    LRETURN\n"+
				" L1\n",
				toStringMethod(rtype.getLatestExecutorBytes(),"foo",false));
		// @formatter:on
		//
		// @formatter:off
		Assert.assertEquals(
				" L0\n"+
				"    ALOAD 0\n"+
				"    LDC 0\n"+
				"    LDC i\n"+
				"    INVOKESTATIC org/springsource/loaded/TypeRegistry.instanceFieldInterceptionRequired(ILjava/lang/String;)Z\n"+
				"    IFEQ L1\n"+
				"    DUP\n"+
				"    LDC i\n"+
				"    INVOKESPECIAL executor/TestOne.r$get(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;\n"+
				"    CHECKCAST java/lang/Integer\n"+
				"    INVOKEVIRTUAL java/lang/Integer.intValue()I\n"+
				"    GOTO L2\n"+
				" L1\n"+
				"    GETFIELD executor/TestOne.i I\n"+
				" L2\n"+
				"    ICONST_2\n"+
				"    IMUL\n"+
				"    IRETURN\n"+
				" L3\n",
				toStringMethod(rtype.getLatestExecutorBytes(),"hashCode",false));
		// @formatter:on
	}

	/**
	 * Testing that type level annotations are copied to the executor (to answer later reflection questions).
	 */
	@Test
	public void typeLevelAnnotations() {
		String t = "executor.A";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		reload(rtype, "2");
		Class<?> clazz = rtype.getLatestExecutorClass();
		Assert.assertEquals(Utils.getExecutorName(t, "2"), clazz.getName());
		Annotation[] annos = clazz.getAnnotations();
		Assert.assertNotNull(annos);
		Assert.assertEquals(1, annos.length);
	}

	/**
	 * Testing that type level annotations are copied to the executor. This loads a different form of the type with a second
	 * annotation.
	 */
	@Test
	public void typeLevelAnnotations2() {
		String t = "executor.A";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		Class<?> clazz = rtype.getLatestExecutorClass();
		Assert.assertEquals(Utils.getExecutorName(t, "2"), clazz.getName());
		Annotation[] annos = clazz.getAnnotations();
		Assert.assertNotNull(annos);
		Assert.assertEquals(2, annos.length);
		Set<String> s = new HashSet<String>();
		for (Annotation anno : annos) {
			s.add(anno.toString());
		}
		Assert.assertTrue(s.remove("@common.Marker()"));
		Assert.assertTrue(s.remove("@common.Anno(someValue=37, longValue=2, id=abc)"));
		Assert.assertEquals(0, s.size());
	}

	@Test
	public void methodLevelAnnotations() throws Exception {
		String t = "executor.B";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		reload(rtype, "37");
		checkAnnotations(rtype.bytesLoaded, "m()V", "@common.Marker()");
		checkAnnotations(rtype.bytesLoaded, "m2()V");
		checkAnnotations(rtype.getLatestExecutorBytes(), "m(Lexecutor/B;)V", "@common.Marker()");
		checkAnnotations(rtype.getLatestExecutorBytes(), "m2(Lexecutor/B;)V");
		rtype.loadNewVersion("39", retrieveRename("executor.B", "executor.B2"));
		checkAnnotations(rtype.getLatestExecutorBytes(), "m(Lexecutor/B;)V");
		checkAnnotations(rtype.getLatestExecutorBytes(), "m2(Lexecutor/B;)V", "@common.Marker()", "@common.Anno(id=abc)");
	}

	@Test
	public void methodLevelAnnotationsOnInterfaces() throws Exception {
		String t = "executor.I";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		reload(rtype, "37");
		checkAnnotations(rtype.bytesLoaded, "m()V", "@common.Marker()");
		checkAnnotations(rtype.bytesLoaded, "m2()V");
		checkAnnotations(rtype.getLatestExecutorBytes(), "m(Lexecutor/I;)V", "@common.Marker()");
		checkAnnotations(rtype.getLatestExecutorBytes(), "m2(Lexecutor/I;)V");
		rtype.loadNewVersion("39", retrieveRename("executor.I", "executor.I2"));
		checkAnnotations(rtype.getLatestExecutorBytes(), "m(Lexecutor/I;)V");
		checkAnnotations(rtype.getLatestExecutorBytes(), "m2(Lexecutor/I;)V", "@common.Marker()", "@common.Anno(id=abc)");
		Method m = rtype.getLatestExecutorClass().getDeclaredMethod("m2", rtype.getClazz());
		assertEquals("@common.Marker()", m.getAnnotations()[0].toString());
		assertEquals("@common.Anno(someValue=37, longValue=2, id=abc)", m.getAnnotations()[1].toString());
	}

	@Test
	public void methodLevelAnnotationsOnInterfaces2() throws Exception {
		String t = "reflection.methodannotations.InterfaceTarget";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		checkAnnotations(rtype.bytesLoaded, "privMethod()V", "@reflection.AnnoT3(value=Foo)");
		reload(rtype, "37");
		checkAnnotations(rtype.getLatestExecutorBytes(), "privMethod(Lreflection/methodannotations/InterfaceTarget;)V",
				"@reflection.AnnoT3(value=Foo)");
		rtype.loadNewVersion("39", retrieveRename(t, t + "002"));
		checkAnnotations(rtype.getLatestExecutorBytes(), "privMethod(Lreflection/methodannotations/InterfaceTarget;)V",
				"@reflection.AnnoT3(value=Bar)");
		Method m = rtype.getLatestExecutorClass().getDeclaredMethod("privMethod", rtype.getClazz());
		assertEquals("@reflection.AnnoT3(value=Bar)", m.getAnnotations()[0].toString());
	}

	@Test
	public void clashingInstanceStaticMethods() throws Exception {
		String t = "executor.C";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		reload(rtype, "37");
	}

	@Test
	public void staticInitializerReloading1() throws Exception {
		String t = "clinit.One";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("5", result.returnValue);
		rtype.loadNewVersion("39", retrieveRename(t, t + "2"));
		rtype.runStaticInitializer(); // call is made on reloadable type
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("7", result.returnValue);
	}

	@Test
	public void staticInitializerReloading2() throws Exception {
		String t = "clinit.One";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("5", result.returnValue);
		rtype.loadNewVersion("39", retrieveRename(t, t + "2"));

		// use the 'new' ___clinit___ method to drive the static initializer
		Method staticInitializer = rtype.getClazz().getMethod("___clinit___");
		assertNotNull(staticInitializer);
		staticInitializer.invoke(null);

		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("7", result.returnValue);
	}

	/**
	 * Dealing with final fields
	 */
	@Test
	public void staticInitializerReloading3() throws Exception {
		String t = "clinit.Two";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("55", result.returnValue);
		rtype.loadNewVersion("39", retrieveRename(t, t + "2"));
		rtype.runStaticInitializer();
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("99", result.returnValue);
	}

	/**
	 * Type that doesn't really have a clinit
	 */
	@Test
	public void staticInitializerReloading4() throws Exception {
		String t = "clinit.Three";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1", result.returnValue);
		rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		rtype.runStaticInitializer();
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1", result.returnValue);
		rtype.loadNewVersion("3", retrieveRename(t, t + "3"));
		rtype.runStaticInitializer();
		result = runUnguarded(rtype.getClazz(), "run");
		ClassPrinter.print(rtype.getLatestExecutorBytes());
		assertEquals("4", result.returnValue);
	}
}