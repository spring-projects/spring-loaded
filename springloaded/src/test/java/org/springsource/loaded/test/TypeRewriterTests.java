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

package org.springsource.loaded.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Test;
import org.springsource.loaded.MethodDelta;
import org.springsource.loaded.MethodInvokerRewriter;
import org.springsource.loaded.MethodMember;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeDelta;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.ri.ReflectiveInterceptor;
import org.springsource.loaded.test.infra.ClassPrinter;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;


/**
 * Tests for rewriting classes to include registry checks. When a class is loaded it must be 'fully instrumented' since
 * it cannot be changed later on. To consider just method live updating for the moment, this means: <br>
 * - methods all instrumented at load to do a registry check and possibly obtain the latest version of the dispatcher<br>
 * - method invocations all modified to involve the registry
 * 
 * The latter is necessary for the case where the method never existed on the original type. The former is sufficient if
 * you simply allow method bodies to be updated over time.
 * 
 * @author Andy Clement
 */
public class TypeRewriterTests extends SpringLoadedTests {

	/*
	 * How constructor reloading works
	 * 
	 * A constructor should be considered in 3 pieces.  There is some code that runs before the INVOKESPECIAL,
	 * the INVOKESPECIAL itself, then some code that runs after the INVOKESPECIAL. It is the INVOKESPECIAL
	 * that moves the 'this' (local variable 0) from UNINITIALIZED to INITIALIZED.  Whilst UNINITIALIZED it cannot
	 * be passed around and any attempt to do so will leave you with something like:
	 * 
	 * - cannot reference this before supertype constructor has been called
	 * 
	 * or
	 * 
	 * - expected initialized object on the stack
	 * 
	 * Because objects need to be initialized before they get passed around, we have to ensure the INVOKESPECIAL
	 * chain is run to initialized the object before we can pass it off to our dispatchers/executors.  The normal
	 * approach (used for method reloading) is to change all methods as follows:
	 * 
	 * Check if this is still the up-to-date version.  If it is then run the code as it was originally, if it was
	 * not then call the dispatcher to run the new version.
	 * 
	 * However, the object *must* be initialized before we can pass it off to the dispatcher, so the INVOKESPECIAL
	 * must be run before the dispatcher is called.  This leaves us with a slightly unusual rewrite of the
	 * constructors, in pseudocode:
	 * 
	 * A() {
	 *   if (hasThisConstructorChanged()?) {
	 *     // Use our 'special' constructors to initialized this
	 *     // call the dispatcher.
	 *     // The invokespecial is rewritten in the executor to call super.___init___ since there may be stuff
	 *     // to do in it.
	 *   } else {
	 *     run the original code
	 *   }
	 * }
	 * 
	 * This will cause us problems when the top constructor is not in a reloadable type.  THis means there is
	 * another variant that would be nice, where the constructor changes but the INVOKESPECIAL has not:
	 * 
	 * A() {
	 *   if (hasThisConstructorChanged()?) {
	 *     if ( Has the invokespecial changed?) {
	 *       // Use our 'special' constructors to initialized this
	 *       // call the dispatcher.
	 *       // The invokespecial is rewritten in the executor to call super.___init___ since there may be stuff
	 *       // to do in it.
	 *     } else {
	 *       // dispatcher - do before ctor stuff
	 *       // invokespecial
	 *       // dispatcher - do after ctor stuff
	 *     }
	 *   } else {
	 *     run the original code
	 *   }
	 *     
	 * This avoids the problem for the invokespecial chain.  it will also perform a little better.
	 * 
	 * Right now the woven code goes as follows.  Types get a new ___init___(ReloadableType) ctor in them,
	 * the parameter avoids a clash occurring and gives us the superchain to initialize uninitialized objects.
	 * Constructors exist in the dispatcher and executor as ___init___ methods, with a rewritten invokespecial
	 * that calls the ___init___ in the supertypes real instance (which then simply dispatches back through
	 * the dispatcher at its 'level').
	 * 
	 */

	/**
	 * The static initializer has had two things inserted: (1) the setting of the reloadable type (2) the static state
	 * manager
	 */
	@Test
	public void staticInitializers() throws Exception {
		String t = "test.Initializers";
		String t2 = "test.SubInitializers";
		TypeRegistry r = getTypeRegistry(t + "," + t2);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		ReloadableType subtype = r.addType(t2, loadBytesForClass(t2));

		// Check the format of the modified static initializer
		// @formatter:off
		assertEquals(
				// initialization of reloadabletype
				"    LDC 0\n"
						+
						"    LDC 0\n"
						+
						"    INVOKESTATIC org/springsource/loaded/TypeRegistry.getReloadableType(II)Lorg/springsource/loaded/ReloadableType;\n"
						+
						"    PUTSTATIC test/Initializers.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						// initialized of static static manager
						"    GETSTATIC test/Initializers.r$sfields Lorg/springsource/loaded/SSMgr;\n" +
						"    IFNONNULL L0\n" +
						"    NEW org/springsource/loaded/SSMgr\n" +
						"    DUP\n" +
						"    INVOKESPECIAL org/springsource/loaded/SSMgr.<init>()V\n" +
						"    PUTSTATIC test/Initializers.r$sfields Lorg/springsource/loaded/SSMgr;\n" +
						" L0\n" +
						// redirecting to another clinit - in cases where clinit changes before type is initialized
						"    GETSTATIC test/Initializers.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						"    INVOKEVIRTUAL org/springsource/loaded/ReloadableType.clinitchanged()I\n" +
						"    IFEQ L1\n" +
						"    GETSTATIC test/Initializers.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						"    INVOKEVIRTUAL org/springsource/loaded/ReloadableType.fetchLatest()Ljava/lang/Object;\n" +
						"    CHECKCAST test/Initializers__I\n" +
						"    INVOKEINTERFACE test/Initializers__I.___clinit___()V\n" +
						"    RETURN\n" +
						" L1\n" +
						// original code from the clinit
						"    GETSTATIC java/lang/System.out Ljava/io/PrintStream;\n" +
						"    LDC abc\n" +
						"    INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V\n" +
						" L2\n" +
						"    RETURN\n", toStringMethod(rtype.bytesLoaded, "<clinit>", false));
		// @formatter:on

		// Check that the top most reloadable type has the static state manager
		assertEquals("0x19(public static final) r$sfields Lorg/springsource/loaded/SSMgr;",
				toStringField(rtype.bytesLoaded, "r$sfields"));
		assertNull(toStringField(subtype.bytesLoaded, "r$sfields"));

		System.out.println(toStringMethod(subtype.bytesLoaded, "<clinit>", false));
		// Check the format of the modified static initializer
		// @formatter:off
		assertEquals(
				// initialization of reloadabletype
				"    LDC 0\n"
						+
						"    LDC 1\n"
						+
						"    INVOKESTATIC org/springsource/loaded/TypeRegistry.getReloadableType(II)Lorg/springsource/loaded/ReloadableType;\n"
						+
						"    PUTSTATIC test/SubInitializers.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						// initialized of static static manager
						"    GETSTATIC test/SubInitializers.r$sfields Lorg/springsource/loaded/SSMgr;\n" +
						"    IFNONNULL L0\n" +
						"    NEW org/springsource/loaded/SSMgr\n" +
						"    DUP\n" +
						"    INVOKESPECIAL org/springsource/loaded/SSMgr.<init>()V\n" +
						"    PUTSTATIC test/SubInitializers.r$sfields Lorg/springsource/loaded/SSMgr;\n" +
						" L0\n" +
						"    GETSTATIC test/SubInitializers.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						"    INVOKEVIRTUAL org/springsource/loaded/ReloadableType.clinitchanged()I\n" +
						"    IFEQ L1\n" +
						"    GETSTATIC test/SubInitializers.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						"    INVOKEVIRTUAL org/springsource/loaded/ReloadableType.fetchLatest()Ljava/lang/Object;\n" +
						"    CHECKCAST test/SubInitializers__I\n" +
						"    INVOKEINTERFACE test/SubInitializers__I.___clinit___()V\n" +
						"    RETURN\n" +
						" L1\n" +
						// original code from the clinit
						"    GETSTATIC java/lang/System.out Ljava/io/PrintStream;\n" +
						"    LDC def\n" +
						"    INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V\n" +
						" L2\n" +
						"    RETURN\n", toStringMethod(subtype.bytesLoaded, "<clinit>", false));
		// @formatter:on

		// Now look at the set/get field accessing forwarders
		// @formatter:off
		assertEquals(
				"    ALOAD 0\n"
						+
						"    GETFIELD test/Initializers.r$fields Lorg/springsource/loaded/ISMgr;\n"
						+
						"    IFNONNULL L0\n"
						+
						"    ALOAD 0\n"
						+
						"    NEW org/springsource/loaded/ISMgr\n"
						+
						"    DUP\n"
						+
						"    ALOAD 0\n"
						+
						"    GETSTATIC test/Initializers.r$type Lorg/springsource/loaded/ReloadableType;\n"
						+
						"    INVOKESPECIAL org/springsource/loaded/ISMgr.<init>(Ljava/lang/Object;Lorg/springsource/loaded/ReloadableType;)V\n"
						+
						//				"    INVOKESPECIAL org/springsource/loaded/ISMgr.<init>()V\n"+
						"    PUTFIELD test/Initializers.r$fields Lorg/springsource/loaded/ISMgr;\n"
						+
						" L0\n"
						+
						"    ALOAD 0\n"
						+
						"    GETFIELD test/Initializers.r$fields Lorg/springsource/loaded/ISMgr;\n"
						+
						"    GETSTATIC test/Initializers.r$type Lorg/springsource/loaded/ReloadableType;\n"
						+
						"    ALOAD 2\n"
						+
						"    ALOAD 1\n"
						+
						"    ALOAD 3\n"
						+
						"    INVOKEVIRTUAL org/springsource/loaded/ISMgr.setValue(Lorg/springsource/loaded/ReloadableType;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V\n"
						+
						"    RETURN\n",
				toStringMethod(rtype.bytesLoaded, "r$set", false));
		// @formatter:on

		// The subtype and supertype one do vary, due to using different reloadabletype objects
		// @formatter:off
		assertEquals(
				"    ALOAD 0\n"
						+
						"    GETFIELD test/SubInitializers.r$fields Lorg/springsource/loaded/ISMgr;\n"
						+
						"    IFNONNULL L0\n"
						+
						"    ALOAD 0\n"
						+
						"    NEW org/springsource/loaded/ISMgr\n"
						+
						"    DUP\n"
						+
						"    ALOAD 0\n"
						+
						"    GETSTATIC test/SubInitializers.r$type Lorg/springsource/loaded/ReloadableType;\n"
						+
						"    INVOKESPECIAL org/springsource/loaded/ISMgr.<init>(Ljava/lang/Object;Lorg/springsource/loaded/ReloadableType;)V\n"
						+
						//				"    INVOKESPECIAL org/springsource/loaded/ISMgr.<init>()V\n"+
						"    PUTFIELD test/SubInitializers.r$fields Lorg/springsource/loaded/ISMgr;\n"
						+
						" L0\n"
						+
						"    ALOAD 0\n"
						+
						"    GETFIELD test/SubInitializers.r$fields Lorg/springsource/loaded/ISMgr;\n"
						+
						"    GETSTATIC test/SubInitializers.r$type Lorg/springsource/loaded/ReloadableType;\n"
						+
						"    ALOAD 2\n"
						+
						"    ALOAD 1\n"
						+
						"    ALOAD 3\n"
						+
						"    INVOKEVIRTUAL org/springsource/loaded/ISMgr.setValue(Lorg/springsource/loaded/ReloadableType;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V\n"
						+
						"    RETURN\n",
				toStringMethod(subtype.bytesLoaded, "r$set", false));
		// @formatter:on
	}

