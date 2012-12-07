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

import java.lang.reflect.Method;

import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;


/**
 * Tests 'Method.invoke' where the method is dispatched in different ways (static or dynamic) and where the receiver object's
 * dynamictype is varies w.r.t. to the declaring type of the invoked method.
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
public class MethodInvokeTest extends GenerativeSpringLoadedTest {

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;
	private Object callerInstance;

	// Parameters that change for different test runs
	private Class<?> declaringClass; // Class to get methods from
	private Method method; // Method to invoke (taken from declaring class's declared methods)
	private Class<?> instanceClass; // Class to create instance of and call the method on
	private Object targetInstance; // Instance of the instanceClass

	@Override
	protected String getTargetPackage() {
		return "reflection.invocation";
	}

	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {

		String[] classNames = { "A", "B", "C" };

		int declaringClassIndex = choice(classNames.length);
		// instance class is always chosen to be a subtype of declaringClass:
		int instanceClassIndex = declaringClassIndex + choice(classNames.length - declaringClassIndex);

		// Now we should load versions of each class that is relevant to the test. That is all classes with index <= instanceClassIndex
		for (int i = 0; i <= instanceClassIndex; i++) {
			Class<?> clazz = targetClass(classNames[i], choice("", "002"));
			if (declaringClassIndex == i) {
				declaringClass = clazz;
				toStringValue.append("<=D ");
			}
			if (instanceClassIndex == i) {
				instanceClass = clazz;
				toStringValue.append("<=I ");
			}
		}

		method = targetMethodFrom(declaringClass);
		targetInstance = newInstance(instanceClass);

		callerClazz = loadClassVersion("reflection.MethodInvoker", "");
		callerInstance = newInstance(callerClazz);
	}

	@Override
	public Result test() throws ResultException, Exception {
		method.setAccessible(true);
		Result r = runOnInstance(callerClazz, callerInstance, "callInvoke", method, targetInstance, new Object[0]);
		return r;
	}

}
