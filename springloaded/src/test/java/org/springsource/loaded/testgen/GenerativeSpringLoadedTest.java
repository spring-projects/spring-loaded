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

package org.springsource.loaded.testgen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springsource.loaded.ri.ReflectiveInterceptor;
import org.springsource.loaded.test.infra.Result;


/**
 * This class is intended to be subclassed to create 'generated' sprongloaded tests. It needs to be run with the
 * {@link ExploreAllChoicesRunner} test runner, using the {@link RunWith} annotation.
 * <p>
 * To create a generative test two things come together:
 * 
 * <ul>
 * <li>A mechanism to create different test configurations based on 'random' choices. These random choices are made by
 * the test's 'setup' method calling the provided 'choice' methods.
 * 
 * <li>A mechanism to run the same test twice in two different execution contexts. It is the responsibility of the test
 * subclass. One context uses an ordinary Java classloader to obtain Class objects by loading a the 'final' version of
 * the class. The other uses SpringLoaded infrastructure, and reloads a class's successive version up to the 'final'
 * version.
 * </ul>
 * 
 * On a first run, the test runner will provide a 'recording' random choices generator and an 'ordinary Java context'.
 * The test is run multiple times until all possible choices have been explored. for each test run the choices are
 * recorded together with the observed test result. This used to populate the test tree.
 * <p>
 * Then the tests are run again replaying the recorded choices, in a SpringLoaded context. The result is compared with
 * the result from the first run. The test fails if the results are not 'equals'.
 * 
 * @author kdvolder
 */
public abstract class GenerativeSpringLoadedTest extends GenerativeTest {

	/**
	 * Provides the execution context where we can load class versions, so that we can then uses these classes to
	 * execute reflective calls on them.
	 * <p>
	 * During 'generative' setup, an execution context with a standard java class loader is used to 'predict' the
	 * expected test result. During 'replay' a SpringLoaded based implementation is used instead.
	 */
	protected IClassProvider classProvider = null;

	/**
	 * To have 'nice' toString value and display name. While generating test parameters, add some text to this buffer to
	 * describe the selected parameter. Method in this class that are called 'targetXXX' generally will add some text to
	 * this buffer.
	 */
	protected StringBuffer toStringValue = new StringBuffer();

	/**
	 * Typically, each test has a particular 'target package' which is a package in the test data project that contains
	 * the reloadable classes that this test is operating on. This method must be implemented by the subclass to provide
	 * a suitable value.
	 */
	protected abstract String getTargetPackage();

	/**
	 * Loads up a given version of a given type.
	 * <p>
	 * In 'JustJava' mode a standard Java classloader is used to immediately load the stipulated version.
	 * <p>
	 * In 'SpringLoaded' mode the original version of the type is loaded first. Then successive version are reloaded
	 * until the stipulated version number is reached.
	 * <p>
	 * This method should only be called to load a class that has not been loaded yet or an error will occur.
	 * <p>
	 * Since loading a class may trigger loading of dependent classes, it is important to call this method on classes in
	 * the correct order.
	 * 
	 * @param typeName Dotted name of the type to load.
	 * @param version One of "", "002", "003", ...
	 */
	protected Class<?> loadClassVersion(String typeName, String version) {
		return classProvider.loadClassVersion(typeName, version);
	}

	/**
	 * Get a type from the classloader. Use this to get references to already loaded classes, or to get classes that
	 * fall outside the reloadable types universe.
	 */
	public Class<?> classForName(String className) throws ClassNotFoundException {
		return classProvider.classForName(className);
	}

	@Override
	public void setup() throws Exception, RejectedChoice {
		super.setup();
		if (generative) {
			classProvider = new JustJavaClassProvider();
		}
		else {
			classProvider = new SpringLoadedClassProvider(getReloableTypeConfig());
		}
		chooseTestParameters();
	}

	/**
	 * This method should be overridden in order for a test to choose its test parameters. If a test throws
	 * RejectedChoice exception, this test configuration will be silently ignored. Any other exceptions raised will
	 * result in an error initialising the test suite.
	 * 
	 * @throws RejectedChoice
	 * @throws Exception
	 */
	protected abstract void chooseTestParameters() throws RejectedChoice, Exception;

	/**
	 * Override this in your own test class to configure SpringLoaded type registry.
	 */
	protected String getReloableTypeConfig() {
		String targetPackage = getTargetPackage();
		Assert.assertNotNull(targetPackage);
		return targetPackage + "..*";
	}

	/**
	 * Select a method from given class's declared methods as a test target.
	 * 
	 * @throws RejectedChoice If class has no methods to choose from.
	 */
	protected Method targetMethodFrom(Class<?> targetClass) throws RejectedChoice {
		Method[] methods = ReflectiveInterceptor.jlClassGetDeclaredMethods(targetClass);

		//To be deterministic we must sort these methods in a predictable fashion! Otherwise the test
		//may compare results from one method in the first run with those of another method in the second
		//run and fail.
		sort(methods);
		// Arrays.sort(methods, new ToStringComparator());
		Method method = choice(methods);
		toStringValue.append(method);
		return method;
	}