	@Test
	public void staticInitializersAndInterfaces() throws Exception {
		String t = "test.Interface";
		String t2 = "test.SubInterface";
		TypeRegistry r = getTypeRegistry(t + "," + t2);
		ReloadableType itype = r.addType(t, loadBytesForClass(t));
		ReloadableType subitype = r.addType(t2, loadBytesForClass(t2));

		//		ClassPrinter.print(subitype.bytesLoaded);

		// An interface will get the reloadable type field
		assertEquals("0x19(public static final) r$type Lorg/springsource/loaded/ReloadableType;",
				toStringField(itype.bytesLoaded, "r$type"));

		// An interface will get the reloadable static state manager instance
		assertEquals("0x19(public static final) r$sfields Lorg/springsource/loaded/SSMgr;",
				toStringField(itype.bytesLoaded, "r$sfields"));

		// The static initializer will be augmented to initialize both of these
		// @formatter:off
		assertEquals(
				"    LDC 0\n"
						+
						"    LDC "
						+ itype.getId()
						+ "\n"
						+
						"    INVOKESTATIC org/springsource/loaded/TypeRegistry.getReloadableType(II)Lorg/springsource/loaded/ReloadableType;\n"
						+
						"    PUTSTATIC test/Interface.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						"    GETSTATIC test/Interface.r$sfields Lorg/springsource/loaded/SSMgr;\n" +
						"    IFNONNULL L0\n" +
						"    NEW org/springsource/loaded/SSMgr\n" +
						"    DUP\n" +
						"    INVOKESPECIAL org/springsource/loaded/SSMgr.<init>()V\n" +
						"    PUTSTATIC test/Interface.r$sfields Lorg/springsource/loaded/SSMgr;\n" +
						" L0\n" +
						"    GETSTATIC test/Interface.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						"    INVOKEVIRTUAL org/springsource/loaded/ReloadableType.clinitchanged()I\n" +
						"    IFEQ L1\n" +
						"    GETSTATIC test/Interface.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						"    INVOKEVIRTUAL org/springsource/loaded/ReloadableType.fetchLatest()Ljava/lang/Object;\n" +
						"    CHECKCAST test/Interface__I\n" +
						"    INVOKEINTERFACE test/Interface__I.___clinit___()V\n" +
						"    RETURN\n" +
						" L1\n" +
						"    RETURN\n",
				toStringMethod(itype.bytesLoaded, "<clinit>", false));
		// @formatter:on

		// Sub interface should look identical
		// An interface will get the reloadable type field
		assertEquals("0x19(public static final) r$type Lorg/springsource/loaded/ReloadableType;",
				toStringField(itype.bytesLoaded, "r$type"));

		// An interface will get the reloadable static state manager instance
		assertEquals("0x19(public static final) r$sfields Lorg/springsource/loaded/SSMgr;",
				toStringField(itype.bytesLoaded, "r$sfields"));

		// The static initializer will be augmented to initialize both of these
		// @formatter:off
		assertEquals(
				"    LDC 0\n"
						+
						"    LDC 1\n"
						+
						"    INVOKESTATIC org/springsource/loaded/TypeRegistry.getReloadableType(II)Lorg/springsource/loaded/ReloadableType;\n"
						+
						"    PUTSTATIC test/SubInterface.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						"    GETSTATIC test/SubInterface.r$sfields Lorg/springsource/loaded/SSMgr;\n" +
						"    IFNONNULL L0\n" +
						"    NEW org/springsource/loaded/SSMgr\n" +
						"    DUP\n" +
						"    INVOKESPECIAL org/springsource/loaded/SSMgr.<init>()V\n" +
						"    PUTSTATIC test/SubInterface.r$sfields Lorg/springsource/loaded/SSMgr;\n" +
						" L0\n" +
						"    GETSTATIC test/SubInterface.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						"    INVOKEVIRTUAL org/springsource/loaded/ReloadableType.clinitchanged()I\n" +
						"    IFEQ L1\n" +
						"    GETSTATIC test/SubInterface.r$type Lorg/springsource/loaded/ReloadableType;\n" +
						"    INVOKEVIRTUAL org/springsource/loaded/ReloadableType.fetchLatest()Ljava/lang/Object;\n" +
						"    CHECKCAST test/SubInterface__I\n" +
						"    INVOKEINTERFACE test/SubInterface__I.___clinit___()V\n" +
						"    RETURN\n" +
						" L1\n" +
						"    RETURN\n",
				toStringMethod(subitype.bytesLoaded, "<clinit>", false));
		// @formatter:on

		// Although the interface has fields, they are constants and so there is no code to implement them
		assertEquals("0x9(public static) i I 234", toStringField(itype.bytesLoaded, "i"));
		assertEquals("0x9(public static) j I 456", toStringField(subitype.bytesLoaded, "j"));
	}

