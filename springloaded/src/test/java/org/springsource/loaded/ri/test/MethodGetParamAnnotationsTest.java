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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;


/**
 * Tests - Method.getAnnotations - Method.getDeclaredAnnotations As well as these same methods called via {@link AnnotatedElement}
 * and {@link AccessibleObject}.
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
public class MethodGetParamAnnotationsTest extends GenerativeSpringLoadedTest {

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;
	private Object callerInstance;

	// Parameters that change for different test runs
	private Class<?> targetClass; //One class chosen to focus test on
	private Method method; //A method declared on the target class

	private String testedMethodCaller;

	@Override
	protected String getTargetPackage() {
		return "reflection.methodannotations";
	}

	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {

		//For Methods objects these two methods should behave the same since annotations on
		//  Methods are not inherited
		testedMethodCaller = "call" + choice("Method") + choice("GetParameterAnnotations");
		//toStringValue.append(testedMethodCaller+": "); //Don't include in toString value.. only one possible value!

		if (choice()) {
			targetClass = targetClass("ParamAnnotClass", choice("", "002"));
		} else {
			targetClass = targetClass("ParamAnnotInterface", choice("", "002"));
		}

		callerClazz = loadClassVersion("reflection.AnnotationsInvoker", "");
		callerInstance = newInstance(callerClazz);

		method = targetMethodFrom(targetClass);
	}

	@Override
	public Result test() throws ResultException, Exception {
		try {
			Result r = runOnInstance(callerClazz, callerInstance, testedMethodCaller, method);
			Assert.assertTrue(r.returnValue instanceof List<?>);
			return r;
		} catch (ResultException e) {
			throw new Error(e);
		}
	}

}
