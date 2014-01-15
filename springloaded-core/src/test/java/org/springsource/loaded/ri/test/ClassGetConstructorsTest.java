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

import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;


/**
 * Test Class.getDeclaredConstructors and Class.getConstructors
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
// @PredictResult
public class ClassGetConstructorsTest extends GenerativeSpringLoadedTest {

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;
	private Object callerInstance;
	//	private Set<String> signatures;

	// Parameters that change for different test runs
	private Class<?> targetClass; //One class chosen to focus test on
	private String targetMethodName;

	@Override
	protected String getTargetPackage() {
		return "reflection.constructors";
	}

	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {
		targetMethodName = "call" + choice("GetDeclaredConstructors", "GetConstructors");
		toStringValue.append(targetMethodName + ": ");

		if (choice()) {
			targetClass = targetClass("java.lang.String");
		} else {
			targetClass = targetClass("ClassWithConstructors", choice("", "002"));
		}

		callerClazz = loadClassVersion("reflection.ClassInvoker", "");
		callerInstance = newInstance(callerClazz);
	}

	@Override
	public Result test() throws ResultException, Exception {
		try {
			Result r = runOnInstance(callerClazz, callerInstance, targetMethodName, targetClass);
			return r;
		} catch (ResultException e) {
			throw new Error(e);
		}
	}

	@Override
	protected void assertEqualResults(Result expected, Result actual) {
		assertEqualUnorderedToStringLists(expected, actual);
	}

}