	// Looking at a type with only a default ctor (so didn't originally declare anything)
	@Test
	public void constructorReloadingDefault() throws Exception {
		String t = "ctors.Default";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		MethodMember[] ctor = rtype.getLatestTypeDescriptor().getConstructors();
		assertEquals(1, ctor.length); // Only the ctor in the original type is in the descriptor
		assertEquals("0x1 <init>()V", ctor[0].toString());
		// There are in fact two constructors, one is our special one
		result = runConstructor(rtype.getClazz(), magicDescriptorForGeneratedCtors, new Object[] { null });
		assertNotNull(result.returnValue);
	}

	// Tests for reloading the body of an existing constructor
	@Test
	public void constructorReloading() throws Exception {
		String t = "ctors.One";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));

		result = runConstructor(rtype.getClazz(), "");
		assertEquals("Hello Andy", result.stdout);
		assertEquals(rtype.getClazz().getName(), result.returnValue.getClass().getName());

		// Just reload that same version (creates new CurrentLiveVersion)
		rtype.loadNewVersion("000", rtype.bytesInitial);

		result = runConstructor(rtype.getClazz(), "");
		assertEquals("Hello Andy", result.stdout);
		assertEquals(rtype.getClazz().getName(), result.returnValue.getClass().getName());

		// Load a real new version
		rtype.loadNewVersion("002", retrieveRename(t, t + "2"));
		result = runConstructor(rtype.getClazz(), "");
		assertEquals("Hello World", result.stdout);
		assertEquals(rtype.getClazz().getName(), result.returnValue.getClass().getName());
	}

	// similar to previous but constructor takes a parameter
	@Test
	public void constructorReloading2() throws Exception {
		String t = "ctors.Two";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));

		result = runConstructor(rtype.getClazz(), "java.lang.String", "Wibble");
		assertEquals("Wibble", result.stdout);
		assertEquals(rtype.getClazz().getName(), result.returnValue.getClass().getName());

		// Just reload that same version (creates new CurrentLiveVersion)
		rtype.loadNewVersion("000", rtype.bytesInitial);

		result = runConstructor(rtype.getClazz(), "java.lang.String", "Wobble");
		assertEquals("Wobble", result.stdout);
		assertEquals(rtype.getClazz().getName(), result.returnValue.getClass().getName());

		// Load a real new version
		rtype.loadNewVersion("002", retrieveRename(t, t + "2"));
		//		ClassPrinter.print(rtype.getLatestExecutorBytes());
		result = runConstructor(rtype.getClazz(), "java.lang.String", "Wabble");
		assertEquals("WabbleWabble", result.stdout);
		assertEquals(rtype.getClazz().getName(), result.returnValue.getClass().getName());
	}

	/**
	 * Annotation reloading, currently not allowed but hopefully doesn't impact regular code development, they just have
	 * no reloadable type representation.
	 */
	@Test
	public void annotation() throws Exception {
		String a = "annos.SimpleAnnotation";
		String b = "annos.AnnotatedType";
		TypeRegistry r = getTypeRegistry(a + "," + b);
		ReloadableType annotationType = r.addType(a, loadBytesForClass(a));
		ReloadableType annotatedType = r.addType(b, loadBytesForClass(b));

		assertNull(annotationType);
		result = runUnguarded(annotatedType.getClazz(), "printit");
		assertEquals("@annos.SimpleAnnotation(value=hello)", result.stdout);

		// reload annotated type
		annotatedType.loadNewVersion("2", retrieveRename(b, b + "2"));

		result = runUnguarded(annotatedType.getClazz(), "printit");
		assertEquals(">>@annos.SimpleAnnotation(value=hello)", result.stdout);
	}

	/**
	 * Testing reflective field access when the field flips from static to non-static.
	 */
	@Test
	public void reflectiveFieldGet() throws Exception {
		String a = "reflect.FieldAccessing";
		TypeRegistry r = getTypeRegistry(a);
		ReloadableType type = r.addType(a, loadBytesForClass(a));

		Object o = type.getClazz().newInstance();

		// Access the fields
		result = runOnInstance(type.getClazz(), o, "geti");
		assertEquals(4, ((Integer) result.returnValue).intValue());
		result = runOnInstance(type.getClazz(), o, "getj");
		assertEquals(5, ((Integer) result.returnValue).intValue());

		// Load a new version that switches them from static<>non-static
		type.loadNewVersion("2", retrieveRename(a, a + "2"));
		try {
			result = runOnInstance(type.getClazz(), o, "geti");
			fail();
		}
		catch (ResultException re) {
			Throwable cause = re.getCause();
			assertTrue(cause instanceof InvocationTargetException);
			cause = ((InvocationTargetException) cause).getCause();
			assertTrue(cause instanceof IncompatibleClassChangeError);
			assertEquals("Expected non-static field reflect/FieldAccessing.i", cause.getMessage());
		}

		try {
			result = runOnInstance(type.getClazz(), o, "getj");
			fail();
		}
		catch (ResultException re) {
			Throwable cause = re.getCause();
			assertTrue(cause instanceof InvocationTargetException);
			cause = ((InvocationTargetException) cause).getCause();
			assertTrue(cause instanceof IncompatibleClassChangeError);
			assertEquals("Expected static field reflect/FieldAccessing.j", cause.getMessage());
		}
	}

	/**
	 * Testing reflective field access when the field flips from static to non-static.
	 */
	@Test
	public void reflectiveFieldSet() throws Exception {
		String a = "reflect.FieldWriting";
		TypeRegistry r = getTypeRegistry(a);
		ReloadableType type = r.addType(a, loadBytesForClass(a));

		Object o = type.getClazz().newInstance();

		// Access the fields
		result = runOnInstance(type.getClazz(), o, "seti", 123);
		result = runOnInstance(type.getClazz(), o, "setj", 456);

		// Load a new version that switches them from static<>non-static
		type.loadNewVersion("2", retrieveRename(a, a + "2"));
		try {
			result = runOnInstance(type.getClazz(), o, "seti", 456);
			fail();
		}
		catch (ResultException re) {
			Throwable cause = re.getCause();
			assertTrue(cause instanceof InvocationTargetException);
			cause = ((InvocationTargetException) cause).getCause();
			assertTrue(cause instanceof IncompatibleClassChangeError);
			assertEquals("Expected non-static field reflect/FieldWriting.i", cause.getMessage());
		}

		try {
			result = runOnInstance(type.getClazz(), o, "setj", 789);
			fail();
		}
		catch (ResultException re) {
			Throwable cause = re.getCause();
			assertTrue(cause instanceof InvocationTargetException);
			cause = ((InvocationTargetException) cause).getCause();
			assertTrue(cause instanceof IncompatibleClassChangeError);
			assertEquals("Expected static field reflect/FieldWriting.j", cause.getMessage());
		}
	}

	// test that also relies on correct dispatch to super constructors to do things
	@Test
	public void constructorReloading3() throws Exception {
		String t = "ctors.Three";
		String st = "ctors.SuperThree";
		TypeRegistry r = getTypeRegistry(t + ",ctors.SuperThree");

		ReloadableType supertype = r.addType("ctors.SuperThree", loadBytesForClass("ctors.SuperThree"));
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));

		result = runConstructor(rtype.getClazz(), "");
		assertEquals("Hello from SuperThree.Hello from Three.", result.stderr);
		assertEquals(rtype.getClazz().getName(), result.returnValue.getClass().getName());

		// Just reload that same version (creates new CurrentLiveVersion)
		rtype.loadNewVersion("000", rtype.bytesInitial);

		result = runConstructor(rtype.getClazz(), "");
		assertEquals("Hello from SuperThree.Hello from Three.", result.stderr);
		assertEquals(rtype.getClazz().getName(), result.returnValue.getClass().getName());

		// Load a real new version
		rtype.loadNewVersion("002", retrieveRename(t, t + "2"));
		result = runConstructor(rtype.getClazz(), "");
		assertEquals("Hello from SuperThree.Hello from Three2.", result.stderr);
		assertEquals(rtype.getClazz().getName(), result.returnValue.getClass().getName());

		supertype.loadNewVersion("002", retrieveRename(st, st + "2"));
		result = runConstructor(rtype.getClazz(), "");
		assertEquals("Hello from SuperThree2.Hello from Three2.", result.stderr);
		assertEquals(rtype.getClazz().getName(), result.returnValue.getClass().getName());
	}

	// Now looking at simply changing what fields we initialize in a ctor - the simplest case really
	@Test
	public void constructorReloading4() throws Exception {
		String theType = "ctors.Setter";
		TypeRegistry r = getTypeRegistry(theType);
		ReloadableType rtype = r.addType(theType, loadBytesForClass(theType));

		result = runConstructor(rtype.getClazz(), "");
		Result res = runOnInstance(rtype.getClazz(), result.returnValue, "getInteger");
		assertEquals(1, ((Integer) res.returnValue).intValue());
		res = runOnInstance(rtype.getClazz(), result.returnValue, "getString");
		assertEquals("one", (res.returnValue));

		rtype.loadNewVersion("000", rtype.bytesInitial);
		result = runConstructor(rtype.getClazz(), "");
		res = runOnInstance(rtype.getClazz(), result.returnValue, "getInteger");
		assertEquals(1, ((Integer) res.returnValue).intValue());
		res = runOnInstance(rtype.getClazz(), result.returnValue, "getString");
		assertEquals("one", (res.returnValue));

		rtype.loadNewVersion("002", retrieveRename(theType, theType + "2"));
		result = runConstructor(rtype.getClazz(), "");
		res = runOnInstance(rtype.getClazz(), result.returnValue, "getInteger");
		assertEquals(2, ((Integer) res.returnValue).intValue());
		res = runOnInstance(rtype.getClazz(), result.returnValue, "getString");
		assertEquals("two", (res.returnValue));

		// version 3 no longer sets the string
		rtype.loadNewVersion("003", retrieveRename(theType, theType + "3"));
		result = runConstructor(rtype.getClazz(), "");
		res = runOnInstance(rtype.getClazz(), result.returnValue, "getInteger");
		assertEquals(3, ((Integer) res.returnValue).intValue());
		res = runOnInstance(rtype.getClazz(), result.returnValue, "getString");
		assertNull(res.returnValue);
	}

	@Test
	public void constructorReloading5() throws Exception {
		String supertype = "ctors.A";
		String subtype = "ctors.B";
		TypeRegistry r = getTypeRegistry(supertype + "," + subtype);
		ReloadableType rsupertype = r.addType(supertype, loadBytesForClass(supertype));
		ReloadableType rsubtype = r.addType(subtype, loadBytesForClass(subtype));
		Result res = null;

		// Use the code 'untouched'
		result = runConstructor(rsubtype.getClazz(), "int", 3);
		res = runOnInstance(rsubtype.getClazz(), result.returnValue, "getString");
		assertEquals("3", (res.returnValue));

		// reload the types 
		rsubtype.loadNewVersion("000", rsubtype.bytesInitial);
		rsupertype.loadNewVersion("000", rsupertype.bytesInitial);
		result = runConstructor(rsubtype.getClazz(), "int", 5);
		res = runOnInstance(rsubtype.getClazz(), result.returnValue, "getString");
		assertEquals("5", (res.returnValue));

		// load a new version of the subtype which adjusts the super call
		rsubtype.loadNewVersion("001", retrieveRename(subtype, subtype + "2"));
		result = runConstructor(rsubtype.getClazz(), "int", 5);
		res = runOnInstance(rsubtype.getClazz(), result.returnValue, "getString");
		assertEquals("27", (res.returnValue));
	}

	// Looking at how constructors get rewritten when the target did not originally declare the constructor
	@Test
	public void newConstructors() throws Exception {
		String caller = "ctors.Caller";
		String callee = "ctors.Callee";
		TypeRegistry r = getTypeRegistry(caller + "," + callee);
		ReloadableType rcaller = r.addType(caller, loadBytesForClass(caller));
		ReloadableType rcallee = r.addType(callee, loadBytesForClass(callee));
		Result res = null;

		// Use the code 'untouched'
		Object callerInstance = rcaller.getClazz().newInstance();
		res = runOnInstance(rcaller.getClazz(), callerInstance, "runA");
		assertEquals("callee", res.returnValue.toString());

		// Reload the code, a new constructor in the callee and runB() invokes it
		rcaller.loadNewVersion("002", retrieveRename(caller, caller + "2", "ctors.Callee2:ctors.Callee"));
		rcallee.loadNewVersion("002", retrieveRename(callee, callee + "2"));

		// The new runB() method will include a call 'new Callee("abcde")'
		// Without a rewrite, it will cause this problem:
		// Caused by: java.lang.NoSuchMethodError: ctors.Callee.<init>(Ljava/lang/String;)V
		//   at ctors.Caller__E002.runB(Caller2.java:10)
		res = runOnInstance(rcaller.getClazz(), callerInstance, "runB");
		assertEquals("callee", res.returnValue.toString());
	}

	/**
	 * Final fields. Final fields are typically inlined at their usage sites, which means in a reloadable scenario they
	 * can introduce unexpected (but correct) behaviour.
	 */
	@Test
	public void constructorsAndFinalFields() throws Exception {
		String caller = "ctors.Finals";
		TypeRegistry r = getTypeRegistry(caller);
		ReloadableType rcaller = r.addType(caller, loadBytesForClass(caller));
		Result res = null;

		// Use the code 'untouched'
		Object callerInstance = rcaller.getClazz().newInstance();
		res = runOnInstance(rcaller.getClazz(), callerInstance, "getValue");
		assertEquals("324 abc", res.returnValue.toString());

		// Reload the code
		rcaller.loadNewVersion("002", retrieveRename(caller, caller + "2"));

		// Constants are inlined - that is why getValue() returns the new value for the String
		res = runOnInstance(rcaller.getClazz(), callerInstance, "getValue");
		assertEquals("324 def", res.returnValue.toString());

		// Without changing visibility from final this would cause an IllegalAccessError from the ___init___ method.
		// That is because, if the constant hasn't changed value, there will be  PUTFIELD for an already set final
		// field in the ___init___ that gets run.  If the value is changed an entirely different codepath is used.
		callerInstance = rcaller.getClazz().newInstance();

	}

	/**
	 * Super/subtypes and in the reload the new constructor in the subtype calls a constructor in the supertype that did
	 * not initially exist. I think the rewrite of the invokespecial should be able to determine it isn't there at the
	 * start and use the all powerful _execute method added to the instance at startup (rather than the ___init___)
	 * which will then call through the dispatcher.
	 */
	@Test
	public void newConstructors2() throws Exception {
		String caller = "ctors.CallerB";
		String callee = "ctors.CalleeB";
		String calleeSuper = "ctors.CalleeSuperB";
		TypeRegistry r = getTypeRegistry(caller + "," + callee + "," + calleeSuper);
		ReloadableType rcaller = r.addType(caller, loadBytesForClass(caller));
		ReloadableType rcalleeSuper = r.addType(calleeSuper, loadBytesForClass(calleeSuper));
		ReloadableType rcallee = r.addType(callee, loadBytesForClass(callee));
		Result res = null;

		// Use the code 'untouched'
		Object callerInstance = rcaller.getClazz().newInstance();
		res = runOnInstance(rcaller.getClazz(), callerInstance, "runA");
		assertEquals("callee", res.returnValue.toString());

		// Reload the code, a new constructor in the callee and runB() invokes it
		rcalleeSuper.loadNewVersion("002", retrieveRename(calleeSuper, calleeSuper + "2"));
		rcaller.loadNewVersion("002", retrieveRename(caller, caller + "2", "ctors.CalleeB2:ctors.CalleeB"));
		rcallee.loadNewVersion("002", retrieveRename(callee, callee + "2", "ctors.CalleeSuperB2:ctors.CalleeSuperB"));

		// The new runB() method will include a call 'new Callee("abcde")'
		// Without a rewrite, it will cause this problem:
		// Caused by: java.lang.NoSuchMethodError: ctors.Callee.<init>(Ljava/lang/String;)V
		//   at ctors.Caller__E002.runB(Caller2.java:10)
		// This new Callee constructor also invokes a constructor in the supertype that wasn't there initially
		res = runOnInstance(rcaller.getClazz(), callerInstance, "runB");
		assertEquals("callee", res.returnValue.toString());
		assertContains("Super number was 32768", res.toString());
		assertContains("abcde", res.toString());
	}

	// TODO synchronized fields
	// TODO synchronization around access to the static/instance field maps
	@Test
	public void rewriteInstanceFields() throws Exception {
		// turn off to simplify debugging verify problems:
		// GlobalConfiguration.rewriteMethodExecutions = false;
		TypeRegistry r = getTypeRegistry("data.HelloWorldFields");
		ReloadableType rtype = r.addType("data.HelloWorldFields", loadBytesForClass("data.HelloWorldFields"));
		assertEquals("Hello Andy", runUnguarded(rtype.getClazz(), "greet").stdout);

		// Just reload that same version (creates new CurrentLiveVersion)
		rtype.loadNewVersion("000", rtype.bytesInitial);
		assertEquals("Hello Andy", runUnguarded(rtype.getClazz(), "greet").stdout);

		// Load a real new version
		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldFields", "data.HelloWorldFields002"));
		assertEquals("Hello Christian", runUnguarded(rtype.getClazz(), "greet").stdout);

		Object o = rtype.getClazz().newInstance();
		assertEquals("Hello Christian", runOnInstance(rtype.getClazz(), o, "greet").stdout);
		runOnInstance(rtype.getClazz(), o, "setMessage", "Hello Christian");
		assertEquals("Hello Christian", runOnInstance(rtype.getClazz(), o, "greet").stdout);
	}

	@Test
	public void primitiveFieldRewriting() throws Exception {
		TypeRegistry r = getTypeRegistry("data.FieldsB");
		ReloadableType rtype = r.addType("data.FieldsB", loadBytesForClass("data.FieldsB"));
		Class<?> c = rtype.getClazz();
		Object instance = c.newInstance();
		assertEquals(23, runOnInstance(c, instance, "getI").returnValue);
		assertEquals("{1,2,3}", intArrayToString(runOnInstance(c, instance, "getIs").returnValue));
		assertEquals(false, runOnInstance(c, instance, "isB").returnValue);
		assertEquals('a', runOnInstance(c, instance, "getC").returnValue);
		assertEquals((short) 123, runOnInstance(c, instance, "getS").returnValue);
		assertEquals(10715136L, runOnInstance(c, instance, "getL").returnValue);
		assertEquals(2.0d, runOnInstance(c, instance, "getD").returnValue);
		assertEquals(1.4f, runOnInstance(c, instance, "getF").returnValue);
		assertEquals("Hello Andy", runOnInstance(c, instance, "getTheMessage").returnValue);

		runOnInstance(c, instance, "setI", 312);
		assertEquals(312, runOnInstance(c, instance, "getI").returnValue);
		runOnInstance(c, instance, "setIs", new int[] { 4, 5 });
		assertEquals("{4,5}", intArrayToString(runOnInstance(c, instance, "getIs").returnValue));
		runOnInstance(c, instance, "setB", true);
		assertEquals(true, runOnInstance(c, instance, "isB").returnValue);
		runOnInstance(c, instance, "setC", 'z');
		assertEquals('z', runOnInstance(c, instance, "getC").returnValue);
		runOnInstance(c, instance, "setS", (short) 12);
		assertEquals((short) 12, runOnInstance(c, instance, "getS").returnValue);
		runOnInstance(c, instance, "setL", 36L);
		assertEquals(36L, runOnInstance(c, instance, "getL").returnValue);
		runOnInstance(c, instance, "setD", 111.0d);
		assertEquals(111.0d, runOnInstance(c, instance, "getD").returnValue);
		runOnInstance(c, instance, "setF", 123.0f);
		assertEquals(123.0f, runOnInstance(c, instance, "getF").returnValue);
		runOnInstance(c, instance, "setTheMessage", "Hello");
		assertEquals("Hello", runOnInstance(c, instance, "getTheMessage").returnValue);
	}

	@Test
	public void primitiveStaticFieldRewriting() throws Exception {
		TypeRegistry r = getTypeRegistry("data.StaticFieldsB");
		ReloadableType rtype = r.addType("data.StaticFieldsB", loadBytesForClass("data.StaticFieldsB"));
		Class<?> c = rtype.getClazz();
		Object instance = c.newInstance();
		assertEquals(23, runOnInstance(c, instance, "getI").returnValue);
		assertEquals(false, runOnInstance(c, instance, "isB").returnValue);
		assertEquals("{true,false,true}", objectArrayToString(runOnInstance(c, instance, "getBs").returnValue));
		assertEquals('a', runOnInstance(c, instance, "getC").returnValue);
		assertEquals((short) 123, runOnInstance(c, instance, "getS").returnValue);
		assertEquals(10715136L, runOnInstance(c, instance, "getL").returnValue);
		assertEquals(2.0d, runOnInstance(c, instance, "getD").returnValue);
		assertEquals(1.4f, runOnInstance(c, instance, "getF").returnValue);
		assertEquals("Hello Andy", runOnInstance(c, instance, "getTheMessage").returnValue);

		Object instance2 = c.newInstance();
		runOnInstance(c, instance2, "setI", 312);
		assertEquals(312, runOnInstance(c, instance2, "getI").returnValue);
		runOnInstance(c, instance2, "setB", true);
		assertEquals(true, runOnInstance(c, instance2, "isB").returnValue);
		runOnInstance(c, instance, "setBs", (Object) new Boolean[] { false, true });
		assertEquals("{false,true}", objectArrayToString(runOnInstance(c, instance, "getBs").returnValue));
		runOnInstance(c, instance2, "setC", 'z');
		assertEquals('z', runOnInstance(c, instance2, "getC").returnValue);
		runOnInstance(c, instance2, "setS", (short) 12);
		assertEquals((short) 12, runOnInstance(c, instance2, "getS").returnValue);
		runOnInstance(c, instance2, "setL", 36L);
		assertEquals(36L, runOnInstance(c, instance2, "getL").returnValue);
		runOnInstance(c, instance2, "setD", 111.0d);
		assertEquals(111.0d, runOnInstance(c, instance2, "getD").returnValue);
		runOnInstance(c, instance2, "setF", 123.0f);
		assertEquals(123.0f, runOnInstance(c, instance2, "getF").returnValue);
		runOnInstance(c, instance2, "setTheMessage", "Hello");
		assertEquals("Hello", runOnInstance(c, instance2, "getTheMessage").returnValue);
	}

	@Test
	public void rewriteStaticFields() throws Exception {
		// turn off to simplify debugging verify problems:
		// GlobalConfiguration.rewriteMethodExecutions = true;
		TypeRegistry r = getTypeRegistry("data.HelloWorldStaticFields");
		ReloadableType rtype = r.addType("data.HelloWorldStaticFields",
				loadBytesForClass("data.HelloWorldStaticFields"));
		assertEquals("Hello Andy", runUnguarded(rtype.getClazz(), "greet").stdout);

		// Just reload that same version (creates new CurrentLiveVersion)
		rtype.loadNewVersion("000", rtype.bytesInitial);
		assertEquals("Hello Andy", runUnguarded(rtype.getClazz(), "greet").stdout);

		// Load a real new version 
		// won't say 'hello christian' because field static initializers are not re-run
		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldStaticFields", "data.HelloWorldStaticFields002"));
		assertEquals("Hello Andy", runUnguarded(rtype.getClazz(), "greet").stdout);

		Object o = rtype.getClazz().newInstance();
		assertEquals("Hello Andy", runOnInstance(rtype.getClazz(), o, "greet").stdout);
		runOnInstance(rtype.getClazz(), o, "setMessage", "Hello Christian");
		assertEquals("Hello Christian", runOnInstance(rtype.getClazz(), o, "greet").stdout);
	}

	/**
	 * Just does a rewrite and checks the result will run a simple method
	 */
	@Test
	public void rewrite() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorld");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorld", loadBytesForClass("data.HelloWorld"));
		runUnguarded(rtype.getClazz(), "greet");

		// Just transform the existing version into a dispatcher/executor
		rtype.loadNewVersion("000", rtype.bytesInitial);
		assertEquals("Greet from HelloWorld", runUnguarded(rtype.getClazz(), "greet").stdout);

		// Load a real new version
		rtype.loadNewVersion("002", retrieveRename("data.HelloWorld", "data.HelloWorld002"));
		assertEquals("Greet from HelloWorld 2", runUnguarded(rtype.getClazz(), "greet").stdout);
	}

	/**
	 * Simple rewrite but the class has a static initializer (which we have to merge some code into)
	 */
	@Test
	public void rewriteWithStaticInitializer() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorldClinit");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorldClinit", loadBytesForClass("data.HelloWorldClinit"));

		runUnguarded(rtype.getClazz(), "greet");

		// Just transform the existing version into a dispatcher/executor
		rtype.loadNewVersion("000", rtype.bytesInitial);
		assertEquals("Greet from HelloWorldClinit", runUnguarded(rtype.getClazz(), "greet").stdout);

		// Load a real new version
		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldClinit", "data.HelloWorldClinit002"));
		assertEquals("Greet from HelloWorldClinit 2", runUnguarded(rtype.getClazz(), "greet").stdout);
	}

	@Test
	public void rewriteWithReturnValues() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorld");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorld", loadBytesForClass("data.HelloWorld"));

		Result r = runUnguarded(rtype.getClazz(), "getValue");
		assertEquals("message from HelloWorld", r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getValue");
		assertEquals("message from HelloWorld", r.returnValue);
	}

	@Test
	public void rewriteWithPrimitiveReturnValues_int() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorldPrimitive");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorldPrimitive",
				loadBytesForClass("data.HelloWorldPrimitive"));

		Result r = runUnguarded(rtype.getClazz(), "getValue");
		assertTrue(r.returnValue instanceof Integer);
		assertEquals(42, r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getValue");
		assertTrue(r.returnValue instanceof Integer);
		assertEquals(42, r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldPrimitive", "data.HelloWorldPrimitive002"));
		r = runUnguarded(rtype.getClazz(), "getValue");
		assertTrue(r.returnValue instanceof Integer);
		assertEquals(37, r.returnValue);
	}

	@Test
	public void rewriteWithPrimitiveReturnValues_boolean() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorldPrimitive");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorldPrimitive",
				loadBytesForClass("data.HelloWorldPrimitive"));

		Result r = runUnguarded(rtype.getClazz(), "getValueBoolean");
		assertTrue(r.returnValue instanceof Boolean);
		assertEquals(true, r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getValueBoolean");
		assertTrue(r.returnValue instanceof Boolean);
		assertEquals(true, r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldPrimitive", "data.HelloWorldPrimitive002"));
		r = runUnguarded(rtype.getClazz(), "getValueBoolean");
		assertTrue(r.returnValue instanceof Boolean);
		assertEquals(false, r.returnValue);
	}

	@Test
	public void rewriteWithPrimitiveReturnValues_short() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorldPrimitive");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorldPrimitive",
				loadBytesForClass("data.HelloWorldPrimitive"));

		Result r = runUnguarded(rtype.getClazz(), "getValueShort");
		assertTrue(r.returnValue instanceof Short);
		assertEquals((short) 3, r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getValueShort");
		assertTrue(r.returnValue instanceof Short);
		assertEquals((short) 3, r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldPrimitive", "data.HelloWorldPrimitive002"));
		r = runUnguarded(rtype.getClazz(), "getValueShort");
		assertTrue(r.returnValue instanceof Short);
		assertEquals((short) 6, r.returnValue);
	}

	@Test
	public void rewriteWithPrimitiveReturnValues_long() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorldPrimitive");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorldPrimitive",
				loadBytesForClass("data.HelloWorldPrimitive"));

		Result r = runUnguarded(rtype.getClazz(), "getValueLong");
		assertTrue(r.returnValue instanceof Long);
		assertEquals(3L, r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getValueLong");
		assertTrue(r.returnValue instanceof Long);
		assertEquals(3L, r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldPrimitive", "data.HelloWorldPrimitive002"));
		r = runUnguarded(rtype.getClazz(), "getValueLong");
		assertTrue(r.returnValue instanceof Long);
		assertEquals(6L, r.returnValue);
	}

	@Test
	public void rewriteWithPrimitiveReturnValues_double() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorldPrimitive");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorldPrimitive",
				loadBytesForClass("data.HelloWorldPrimitive"));

		Result r = runUnguarded(rtype.getClazz(), "getValueDouble");
		assertTrue(r.returnValue instanceof Double);
		assertEquals(3.0D, r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getValueDouble");
		assertTrue(r.returnValue instanceof Double);
		assertEquals(3.0D, r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldPrimitive", "data.HelloWorldPrimitive002"));
		r = runUnguarded(rtype.getClazz(), "getValueDouble");
		assertTrue(r.returnValue instanceof Double);
		assertEquals(6.0D, r.returnValue);
	}

	@Test
	public void rewriteWithPrimitiveReturnValues_char() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorldPrimitive");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorldPrimitive",
				loadBytesForClass("data.HelloWorldPrimitive"));

		Result r = runUnguarded(rtype.getClazz(), "getValueChar");
		assertTrue(r.returnValue instanceof Character);
		assertEquals('c', r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getValueChar");
		assertTrue(r.returnValue instanceof Character);
		assertEquals('c', r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldPrimitive", "data.HelloWorldPrimitive002"));
		r = runUnguarded(rtype.getClazz(), "getValueChar");
		assertTrue(r.returnValue instanceof Character);
		assertEquals('f', r.returnValue);
	}

	@Test
	public void rewriteWithPrimitiveReturnValues_byte() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorldPrimitive");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorldPrimitive",
				loadBytesForClass("data.HelloWorldPrimitive"));

		Result r = runUnguarded(rtype.getClazz(), "getValueByte");
		assertTrue(r.returnValue instanceof Byte);
		assertEquals((byte) 3, r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getValueByte");
		assertTrue(r.returnValue instanceof Byte);
		assertEquals((byte) 3, r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldPrimitive", "data.HelloWorldPrimitive002"));
		r = runUnguarded(rtype.getClazz(), "getValueByte");
		assertTrue(r.returnValue instanceof Byte);
		assertEquals((byte) 6, r.returnValue);
	}

	@Test
	public void rewriteWithPrimitiveReturnValues_intArray() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorldPrimitive");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorldPrimitive",
				loadBytesForClass("data.HelloWorldPrimitive"));

		Result r = runUnguarded(rtype.getClazz(), "getArrayInt");
		assertTrue(r.returnValue instanceof int[]);
		assertEquals(3, ((int[]) r.returnValue)[0]);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getArrayInt");
		assertTrue(r.returnValue instanceof int[]);
		assertEquals(3, ((int[]) r.returnValue)[0]);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldPrimitive", "data.HelloWorldPrimitive002"));
		r = runUnguarded(rtype.getClazz(), "getArrayInt");
		assertTrue(r.returnValue instanceof int[]);
		assertEquals(5, ((int[]) r.returnValue)[0]);
	}

	@Test
	public void rewriteWithPrimitiveReturnValues_stringArray() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorldPrimitive");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorldPrimitive",
				loadBytesForClass("data.HelloWorldPrimitive"));

		Result r = runUnguarded(rtype.getClazz(), "getArrayString");
		assertTrue(r.returnValue instanceof String[]);
		assertEquals("ABC", ((String[]) r.returnValue)[0]);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getArrayString");
		assertTrue(r.returnValue instanceof String[]);
		assertEquals("ABC", ((String[]) r.returnValue)[0]);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorldPrimitive", "data.HelloWorldPrimitive002"));
		r = runUnguarded(rtype.getClazz(), "getArrayString");
		assertTrue(r.returnValue instanceof String[]);
		assertEquals("DEF", ((String[]) r.returnValue)[0]);
	}

	@Test
	public void rewriteWithParams() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorld");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorld", loadBytesForClass("data.HelloWorld"));

		Result r = runUnguarded(rtype.getClazz(), "getValueWithParams", "aaa", "bb");
		assertTrue(r.returnValue instanceof String);
		assertEquals("message with inserts aaa and bb", r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getValueWithParams", "c", "d");
		assertTrue(r.returnValue instanceof String);
		assertEquals("message with inserts c and d", r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorld", "data.HelloWorld002"));
		r = runUnguarded(rtype.getClazz(), "getValueWithParams", "aaa", "bb");
		assertTrue(r.returnValue instanceof String);
		assertEquals("message with inserts bb and aaa", r.returnValue);
	}

	@Test
	public void rewriteStaticWithParams() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorld");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorld", loadBytesForClass("data.HelloWorld"));

		Result r = runUnguarded(rtype.getClazz(), "getStaticValueWithParams", "aaa", "bb");
		assertTrue(r.returnValue instanceof String);
		assertEquals("static message with inserts aaa and bb", r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getStaticValueWithParams", "c", "d");
		assertTrue(r.returnValue instanceof String);
		assertEquals("static message with inserts c and d", r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorld", "data.HelloWorld002"));
		r = runUnguarded(rtype.getClazz(), "getStaticValueWithParams", "aaa", "bb");
		assertTrue(r.returnValue instanceof String);
		assertEquals("static message with inserts bb and aaa", r.returnValue);
	}

	@Test
	public void rewriteStaticWithPrimitiveParams() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorld");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorld", loadBytesForClass("data.HelloWorld"));

		Result r = runUnguarded(rtype.getClazz(), "getStaticValueWithPrimitiveParams", "a", 2, 'c');
		assertTrue(r.returnValue instanceof String);
		assertEquals("message with inserts a and 2 and c", r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getStaticValueWithPrimitiveParams", "a", 2, 'c');
		assertTrue(r.returnValue instanceof String);
		assertEquals("message with inserts a and 2 and c", r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorld", "data.HelloWorld002"));
		r = runUnguarded(rtype.getClazz(), "getStaticValueWithPrimitiveParams", "a", 2, 'c');
		assertTrue(r.returnValue instanceof String);
		assertEquals("message with inserts c and 2 and a", r.returnValue);
	}

	@Test
	public void rewriteStaticWithPrimitiveDoubleSlottersParams() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorld");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorld", loadBytesForClass("data.HelloWorld"));

		Result r = runUnguarded(rtype.getClazz(), "getStaticValueWithPrimitiveDSParams", 3L, "a", 2.0d, true);
		assertTrue(r.returnValue instanceof String);
		assertEquals("message with inserts 3 and a and 2.0 and true", r.returnValue);

		rtype.loadNewVersion("000", rtype.bytesInitial);
		r = runUnguarded(rtype.getClazz(), "getStaticValueWithPrimitiveDSParams", 3L, "a", 2.0d, true);
		assertTrue(r.returnValue instanceof String);
		assertEquals("message with inserts 3 and a and 2.0 and true", r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("data.HelloWorld", "data.HelloWorld002"));
		r = runUnguarded(rtype.getClazz(), "getStaticValueWithPrimitiveDSParams", 3L, "a", 2.0d, true);
		assertTrue(r.returnValue instanceof String);
		assertEquals("message with inserts true and 2.0 and a and 3", r.returnValue);
	}

	// Checking parameter loading is using correct offsets
	@Test
	public void catchers1() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("catchers..*");
		loadType(typeRegistry, "catchers.AbstractClass");
		ReloadableType rtype = loadType(typeRegistry, "catchers.SimpleCatcher");
		// Checking for a verify error:
		rtype.getClazz().newInstance();
		runUnguarded(rtype.getClazz(), "setTelephone", "abc");
	}

	/**
	 * Testing what happens if we 'reload' a type where a method that was previously final has been made non-final and
	 * has been overridden in a subtype. This works even though the 'final' isn't removed on type rewriting because the
	 * new method doesn't ever really exist in the subtype!
	 */
	@Test
	public void overridingFinalMethods() throws Exception {
		String t = "basic.Top";
		String b = "basic.Bottom";
		TypeRegistry typeRegistry = getTypeRegistry(t + "," + b);
		ReloadableType ttype = loadType(typeRegistry, t);
		ReloadableType btype = loadType(typeRegistry, b);

		runUnguarded(btype.getClazz(), "run");

		ttype.loadNewVersion("2", retrieveRename(t, t + "2"));
		btype.loadNewVersion("2", retrieveRename(b, b + "2", "basic.Top2:basic.Top"));
		result = runUnguarded(btype.getClazz(), "run");
		assertEquals("abc", result.stdout);
	}

	// checking catchers when final methods are being overridden
	// What we are checking:  in a small type hierarchy an intermediate type provides a final version of hashCode.  There should be no catcher
	// in the lower types.
	@Test
	public void catchers2() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("catchers..*");
		// A verify error will occur if Finality.hashCode() is generated (as a catcher for the one in the supertype)
		loadType(typeRegistry, "catchers.Super");
		ReloadableType rtype = loadType(typeRegistry, "catchers.Finality");
		rtype.getClazz().newInstance();
		result = runUnguarded(rtype.getClazz(), "hashCode");
		assertEquals(12, result.returnValue);
	}

	@Test
	public void methodDeletion() throws Exception {
		TypeRegistry tr = getTypeRegistry("methoddeletion.TypeA");
		ReloadableType rt = loadType(tr, "methoddeletion.TypeA");
		result = runUnguarded(rt.getClazz(), "forDeletion");
		boolean b = rt.loadNewVersion("2", retrieveRename("methoddeletion.TypeA", "methoddeletion.TypeA2"));
		assertTrue(b);
		try {
			result = runUnguarded(rt.getClazz(), "forDeletion");
			fail();
		}
		catch (InvocationTargetException ite) {
			assertEquals("java.lang.NoSuchMethodError", ite.getCause().getClass().getName());
			assertEquals("methoddeletion.TypeA.forDeletion()V", ite.getCause().getMessage());
		}
	}

	@Test
	public void fieldAccessorsAndUsageFromExecutors() throws Exception {
		TypeRegistry tr = getTypeRegistry("accessors.ProtectedFields");
		ReloadableType rt = loadType(tr, "accessors.ProtectedFields");
		result = runUnguarded(rt.getClazz(), "run");
		assertEquals("success", result.returnValue);
		// reload itself, which means the executors will now be trying to access those fields
		rt.loadNewVersion("2", rt.bytesInitial);
		result = runUnguarded(rt.getClazz(), "run");
		assertEquals("success", result.returnValue);
	}

	// Checking that field accessors are created for private fields that can be used from the executors
	//	@Test
	//	public void fieldAccessors() throws Exception {
	//		registry = getTypeRegistry("accessors.PrivateFields");
	//		ReloadableType rtype = loadType(registry, "accessors.PrivateFields");
	//
	//		Class<?> clazz = rtype.getClazz();
	//		Object instance = clazz.newInstance();
	//
	//		// Fields to test are 'int i' 'String someString' 'long lng' and static 'boolean b' and 'String someStaticString'
	//		Method accessor = clazz.getMethod(Utils.getFieldAccessorName("i", "accessors/PrivateFields"));
	//		assertEquals(Integer.TYPE, accessor.getReturnType());
	//		assertTrue(Modifier.isPublic(accessor.getModifiers()));
	//		assertTrue(accessor.isSynthetic());
	//		assertEquals(23, ((Integer) accessor.invoke(instance)).intValue());
	//
	//		accessor = clazz.getMethod(Utils.getFieldAccessorName("lng", "accessors/PrivateFields"));
	//		assertEquals(Long.TYPE, accessor.getReturnType());
	//		assertTrue(Modifier.isPublic(accessor.getModifiers()));
	//		assertTrue(accessor.isSynthetic());
	//		assertEquals(32768L, ((Long) accessor.invoke(instance)).longValue());
	//
	//		accessor = clazz.getMethod(Utils.getFieldAccessorName("someString", "accessors/PrivateFields"));
	//		assertEquals(String.class, accessor.getReturnType());
	//		assertTrue(Modifier.isPublic(accessor.getModifiers()));
	//		assertFalse(Modifier.isStatic(accessor.getModifiers()));
	//		assertTrue(accessor.isSynthetic());
	//		assertEquals("abc", accessor.invoke(instance));
	//
	//		accessor = clazz.getMethod(Utils.getFieldAccessorName("b", "accessors/PrivateFields"));
	//		assertEquals(Boolean.TYPE, accessor.getReturnType());
	//		assertTrue(Modifier.isPublic(accessor.getModifiers()));
	//		assertTrue(Modifier.isStatic(accessor.getModifiers()));
	//		assertTrue(accessor.isSynthetic());
	//		assertEquals(false, accessor.invoke(instance));
	//
	//		accessor = clazz.getMethod(Utils.getFieldAccessorName("someStaticString", "accessors/PrivateFields"));
	//		assertEquals(String.class, accessor.getReturnType());
	//		assertTrue(Modifier.isPublic(accessor.getModifiers()));
	//		assertTrue(Modifier.isStatic(accessor.getModifiers()));
	//		assertTrue(accessor.isSynthetic());
	//		assertEquals("def", accessor.invoke(instance));
	//	}

	/**
	 * Basically testing
	 * 
	 * <pre>
	 * 
	 * class A {
	 * 
	 * 	public void foo() {
	 * 	}
	 * }
	 * 
	 * class B extends A {
	 * 
	 * 	public void foo() {
	 * 	}
	 * }
	 * 
	 * class C {
	 * 
	 * 	public void m() {
	 * 		A a = new B();
	 * 		a.foo();
	 * 	}
	 * }
	 * </pre>
	 * 
	 * then reload A and remove foo. What happens?
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMethodDeletion() throws Exception {
		registry = getTypeRegistry("inheritance.A,inheritance.B");
		ReloadableType a = loadType(registry, "inheritance.A");
		//		ReloadableType b =
		loadType(registry, "inheritance.B");
		byte[] callerBytes = loadBytesForClass("inheritance.C");
		callerBytes = MethodInvokerRewriter.rewrite(registry, callerBytes);
		Class<?> callerClazz = loadit("inheritance.C", callerBytes);
		Method m = callerClazz.getMethod("run");
		assertEquals(66, ((Integer) m.invoke(null)).intValue());

		// reload A with the method foo removed
		a.loadNewVersion("002", retrieveRename("inheritance.A", "inheritance.A002"));

		// Now expect: java.lang.NoSuchMethodError: inheritance.A.foo()I
		try {
			m.invoke(null);
			fail();
		}
		catch (InvocationTargetException ite) {
			assertEquals("java.lang.NoSuchMethodError", ite.getCause().getClass().getName());
			assertEquals("A.foo()I", ite.getCause().getMessage());
		}
	}

	/**
	 * This test is the first to allow catchers in abstract classes for non-implemented interface methods.
	 */
	@Test
	public void testAbstractClass() throws Exception {
		String intface = "abs.Intface";
		String absimpl = "abs.AbsImpl";
		String impl = "abs.Impl";
		registry = getTypeRegistry(intface + "," + absimpl + "," + impl);
		//		ReloadableType rIntface = 
		loadType(registry, intface);
		ReloadableType rAbsimpl = loadType(registry, absimpl);
		ReloadableType rImpl = loadType(registry, impl);
		result = runUnguarded(rImpl.getClazz(), "run");
		assertEquals("1", result.stdout);

		// Now load a new version of the abstract type with method() in it, and
		// a new version of the concrete type without method() in it
		rAbsimpl.loadNewVersion("2", retrieveRename(absimpl, absimpl + "2"));
		rImpl.loadNewVersion("2", retrieveRename(impl, impl + "2", absimpl + "2:" + absimpl));
		//		ClassPrinter.print(rAbsimpl.bytesLoaded);
		result = runUnguarded(rImpl.getClazz(), "run");
		assertEquals("2", result.stdout);
	}

	//TODO similar to above case but where INVOKEINTERFACE is being used to call the method

	/**
	 * Checking that rtype field is created and initialized for a reloadable interface type.
	 */
	@Test
	public void testInterfaceRTypeField() {
		String interfaceName = "reflection.targets.InterfaceTarget";
		registry = getTypeRegistry(interfaceName);
		ReloadableType rtype = registry.addType(interfaceName, loadBytesForClass(interfaceName));
		assertEquals(rtype, ReflectiveInterceptor.getRType(rtype.getClazz()));
	}

	@Test
	public void staticMethodAddedlater() throws Exception {
		String t = "invokestatic.A";
		registry = getTypeRegistry(t);
		ReloadableType type = loadType(registry, t);
		result = runUnguarded(type.getClazz(), "run");
		assertEquals("hello", result.returnValue);
		type.loadNewVersion("2", retrieveRename(t, t + "2"));
		//		ClassPrinter.print(type.getLatestExecutorBytes());
		result = runUnguarded(type.getClazz(), "run");
		assertEquals("world", result.returnValue);
	}

	@Test
	public void privateStaticMethod() throws Exception {
		String t = "invokestatic.B";
		registry = getTypeRegistry(t);
		ReloadableType type = loadType(registry, t);
		result = runUnguarded(type.getClazz(), "run");
		assertEquals("hello", result.returnValue);
		type.loadNewVersion("2", retrieveRename(t, t + "2"));
		result = runUnguarded(type.getClazz(), "run");
		assertEquals("goodbye", result.returnValue);
	}

	/**
	 * Testing the type delta which records what has changed on a reload. Here we are reloading a type twice. In the
	 * first reload nothing has changed. In the second reload the code in the constructor has changed, but the
	 * invokespecial call to super has not changed.
	 */
	@Test
	public void constructorChangingButNotSuper() throws Exception {
		String t = "ctors.V";
		registry = getTypeRegistry(t);
		ReloadableType type = loadType(registry, t);
		result = runConstructor(type.getClazz(), "");
		assertEquals("Hello", result.stdout);
		type.loadNewVersion("2", retrieveRename(t, t + "2"));
		result = runConstructor(type.getClazz(), "");
		assertEquals("Hello", result.stdout);

		// should be no changes
		TypeDelta td = type.getLiveVersion().getTypeDelta();
		assertNotNull(td);
		assertFalse(td.hasAnythingChanged());
		assertNull(td.getChangedMethods());

		// this version changes the constructor
		type.loadNewVersion("3", retrieveRename(t, t + "3"));
		result = runConstructor(type.getClazz(), "");
		assertEquals("Goodbye", result.stdout);
		td = type.getLiveVersion().getTypeDelta();
		assertNotNull(td);
		assertTrue(td.hasAnythingChanged());
		Map<String, MethodDelta> changedMethods = td.getChangedMethods();
		assertNotNull(changedMethods);
		assertEquals(1, changedMethods.size());
		assertEquals("MethodDelta[method:<init>()V]", changedMethods.get("<init>()V").toString());
		MethodDelta md = changedMethods.get("<init>()V");
		assertTrue(md.hasAnyChanges());
		assertFalse(md.hasInvokeSpecialChanged());
		assertTrue(md.hasCodeChanged());
	}

	/**
	 * Testing that even though a type is reloaded, if the constructor body hasn't changed then we still run the
	 * original version of it.
	 */
	@Test
	public void originalConstructorRunOnReload() throws Exception {
		String t = "ctors.XX";
		registry = getTypeRegistry(t);
		ReloadableType type = loadType(registry, t);

		result = runConstructor(type.getClazz(), "");
		// should be running inside the constructor directly
		assertEquals("ctors.XX.<init>(XX.java:6)", result.stdout);

		// Reload, no code changes
		type.loadNewVersion("2", type.bytesInitial);
		result = runConstructor(type.getClazz(), "");
		// Now, if running reloaded code, stack frame will be: ctors.XX__E2.___init___(XX.java:6)
		// but we want it to be the same as before
		assertEquals("ctors.XX.<init>(XX.java:6)", result.stdout);

		// Reload, change the constructor this time
		type.loadNewVersion("3", retrieveRename(t, t + "2"));
		result = runConstructor(type.getClazz(), "");
		assertEquals("ctors.XX$$E3.___init___(XX2.java:7)", result.stdout);
	}

	//  TODO need some tests for static methods in a hierarchy, do we dispatch correctly, can you have a private static in between a pair of non-statics in a super and subtype?

}
