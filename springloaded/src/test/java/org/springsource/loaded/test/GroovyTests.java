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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.test.infra.ClassPrinter;
import org.springsource.loaded.test.infra.TestClassloaderWithRewriting;


public class GroovyTests extends SpringLoadedTests {

	@Before
	public void setUp() throws Exception {
		switchToGroovy();
	}

	// Accessing a field from another type, which just turns into property
	// access via a method
	@Test
	public void fields() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String a = "simple.Front";
		String b = "simple.Back";
		TypeRegistry r = getTypeRegistry(a + "," + b);
		ReloadableType rtypea = r.addType(a, loadBytesForClass(a));
		ReloadableType rtypeb = r.addType(b, loadBytesForClass(b));
		result = runUnguarded(rtypea.getClazz(), "run");
		assertEquals(35, result.returnValue);

		try {
			result = runUnguarded(rtypea.getClazz(), "run2");
			fail();
		}
		catch (InvocationTargetException ite) {
			// success - var2 doesn't exist yet
		}

		rtypeb.loadNewVersion("2", retrieveRename(b, b + "2"));

		result = runUnguarded(rtypea.getClazz(), "run2");
		assertEquals(3355, result.returnValue);
	}

	// test is too sensitive to changes between groovy compiler versions
	@Ignore
	@Test
	public void reflection() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String a = "simple.SelfReflector";
		TypeRegistry r = getTypeRegistry(a);
		ReloadableType rtypea = r.addType(a, loadBytesForClass(a));
		result = runUnguarded(rtypea.getClazz(), "run");

		assertEquals(
				"14 $callSiteArray $class$java$lang$String $class$java$lang$StringBuilder $class$java$lang$reflect$Field $class$java$util$ArrayList $class$java$util$Collections $class$java$util$Iterator $class$java$util$List $class$simple$SelfReflector $staticClassInfo __$stMC array$$class$java$lang$reflect$Field i metaClass",
				result.returnValue);
	}

	// TODO why doesn't this need swapInit? Is that only required for static
	// field constants?
	@Test
	public void localVariables() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String a = "simple.LFront";
		TypeRegistry r = getTypeRegistry(a);
		ReloadableType rtypea = r.addType(a, loadBytesForClass(a));
		result = runUnguarded(rtypea.getClazz(), "run");
		assertEquals("abc", result.returnValue);
		result = runUnguarded(rtypea.getClazz(), "run2");
		assertEquals(99, result.returnValue);

		rtypea.loadNewVersion("2", retrieveRename(a, a + "2"));

		result = runUnguarded(rtypea.getClazz(), "run");
		assertEquals("xxx", result.returnValue);
		result = runUnguarded(rtypea.getClazz(), "run2");
		assertEquals(88, result.returnValue);
	}

	@Test
	public void fieldsOnInstance() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String a = "simple.Front";
		String b = "simple.Back";
		TypeRegistry r = getTypeRegistry(a + "," + b);
		ReloadableType rtypea = r.addType(a, loadBytesForClass(a));
		ReloadableType rtypeb = r.addType(b, loadBytesForClass(b));
		Object instance = rtypea.getClazz().newInstance();
		result = runOnInstance(rtypea.getClazz(), instance, "run");
		assertEquals(35, result.returnValue);
		try {
			result = runOnInstance(rtypea.getClazz(), instance, "run2");
			fail();
		}
		catch (Exception e) {
			// success - var2 doesn't exist yet
		}
		// rtypea.fixupGroovyType();
		rtypeb.loadNewVersion("2", retrieveRename(b, b + "2"));

		result = runOnInstance(rtypea.getClazz(), instance, "run2");
		// The field will not be initialized, so will contain 0
		assertEquals(0, result.returnValue);
	}

	// Changing the return value within a method
	@Test
	public void basic() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String t = "simple.Basic";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("hello", result.returnValue);
		rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("goodbye", result.returnValue);
	}

	@Test
	public void basicInstance() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String t = "simple.Basic";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		Object instance = null;
		instance = rtype.getClazz().newInstance();

		// First method call to 'run' should return "hello"
		result = runOnInstance(rtype.getClazz(), instance, "run");
		assertEquals("hello", result.returnValue);

		// Version 3 makes run() call another local method to get the string
		// "abc"
		rtype.loadNewVersion("3", retrieveRename(t, t + "3"));

		// Field f = rtype.getClazz().getDeclaredField("metaClass");
		// f.setAccessible(true);
		// Object mc = f.get(instance);
		// System.out.println("Metaclass is currently " + mc);
		//
		// f.set(instance, null);
		//
		// f =
		// rtype.getClazz().getDeclaredField("$class$groovy$lang$MetaClass");
		// f.setAccessible(true);
		// f.set(instance, null);
		//
		// Method m = rtype.getClazz().getDeclaredMethod("getMetaClass");
		// m.setAccessible(true);
		// m.invoke(instance);
		// f.setAccessible(true);
		// mc = f.get(instance);

		// 9: invokevirtual #23; //Method
		// $getStaticMetaClass:()Lgroovy/lang/MetaClass;
		// 12: dup
		// 13: invokestatic #27; //Method
		// $get$$class$groovy$lang$MetaClass:()Ljava/lang/Class;
		// 16: invokestatic #33; //Method
		// org/codehaus/groovy/runtime/ScriptBytecodeAdapter.castToType:(Ljava/lang/Object;Ljav
		// a/lang/Class;)Ljava/lang/Object;
		// 19: checkcast #35; //class groovy/lang/MetaClass
		// 22: aload_0
		// 23: swap
		// 24: putfield #37; //Field metaClass:Lgroovy/lang/MetaClass;
		// 27: pop
		// 28: return

		// Method m = rtype.getClazz().getDeclaredMethod("$getStaticMetaClass");
		// m.setAccessible(true);
		// Object o = m.invoke(instance);
		// m =
		// rtype.getClazz().getDeclaredMethod("$get$$class$groovy$lang$MetaClass");
		// m.setAccessible(true);
		// Object p = m.invoke(null);
		// m =
		// rtype.getClazz().getClassLoader().loadClass("org.codehaus.groovy.runtime.ScriptBytecodeAdapter")
		// .getDeclaredMethod("castToType", Object.class, Class.class);
		// m.setAccessible(true);
		//
		// Object mc = m.invoke(null, o, p);
		// Field f = rtype.getClazz().getDeclaredField("metaClass");
		// f.setAccessible(true);
		// f.set(instance, null);

		// instance = rtype.getClazz().newInstance();

		// System.out.println("Metaclass is currently " + mc);
		// Let's reinitialize the instance meta class by duplicating

		result = runOnInstance(rtype.getClazz(), instance, "run");
		// result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("abc", result.returnValue);
	}

	// The method calls another method to get the return string, test
	// that when the method we are calling changes, we do call the new
	// one (simply checking the callsite cache is reset)
	@Test
	public void basic2() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String t = "simple.BasicB";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("hello", result.returnValue);
		rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("goodbye", result.returnValue);
	}

	// Similar to BasicB but now using non-static methods
	@Test
	public void basic3() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String t = "simple.BasicC";
		TypeRegistry r = getTypeRegistry(t);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("hello", result.returnValue);
		rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("goodbye", result.returnValue);
	}

	// Now calling between two different types to check if we have
	// to clear more than 'our' state on a reload. In this scenario
	// the method being called is static
	@Test
	public void basic4() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String t = "simple.BasicD";
		String target = "simple.BasicDTarget";
		TypeRegistry r = getTypeRegistry(t + "," + target);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		ReloadableType rtypeTarget = r.addType(target, loadBytesForClass(target));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("hello", result.returnValue);
		rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		rtypeTarget.loadNewVersion("2", retrieveRename(target, target + "2"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("abc", result.returnValue);
	}

	// Calling from one type to another, now the methods are non-static
	@Test
	public void basic5() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String t = "simple.BasicE";
		String target = "simple.BasicETarget";

		TypeRegistry r = getTypeRegistry(t + "," + target);

		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		ReloadableType rtypeTarget = r.addType(target, loadBytesForClass(target));

		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("hello", result.returnValue);

		rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		rtypeTarget.loadNewVersion("2", retrieveRename(target, target + "2"));

		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("foobar", result.returnValue);
	}

	// Now call a method on the target, then load a version where it is gone,
	// what happens?
	// I'm looking to determine what in the caller needs clearing out based on
	// what it has
	// cached about the target
	@Test
	public void basic6() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String t = "simple.BasicF";
		String target = "simple.BasicFTarget";
		// GlobalConfiguration.logging = true;
		// GlobalConfiguration.isRuntimeLogging = true;

		TypeRegistry r = getTypeRegistry(t + "," + target);

		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		ReloadableType rtypeTarget = r.addType(target, loadBytesForClass(target));

		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("123", result.returnValue);

		// rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		rtypeTarget.loadNewVersion("2", retrieveRename(target, target + "2"));

		result = null;

		// The target method has been removed, should now fail to call it
		try {
			runUnguarded(rtype.getClazz(), "run");
			fail();
		}
		catch (InvocationTargetException ite) {
			// success
		}

		// Load the original back in, should work again
		rtypeTarget.loadNewVersion("3", retrieveRename(target, target));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("123", result.returnValue);

		// rtype.loadNewVersion("2", rtype.bytesInitial); //reload yourself

		// Load a new version that now returns an int
		rtypeTarget.loadNewVersion("4", retrieveRename(target, target + "4"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("4456", result.returnValue);

	}

	@Ignore
	// test has intermittent problems
	// checking call site caching in the caller - do we need to clear it out?
	// Are we just not having to because isCompilable is off?
	@Test
	public void methodCallTargetComingAndGoing() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String t = "simple.BasicG";
		String target = "simple.BasicGTarget";
		TypeRegistry r = getTypeRegistry(t + "," + target);
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		ReloadableType rtypeTarget = r.addType(target, loadBytesForClass(target));

		// At this point BasicG.run() is calling a method that does not yet
		// exist on BasicGTarget
		try {
			result = runUnguarded(rtype.getClazz(), "run");
			fail();
		}
		catch (InvocationTargetException ite) {
			ite.printStackTrace();
			assertCause(ite, "MissingMethodException");
		}

		// Now a version of BasicGTarget is loaded that does define the method
		rtypeTarget.loadNewVersion("2", retrieveRename(target, target + "2"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("hw", result.returnValue);

		// Now load the original version so the method is gone again
		rtypeTarget.loadNewVersion("3", rtypeTarget.bytesInitial);// retrieveRename(target,
																	// target +
																	// "2"));
		try {
			result = runUnguarded(rtype.getClazz(), "run");
			fail();
		}
		catch (InvocationTargetException ite) {
			ite.printStackTrace();
			assertCause(ite, "MissingMethodException");
		}
		// Here is the stack when it fails with a NSME:
		// java.lang.reflect.InvocationTargetException
		// at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
		// at
		// sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
		// at
		// sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
		// at java.lang.reflect.Method.invoke(Method.java:597)
		// at
		// org.springsource.loaded.test.SpringLoadedTests.runUnguarded(SpringLoadedTests.java:201)
		// at
		// org.springsource.loaded.test.GroovyTests.methodCallTargetComingAndGoing(GroovyTests.java:340)
		// at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
		// at
		// sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
		// at
		// sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
		// at java.lang.reflect.Method.invoke(Method.java:597)
		// at
		// org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:44)
		// at
		// org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:15)
		// at
		// org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:41)
		// at
		// org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:20)
		// at
		// org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:28)
		// at
		// org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:31)
		// at
		// org.junit.runners.BlockJUnit4ClassRunner.runNotIgnored(BlockJUnit4ClassRunner.java:79)
		// at
		// org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:71)
		// at
		// org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:49)
		// at org.junit.runners.ParentRunner$3.run(ParentRunner.java:193)
		// at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:52)
		// at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:191)
		// at org.junit.runners.ParentRunner.access$000(ParentRunner.java:42)
		// at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:184)
		// at org.junit.runners.ParentRunner.run(ParentRunner.java:236)
		// at
		// org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:50)
		// at
		// org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:38)
		// at
		// org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:467)
		// at
		// org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:683)
		// at
		// org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:390)
		// at
		// org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:197)
		// Caused by: java.lang.NoSuchMethodError:
		// BasicGTarget.foo()Ljava/lang/String;
		// at
		// org.springsource.loaded.TypeRegistry.istcheck(TypeRegistry.java:1090)
		// at simple.BasicGTarget$foo.call(Unknown Source)
		// at simple.BasicG.run(BasicG.groovy:6)
		// ... 31 more
	}

	private void assertCause(Exception e, String string) {
		String s = e.getCause().toString();
		if (!s.contains(string)) {
			fail("Did not find string '" + string + "' in exception text:\n" + s);
		}
	}

	// Needs groovy 1.7.8
	@Test
	public void simpleValues() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String t = "simple.Values";
		String target = "simple.BasicFTarget";
		// GlobalConfiguration.logging = true;
		// GlobalConfiguration.isRuntimeLogging = true;

		TypeRegistry r = getTypeRegistry(t + "," + target);

		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		// ReloadableType rtypeTarget = r.addType(target,
		// loadBytesForClass(target));

		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals(new Integer(123), result.returnValue);

		rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		// rtypeTarget.loadNewVersion("2", retrieveRename(target, target +
		// "2"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals(new Integer(456), result.returnValue);
		//
		// result = null;
		//
		// // The target method has been removed, should now fail to call it
		// try {
		// runUnguarded(rtype.getClazz(), "run");
		// fail();
		// } catch (InvocationTargetException ite) {
		// // success
		// }
		//
		// // Load the original back in, should work again
		// rtypeTarget.loadNewVersion("3", retrieveRename(target, target));
		// result = runUnguarded(rtype.getClazz(), "run");
		// assertEquals("123", result.returnValue);
		//
		// // rtype.loadNewVersion("2", rtype.bytesInitial); //reload yourself
		//
		// // Load a new version that now returns an int
		// rtypeTarget.loadNewVersion("4", retrieveRename(target, target +
		// "4"));
		// result = runUnguarded(rtype.getClazz(), "run");
		// assertEquals("4456", result.returnValue);

	}

	@Ignore
	// something has changed in closure structure and the tests need debugging to get to the bottom of it
	/**
	 * Reloading code that is using a closure within a method body - no real changes, just checking it hangs together!
	 */
	@Test
	public void closure1() throws Exception {
		String t = "simple.BasicWithClosure";
		String c = "simple.BasicWithClosure$_run_closure1";
		TypeRegistry r = getTypeRegistry(t + "," + c);
		ReloadableType ctype = r.addType(c, loadBytesForClass(c));

		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("Executing:hello!", result.stdout);

		rtype.loadNewVersion(
				"2",
				retrieveRename(t, t + "2",
						"simple.BasicWithClosure2$_run_closure1:simple.BasicWithClosure$_run_closure1"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("Executing:hello!", result.stdout);

		// Change what the closure does and reload it
		ctype.loadNewVersion("3", retrieveRename(c, "simple.BasicWithClosure3$_run_closure1"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("Executing:goodbye!", result.stdout);

		// reload an unchanged version - should behave as before
		rtype.loadNewVersion(
				"3",
				retrieveRename(t, t + "3",
						"simple.BasicWithClosure3$_run_closure1:simple.BasicWithClosure$_run_closure1"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("Executing:goodbye!", result.stdout);
	}

	@Ignore
	// something has changed in closure structure and the tests need debugging to get to the bottom of it
	/**
	 * Now closure is initialized as a field (so in ctor) rather than inside a method
	 */
	@Test
	public void closure2() throws Exception {
		String t = "simple.BasicWithClosureB";
		String c = "simple.BasicWithClosureB$_closure1";
		TypeRegistry r = getTypeRegistry(t + "," + c);
		ReloadableType ctype = r.addType(c, loadBytesForClass(c));
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("Executing:hello!", result.stdout);

		rtype.loadNewVersion("2",
				retrieveRename(t, t + "2", "simple.BasicWithClosureB2$_closure1:simple.BasicWithClosureB$_closure1"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("Executing:hello!", result.stdout);

		// code in closure changes
		ctype.loadNewVersion("3", retrieveRename(c, "simple.BasicWithClosureB3$_closure1"));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("Executing:goodbye!", result.stdout);
	}

	@Ignore
	// something has changed in closure structure and the tests need debugging to get to the bottom of it
	/**
	 * Double nested closure - a method that is invoked on the owners owner.
	 */
	@Test
	public void closure3() throws Exception {
		String t = "simple.BasicWithClosureC";
		String c = "simple.BasicWithClosureC$_run_closure1";
		String c2 = "simple.BasicWithClosureC$_run_closure1_closure2";
		TypeRegistry r = getTypeRegistry(t + "," + c + "," + c2);
		ReloadableType ctype = r.addType(c, loadBytesForClass(c));
		ReloadableType c2type = r.addType(c2, loadBytesForClass(c2));
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("foo() running\nin closure", result.stdout);

		rtype.loadNewVersion(
				"2",
				retrieveRename(t, t + "2",
						"simple.BasicWithClosureC2$_run_closure1:simple.BasicWithClosureC$_run_closure1",
						"simple.BasicWithClosureC2:simple.BasicWithClosureC"));

		c2type.loadNewVersion(
				"2",
				retrieveRename(c2, "simple.BasicWithClosureC2$_run_closure1_closure2",
						"simple.BasicWithClosureC2$_run_closure1:simple.BasicWithClosureC$_run_closure1",
						"simple.BasicWithClosureC2:simple.BasicWithClosureC"));

		ctype.loadNewVersion(
				"2",
				retrieveRename(
						c,
						"simple.BasicWithClosureC2$_run_closure1",
						"simple.BasicWithClosureC2$_run_closure1_closure2:simple.BasicWithClosureC$_run_closure1_closure2",
						"simple.BasicWithClosureC2:simple.BasicWithClosureC"));

		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("in closure\nfoo() running", result.stdout);
		//
		// // code in closure changes
		// ctype.loadNewVersion("3", retrieveRename(c,
		// "simple.BasicWithClosureB3$_closure1"));
		// result = runUnguarded(rtype.getClazz(), "run");
		// assertEquals("Executing:goodbye!", result.stdout);
	}

	@Ignore
	// something has changed in closure structure and the tests need debugging to get to the bottom of it
	/**
	 * Closures with references.
	 * 
	 * This is the testcase that shows the remaining limitation of constructor reloading (that can be fixed, with a bit of work).<br>
	 * 
	 * In the first version of the type the closure references a field 'sone' in outer scope. This means the ctor has an extra
	 * parameter of type Reference that is stuffed into a field in the closure that has the same name as the externally referenced
	 * thing (sone).
	 * 
	 * In the second version of the type the closure is changed to refer to a different field in the outer scope. Now called 'stwo'.
	 * This makes the constructor appear to have changed and so we run the funky ctor instead of the regular ctor. This wouldn't
	 * necessarily be a problem except it means we failed to call the super constructor with the right fields.
	 * 
	 * There are two solutions, the expensive general solution and the cheap solution for groovy closures. For now I may go with the
	 * latter. We know the shape of these things, and we know that a characteristic of generated closures if that they call the
	 * super ctor (Closure.<init>) passing in two parameters (owner,this). For Closure based types we can just do a special and
	 * instead of our funky ctor, we generate a special one that takes these two arguments and we use that one.
	 */
	@Test
	public void closure4_oneReference() throws Exception {
		String t = "simple.BasicWithClosureD";
		String c = "simple.BasicWithClosureD$_run_closure1";
		String c2 = "simple.BasicWithClosureD$_run_closure1_closure2";
		TypeRegistry r = getTypeRegistry(t + "," + c + "," + c2);
		ReloadableType ctype = r.addType(c, loadBytesForClass(c));
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("foo() running\nstring is abc\nowner is not null", result.stdout);

		rtype.loadNewVersion(
				"2",
				retrieveRename(t, t + "2",
						"simple.BasicWithClosureD2$_run_closure1:simple.BasicWithClosureD$_run_closure1",
						"simple.BasicWithClosureD2:simple.BasicWithClosureD"));

		ctype.loadNewVersion(
				"2",
				retrieveRename(
						c,
						"simple.BasicWithClosureD2$_run_closure1",
						"simple.BasicWithClosureD2$_run_closure1_closure2:simple.BasicWithClosureD$_run_closure1_closure2",
						"simple.BasicWithClosureD2:simple.BasicWithClosureD"));

		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("foo() running\nstring is def\nowner is not null", result.stdout);
	}

	@Ignore
	// something has changed in closure structure and the tests need debugging to get to the bottom of it
	/**
	 * Variation of testcase above, but now we move between 1/2/3 references on reload (to exercise new/old constructors
	 * 
	 */
	@Test
	public void closure5_multipleReferences() throws Exception {
		String t = "simple.BasicWithClosureE";
		String c = "simple.BasicWithClosureE$_run_closure1";
		TypeRegistry r = getTypeRegistry(t + "," + c);
		ReloadableType ctype = r.addType(c, loadBytesForClass(c));
		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("foo() running\nstring is abc\nowner is not null", result.stdout);

		rtype.loadNewVersion(
				"2",
				retrieveRename(t, t + "2",
						"simple.BasicWithClosureE2$_run_closure1:simple.BasicWithClosureE$_run_closure1",
						"simple.BasicWithClosureE2:simple.BasicWithClosureE"));

		ctype.loadNewVersion(
				"2",
				retrieveRename(c, "simple.BasicWithClosureE2$_run_closure1",
						"simple.BasicWithClosureE2:simple.BasicWithClosureE"));

		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("foo() running\nstring is abc def\nowner is not null", result.stdout);

		rtype.loadNewVersion(
				"3",
				retrieveRename(t, t + "3",
						"simple.BasicWithClosureE3$_run_closure1:simple.BasicWithClosureE$_run_closure1",
						"simple.BasicWithClosureE3:simple.BasicWithClosureE"));

		ctype.loadNewVersion(
				"3",
				retrieveRename(c, "simple.BasicWithClosureE3$_run_closure1",
						"simple.BasicWithClosureE3:simple.BasicWithClosureE"));

		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("foo() running\nstring is xyz\nowner is not null", result.stdout);
	}

	// TODO testcases:
	// multiple external references
	// external reference is to a primitive type?

	// TODO run these tests without -noverify, why are they failing
	// verification?

	@Ignore
	// something has changed in closure structure and the tests need debugging to get to the bottom of it
	/**
	 * Testing the grails controller usage of closure fields.
	 * 
	 * Ok. Closure names look like this for controller fields: Controller$_closureN where N is the number down the file.
	 * 
	 * if you introduce a new earlier one, that will 'replace' the one you were using before, and as the constructor is not re-run,
	 * you don't see the change. Two options:
	 * <ul>
	 * <li>repairing the damage
	 * <li>rerunning the ctor
	 * </ul>
	 */
	@Test
	public void testControllers1() throws Exception {
		String t = "controller.Controller";
		String closure = "controller.Controller$_closure1";
		TypeRegistry r = getTypeRegistry(t + "," + closure);
		ReloadableType ttype = r.addType(t, loadBytesForClass(t));
		ReloadableType closuretype = r.addType(closure, loadBytesForClass(closure));

		result = runUnguarded(ttype.getClazz(), "execute");
		assertEquals("[action:list, params:2]", result.stdout);

		// Change the body of the 'index' closure
		closuretype.loadNewVersion("2", retrieveRename(closure, "controller.Controller2$_closure1"));
		result = runUnguarded(ttype.getClazz(), "execute");
		assertEquals("[action:custard, params:345]", result.stdout);

		// Introduced a new closure, left the index one unchanged...
		closuretype.loadNewVersion("3", retrieveRename(closure, "controller.Controller3$_closure1"));

		result = runUnguarded(ttype.getClazz(), "execute");
		System.out.println(result);
		// assertEquals("[action:custard, params:345]", result.stdout);

	}

	@Test
	public void staticInitializerReloading1() throws Exception {
		String t = "clinitg.One";
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
		String t = "clinitg.One";
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
	 * Dealing with final fields. This test was passing until groovy started really inlining the final fields. After
	 * doing so it isn't sufficient to run the static initializer to get them set to the new values.
	 */
	@Ignore
	@Test
	public void staticInitializerReloading3() throws Exception {
		String t = "clinitg.Two";
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
	 * Reloading enums written in groovy - very simple enum
	 */
	@Test
	public void enums() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String enumtype = "enums.WhatAnEnum";
		String intface = "enums.ExtensibleEnum";
		String runner = "enums.RunnerA";
		TypeRegistry typeRegistry = getTypeRegistry(enumtype + "," + intface + "," + runner);
		//		ReloadableType rtypeIntface = 
		typeRegistry.addType(intface, loadBytesForClass(intface));
		ReloadableType rtypeEnum = typeRegistry.addType(enumtype, loadBytesForClass(enumtype));
		ReloadableType rtypeRunner = typeRegistry.addType(runner, loadBytesForClass(runner));
		result = runUnguarded(rtypeRunner.getClazz(), "run");
		// ClassPrinter.print(rtypeEnum.bytesInitial);
		assertContains("[RED GREEN BLUE]", result.stdout);
		System.out.println(result);
		byte[] bs = retrieveRename(enumtype, enumtype + "2",
				"enums.WhatAnEnum2$__clinit__closure1:enums.WhatAnEnum$__clinit__closure1",
				"[Lenums/WhatAnEnum2;:[Lenums/WhatAnEnum;",
				"Lenums/WhatAnEnum2;:Lenums/WhatAnEnum;",
				"enums/WhatAnEnum2:enums/WhatAnEnum");
		ClassPrinter.print(bs);
		rtypeEnum.loadNewVersion(bs);
		result = runUnguarded(rtypeRunner.getClazz(), "run");
		System.out.println(result);
		assertContains(
				"[RED GREEN BLUE YELLOW]",
				result.stdout);

		// assertEquals("55", result.returnValue);
		// rtype.loadNewVersion("39", retrieveRename(t, t + "2"));
		// rtype.runStaticInitializer();
		// result = runUnguarded(rtype.getClazz(), "run");
		// assertEquals("99", result.returnValue);
	}

	/**
	 * Reloading enums - more complex enum (grails-7776)
	 */
	@Test
	public void enums2() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String enumtype = "enums.WhatAnEnumB";
		String intface = "enums.ExtensibleEnumB";
		String runner = "enums.RunnerB";
		String closure = "enums.WhatAnEnumB$__clinit__closure1";
		TypeRegistry typeRegistry = getTypeRegistry(enumtype + "," + intface + "," + runner + "," + closure);
		//		ReloadableType rtypeIntface =
		typeRegistry.addType(intface, loadBytesForClass(intface));
		ReloadableType rtypeClosure = typeRegistry.addType(closure, loadBytesForClass(closure));
		ReloadableType rtypeEnum = typeRegistry.addType(enumtype, loadBytesForClass(enumtype));
		ReloadableType rtypeRunner = typeRegistry.addType(runner, loadBytesForClass(runner));
		result = runUnguarded(rtypeRunner.getClazz(), "run");
		assertContains(
				"[PETS_AT_THE_DISCO 1 JUMPING_INTO_A_HOOP 2 HAVING_A_NICE_TIME 3 LIVING_ON_A_LOG 4 WHAT_DID_YOU_DO 5 UNKNOWN 0]",
				result.stdout);

		byte[] cs = retrieveRename(closure, "enums.WhatAnEnumB2$__clinit__closure1",
				"enums.WhatAnEnumB2:enums.WhatAnEnumB");
		rtypeClosure.loadNewVersion(cs);
		byte[] bs = retrieveRename(enumtype, enumtype + "2",
				"enums.WhatAnEnumB2$__clinit__closure1:enums.WhatAnEnumB$__clinit__closure1",
				"[Lenums/WhatAnEnumB2;:[Lenums/WhatAnEnumB;", "enums/WhatAnEnumB2:enums/WhatAnEnumB");
		rtypeEnum.loadNewVersion(bs);
		result = runUnguarded(rtypeRunner.getClazz(), "run");
		System.out.println(result);
		assertContains(
				"[PETS_AT_THE_DISCO 1 JUMPING_INTO_A_HOOP 2 HAVING_A_NICE_TIME 3 LIVING_ON_A_LOG 4 WHAT_DID_YOU_DO 5 WOBBLE 6 UNKNOWN 0]",
				result.stdout);
	}

	/**
	 * Type that doesn't really have a clinit
	 */
	@Ignore
	// Needs investigating...likely a change in groovy bytecode format tripping us up
	@Test
	public void staticInitializerReloading4() throws Exception {
		String t = "clinitg.Three";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1", result.returnValue);
		rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		rtype.runStaticInitializer();
		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1", result.returnValue);
		rtype.loadNewVersion("3", retrieveRename(t, t + "3"));
		// Dont need to do this - think that is because of some of the constant
		// reinit stuff we already
		// drive in groovy
		rtype.runStaticInitializer();
		result = runUnguarded(rtype.getClazz(), "run");
		// ClassPrinter.print(rtype.getLatestExecutorBytes());
		assertEquals("4", result.returnValue);
	}

	/**
	 * Loading a type and not immediately running the clinit.
	 * 
	 * <p>
	 * This is to cover the problem where some type is loaded but not immediately initialized, it is then reloaded
	 * before initialization (i.e. before the clinit has run). If it is a groovy type then we are going to poke at it
	 * during reloading (to clear some caches). This poking may trigger the clinit to run. Now, the helper methods (like
	 * those that setup the callsitecache) will be using the 'new version' but the clinit hasn't been redirected to the
	 * reloaded version and so it indexes into the callsite cache using wrong indices.
	 */
	@Ignore
	// until I can find time
	@Test
	public void staticInitializerReloading5() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String t = "clinitg.Four";
		String t2 = "clinitg.FourHelper";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));
		// ReloadableType rtype2 =
		typeRegistry.addType(t2, loadBytesForClass(t2));
		typeRegistry.getClassLoader().loadClass(t); // load it but do not initialize it
		captureOn();
		byte[] renamed = retrieveRename(t, t + "2");
		rtype.loadNewVersion("2", renamed); // reload it, this will trigger initialization
		String s = captureOffReturnStdout();
		assertEquals("1a", s);

		result = runUnguarded(rtype.getClazz(), "run");
		assertEquals("1312", result.stdout);
	}

}
