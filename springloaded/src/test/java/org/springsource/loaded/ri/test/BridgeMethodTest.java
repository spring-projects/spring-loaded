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

import static org.springsource.loaded.test.SpringLoadedTests.runOnInstance;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;


/**
 * Tests both Class.getAnnotation and Class.isAnnotationPresent at once.
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
public class BridgeMethodTest extends GenerativeSpringLoadedTest {

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;

	private Object callerInstance;

	// Parameters that change for different test runs
	private Class<?> targetClass; //One class chosen to focus test on

	private String testedMethodCaller; // Method to call in 'invoker' class

	private Method targetMethod; // Method to call the method on

	@Override
	protected String getTargetPackage() {
		return "reflection.bridgemethods";
	}

	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {

		testedMethodCaller = "call" + choice("IsSynthetic", "IsBridge");

		toStringValue.append(testedMethodCaller + ": ");

		targetClass = targetClass("ClassWithBridgeMethod", choice("", "002"));
		targetMethod = targetMethodFrom(targetClass);

		callerClazz = loadClassVersion("reflection.MethodInvoker", "");
		callerInstance = callerClazz.newInstance();
	}

	@Override
	public Result test() throws ResultException, Exception {
		try {
			Result r = runOnInstance(callerClazz, callerInstance, testedMethodCaller, targetMethod);
			return r;
		}
		catch (ResultException e) {
			Assert.fail(e.toString());
			return null;
		}
	}

}
