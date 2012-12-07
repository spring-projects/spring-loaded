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

import static org.springsource.loaded.ri.test.AbstractReflectionTests.newInstance;
import static org.springsource.loaded.test.SpringLoadedTests.runOnInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.List;

import junit.framework.Assert;

import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;


/**
 * Tests Generics and VarArgs related methods in {@link Method}
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
public class MethodGenericsAndVarArgsTest extends GenerativeSpringLoadedTest {

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;
	private Object callerInstance;

	// Parameters that change for different test runs
	private Class<?> targetClass; //One class chosen to focus test on
	private Method method; //A method declared on the target class

	private String testedMethodCaller;

	@Override
	protected String getTargetPackage() {
		return "reflection.generics";
	}

	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {

		testedMethodCaller = "call"
				+ choice("GetGenericReturnType", "GetTypeParameters", "ToGenericString", "GetGenericExceptionTypes",
						"GetGenericParameterTypes", "IsVarArgs");
		toStringValue.append(testedMethodCaller + ": ");

		//		if (choice()) { //Overkill?
		//			targetClass = targetClass("java.util.TreeMap");
		//		} else 
		if (choice()) {
			targetClass = targetClass("GenericClass", choice("", "002"));
		} else {
			targetClass = targetClass("GenericInterface", choice("", "002"));
		}

		callerClazz = loadClassVersion("reflection.MethodInvoker", "");
		callerInstance = newInstance(callerClazz);

		method = targetMethodFrom(targetClass);
	}

	@Override
	public Result test() throws ResultException, Exception {
		Result r = runOnInstance(callerClazz, callerInstance, testedMethodCaller, method);
		return r;
	}

	////////////////////////////////////////////////////////////////////////////////////
	// Stuff below implements 'equality' comparison for the expected results of these tests.
	// The usual 'toString' comparison used in many other tests will not suffice here because
	// the toString of type variables and Type objects doesn't capture much info (so the
	// tests would pass too easily with incorrect / incomplete results).
	// 
	// e.g.  for a type variable toString only shows the name. But type variables have type bounds
	// and scope associated with them and these things have to be checked for correctness somehow.
	//
	// The implementation below essentially compares two types (or lists of them) by traversing 
	// the graph of information reachable by public API methods and using toString equality only
	// when an objects with a 'rich' toString method is reached. 

	@Override
	protected void assertEqualResults(Result _expected, Result _actual) {
		equalsCache.clear();
		Object expected = _expected.returnValue;
		Object actual = _actual.returnValue;
		if (expected instanceof Type) {
			assertEqualTypes((Type) expected, (Type) actual);
		} else if (expected instanceof List<?>) {
			assertEqualLists((List<?>) expected, (List<?>) actual);
		} else {
			Assert.fail("All cases (expected by this test) covered above");
			//Note: boolean case is already handled before we get here.
		}
	}

	private void assertEqualLists(List<?> expected, List<?> actual) {
		Assert.assertEquals(expected.size(), actual.size());
		for (int i = 0; i < expected.size(); i++) {
			assertEqualListElements(expected.get(i), actual.get(i));
		}
	}

	private void assertEqualListElements(Object expected, Object actual) {
		if (expected instanceof Type) {
			assertEqualTypes((Type) expected, (Type) actual);
		} else {
			Assert.fail("Unreachable: only expected things in lists in this test should be Types");
		}
	}

	/**
	 * For debugging purposes... making breakpoint on stack overflow doesn't leave enough stack for actual debugging ...
	 */
	private static int stackLimit = 8;

	private void assertEqualTypes(Type expected, Type actual) {
		System.out.println("Equal? " + expected + "  " + actual);
		boolean checkedOrBusy = equalsCache.contains(new TwoObjects(expected, actual));
		if (checkedOrBusy) {
			// Consider them ok until shown otherwise... this cuts infinite recursion trying to find 
			// a difference in a circular structure (i.e. hitting same object pair again recursively we 
			// may consider them equal)
			return;
		}

		equalsCache.add(new TwoObjects(expected, actual));

		if (stackLimit-- < 0) {
			Assert.fail("Too deep recursion");
		}
		try {
			if (expected instanceof ParameterizedType) {
				assertEqualParameterizedTypes((ParameterizedType) expected, (ParameterizedType) actual);
			} else if (expected instanceof Class<?>) {
				Assert.assertEquals(expected.toString(), actual.toString());
			} else if (expected instanceof TypeVariable<?>) {
				assertEqualTypeVariables((TypeVariable<?>) expected, (TypeVariable<?>) actual);
			} else {
				//TODO: [...] no coverage in test cases for these kinds of results:
				//  GenericArrayType
				//  WildCardType
				Assert.fail("Imlement comparison logic for " + expected.getClass() + " & " + actual.getClass());
			}
		} finally {
			stackLimit++;
		}
	}

	private void assertEqualTypeVariables(TypeVariable<?> expected, TypeVariable<?> actual) {
		Assert.assertEquals(expected.getName(), actual.getName());
		assertEqualTypeArrays(expected.getBounds(), actual.getBounds()); //order not relevant: this check is stronger than it needs to be
		assertEqualGenericDecl(expected.getGenericDeclaration(), actual.getGenericDeclaration());
	}

	private void assertEqualGenericDecl(GenericDeclaration expected, GenericDeclaration actual) {
		if (expected instanceof Class<?> || expected instanceof Method || expected instanceof Constructor<?>) {
			Assert.assertEquals(expected.toString(), actual.toString());
		} else {
			Assert.fail("Unreachable code, all cases covered above");
		}
	}

	private void assertEqualParameterizedTypes(ParameterizedType expected, ParameterizedType actual) {
		assertEqualTypes(expected.getRawType(), actual.getRawType());
		assertEqualTypeArrays(expected.getActualTypeArguments(), actual.getActualTypeArguments());
	}

	private void assertEqualTypeArrays(Type[] expected, Type[] actual) {
		Assert.assertEquals(expected.length, actual.length);
		for (int i = 0; i < actual.length; i++) {
			assertEqualTypes(expected[i], actual[i]);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////
	// Code below relates to a 'cache' of the result of 'equals' checks. 
	//
	// Anything in the cache has been checked already or is in the process of being
	// checked. This cache is not just for speed, it is to be able to handle cycles in the
	// graph and avoid infinite recursion.

	private static HashSet<TwoObjects> equalsCache = new HashSet<TwoObjects>();

	public static class TwoObjects {
		final Object o1;
		final Object o2;

		public TwoObjects(Object o1, Object o2) {
			super();
			this.o1 = o1;
			this.o2 = o2;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj.getClass() != this.getClass())
				return false;
			TwoObjects other = (TwoObjects) obj;
			return o1 == other.o1 && o2 == other.o2;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(o1) + 17 * System.identityHashCode(o2);
		}
	}

}