	/**
	 * Select a field from given class's declared field as a test target.
	 * 
	 * @throws RejectedChoice If class has no fields to choose from.
	 */
	protected Field targetFieldFrom(Class<?> clazz, FieldGetMethod howToGet) throws RejectedChoice {
		Field[] fields = null;
		switch (howToGet) {
			case GET_DECLARED_FIELD:
			case GET_DECLARED_FIELDS:
				fields = ReflectiveInterceptor.jlClassGetDeclaredFields(clazz);
				break;
			case GET_FIELD:
			case GET_FIELDS:
				fields = ReflectiveInterceptor.jlClassGetFields(clazz);
				break;
		}
		//To be deterministic we must sort these in a predictable fashion! 
		//		Arrays.sort(fields, new ToStringComparator());
		sort(fields);
		Field f = choice(fields);
		toStringValue.append(f.getName());
		try {
			switch (howToGet) {
				case GET_DECLARED_FIELDS:
				case GET_FIELDS:
					return f;
				case GET_DECLARED_FIELD:
					return ReflectiveInterceptor.jlClassGetDeclaredField(clazz, f.getName());
				case GET_FIELD:
					return ReflectiveInterceptor.jlClassGetField(clazz, f.getName());
			}
		}
		catch (Exception e) {
			throw new Error(e);
		}
		return f;
	}

	/**
	 * Select a Constructor from given class's declared constructors as a test target.
	 * 
	 * @throws RejectedChoice If class has no Constructors to choose from.
	 */
	protected Constructor<?> targetConstructorFrom(Class<?> clazz) throws RejectedChoice {
		Constructor<?>[] constructors = ReflectiveInterceptor.jlClassGetDeclaredConstructors(clazz);
		//To be deterministic we must sort these methods in a predictable fashion! 
		//Arrays.sort(constructors, new ToStringComparator());
		sort(constructors);
		Constructor<?> c = choice(constructors);
		toStringValue.append(c);
		return c;
	}

	/**
	 * Load and selected a target type for testing. This adds the name of the class to the 'toStringValue' making it
	 * part of the test description.
	 */
	protected Class<?> targetClass(String typeName, String version) {
		toStringValue.append(typeName + version + " ");
		return loadClassVersion(getTargetPackage() + "." + typeName, version);
	}

	/**
	 * Similar to other targetClass method, but for getting an unversioned, non-reloadable type.
	 * <p>
	 * Typically this class will <b>not</b> be in the target package (since it is non-reloadable) so you must explicitly
	 * include the package name in the type name.
	 */
	protected Class<?> targetClass(String fullyQuallifiedName) throws ClassNotFoundException {
		if (fullyQuallifiedName == null) {
			toStringValue.append("null");
			return null;
		}
		else {
			Class<?> clazz = classForName(fullyQuallifiedName);
			toStringValue.append(clazz.getSimpleName() + " ");
			return clazz;
		}
	}

	@Override
	public String getConfigDescription() {
		return toStringValue.toString();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " " + getConfigDescription();
	}

	/**
	 * Most of the stuff we are interested in (Methods, Classes, Annotations, will not be 'equals') when they are
	 * executed in a different classloader. So we approximate this by just calling 'toString' on returned objects and
	 * comparing those.
	 */
	@Override
	protected void assertEqualResults(Result expected, Result actual) {
		Assert.assertEquals("" + expected.returnValue, "" + actual.returnValue);
	}

	/**
	 * If your test is expected to return a list of stuff that may not be 'equals' to each other because
	 * <ul>
	 * <li>objects come from different classloaders and are not equals
	 * <li>the order of the objects may vary
	 * </ul>
	 * Then, assuming the objects have a reasonable toString implementation, you can use this method as an
	 * implementation of assertEqualResults. Simply call this method from your assertEqualResults method.
	 */
	protected void assertEqualUnorderedToStringLists(Result _expected, Result _actual) {
		List<String> expected = toStringList((List<?>) _expected.returnValue);
		List<String> actual = toStringList((List<?>) _actual.returnValue);

		StringBuffer msg = new StringBuffer("Actual " + actual + " don't match expected " + expected + "\n");

		List<String> extra = new ArrayList<String>(actual);
		extra.removeAll(expected);
		if (!extra.isEmpty()) {
			msg.append("extra: \n");
			for (String string : extra) {
				msg.append("   " + string + "\n");
			}
		}

		List<String> missing = new ArrayList<String>(expected);
		missing.removeAll(actual);
		if (!missing.isEmpty()) {
			msg.append("missing: \n");
			for (String string : missing) {
				msg.append("   " + string + "\n");
			}
		}
		Assert.assertTrue(msg.toString(), missing.isEmpty() && extra.isEmpty());
		Assert.assertEquals("Duplicates in result?", expected.size(), actual.size());
	}

	/**
	 * Converts a list of any type of object into a list of Strings by calling the toString method on each object.
	 */
	protected List<String> toStringList(List<?> list) {
		List<String> result = new ArrayList<String>();
		for (Object obj : list) {
			result.add("" + obj);
		}
		return result;
	}

	protected void sort(Object[] os) {
		for (int i = 0; i < os.length; i++) {
			for (int x = 1; x < os.length - i; x++) {
				if (os[x - 1].toString().compareTo(os[x].toString()) > 0) {
					Object temp = os[x - 1];
					os[x - 1] = os[x];
					os[x] = temp;

				}
			}
		}
	}

}
