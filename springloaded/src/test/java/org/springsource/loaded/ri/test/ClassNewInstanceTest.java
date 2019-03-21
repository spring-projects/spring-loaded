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

import static org.springsource.loaded.ri.test.AbstractReflectionTests.newInstance;
import static org.springsource.loaded.test.SpringLoadedTests.runOnInstance;

import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;


/**
 * Tests the following methods:
 * 
 * Class.newInstance
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
// @PredictResult
public class ClassNewInstanceTest extends GenerativeSpringLoadedTest {

	// TODO: this test doesn't try any sophisticated things re 'access' checking
	//   E.g.
	//     accessing protected / package visible members without doing setAccess, from
	//     a method that should be allowed to do this via reflection.

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;

	private Object callerInstance;

	private Class<?> targetClass; //One class chosen to focus test on

	// Parameters that change for different test runs
	private String testedMethodCaller;

	@Override
	protected String getTargetPackage() {
		return "reflection.constructors";
	}

	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {
		//We can test all of these methods, they have the same kinds of parameters.

		testedMethodCaller = "callClassNewInstance";

		//		toStringValue.append(testedMethodCaller+": ");

		targetClass = targetClass("ClassForNewInstance", choice("", "002", "003", "004"));

		callerClazz = loadClassVersion("reflection.ConstructorInvoker", "");
		callerInstance = newInstance(callerClazz);

	}

	@Override
	public Result test() throws ResultException, Exception {
		Result r = runOnInstance(callerClazz, callerInstance, testedMethodCaller, targetClass);
		return r;
	}

}
