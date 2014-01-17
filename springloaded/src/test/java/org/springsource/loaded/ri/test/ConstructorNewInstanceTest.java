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

import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;


/**
 * Tests the following methods:
 * 
 * Constructor.newInstance
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
public class ConstructorNewInstanceTest extends GenerativeSpringLoadedTest {

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;
	private Object callerInstance;
	private Class<?> targetClass; //One class chosen to focus test on

	// Parameters that change for different test runs
	private boolean doSetAccess; //Should we call 'setAccessible'?
	private Constructor<?> member; //A constructor declared on this class
	private Object[] args; //List of arguments that should be passed to constructor

	private String testedMethodCaller;

	@Override
	protected String getTargetPackage() {
		return "reflection.constructors";
	}

	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {
		//We can test all of these methods, they have the same kinds of parameters.

		doSetAccess = choice();
		if (doSetAccess) {
			toStringValue.append("setAccess ");
		}

		testedMethodCaller = "callNewInstance";

		//		toStringValue.append(testedMethodCaller+": ");

		targetClass = targetClass("ClassForNewInstance", choice("", "002"));
		member = targetConstructorFrom(targetClass);

		if (choice()) {
			// A regular kind of invoker
			callerClazz = loadClassVersion("reflection.ConstructorInvoker", "");
		} else {
			// Also try an invoker that has special priviliges because...
			callerClazz = targetClass; // The caller is the class itself

			//TODO: other cases (subclass of the class, same package as the class).
		}
		toStringValue.append(" from " + callerClazz.getSimpleName());
		if (generative) {
			callerInstance = callerClazz.newInstance(); // makes it easier to debug specific test case
														// because avoids driving "test generation" thorugh the ReflectiveInterceptor
		} else {
			callerInstance = newInstance(callerClazz);
		}

		args = chooseArgs(member.getParameterTypes());

	}

	/**
	 * Choose a number of arguments, to be passed to the selected constructor, based on that constructor's formal parameters.
	 * 
	 * @throws RejectedChoice
	 */
	private Object[] chooseArgs(Class<?>[] parameterTypes) throws RejectedChoice {
		if (parameterTypes.length == 0) {
			Object[] result = choice() ? null : new Object[0];
			toStringValue.append(result == null ? "null" : "[]");
			return result;
		}
		toStringValue.append("(");
		Object[] args = new Object[parameterTypes.length];
		for (int i = 0; i < args.length; i++) {
			if (i > 0) {
				toStringValue.append(", ");
			}
			args[i] = chooseArg(parameterTypes[i]);
			toStringValue.append("" + args[i]);
		}
		toStringValue.append(")");
		return args;
	}

	private Object chooseArg(Class<?> param) throws RejectedChoice {
		if (int.class == param) {
			return (int) (choice() ? 0 : 15);
		} else if (boolean.class == param) {
			return choice();
		} else if (String.class == param) {
			return choice(null, "someString");
		} else if (double.class == param) {
			return (double) 3.14;
		} else if (float.class == param) {
			return (float) 3.14;
		} else if (char.class == param) {
			return (char) 'A';
		}
		throw new Error("Don't know how to provide parameter value for: " + param);
	}

	@Override
	public Result test() throws ResultException, Exception {
		if (doSetAccess) {
			member.setAccessible(true);
		}
		Result r = runOnInstance(callerClazz, callerInstance, testedMethodCaller, member, args);
		return r;
	}

}
