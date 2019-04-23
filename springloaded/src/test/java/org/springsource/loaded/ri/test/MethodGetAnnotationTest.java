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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.runner.RunWith;
import org.springsource.loaded.ri.ReflectiveInterceptor;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;


/**
 * Tests the following methods:
 * 
 * Method.getAnnotation Method.isAnnotationPresent
 * 
 * It is convenient to test both of these here, since they have the kinds of argument types, which means generation of
 * test parameters is the same.
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
public class MethodGetAnnotationTest extends GenerativeSpringLoadedTest {

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;

	private Object callerInstance;

	// Parameters that change for different test runs
	private Class<?> targetClass; //One class chosen to focus test on

	private Method method; //A method declared on the target class

	private Class<Annotation> annotClass; //An annotation type to look for

	private String testedMethodCaller;

	@Override
	protected String getTargetPackage() {
		return "reflection.methodannotations";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {
		//We can test all of these methods, they have the same kinds of parameters.
		testedMethodCaller = "call"
				+ choice("AnnotatedElement", "AccessibleObject", "Method")
				+ choice("IsAnnotationPresent", "GetAnnotation");

		toStringValue.append(testedMethodCaller + ": ");


		if (choice()) {
			targetClass = targetClass("ClassTarget", choice("", "002", "003"));
		}
		else {
			targetClass = targetClass("InterfaceTarget", choice("", "002", "003"));
		}

		String annotClassName = choice(
				null,
				Deprecated.class.getName(),
				"reflection.AnnoT",
				"reflection.AnnoT2",
				"reflection.AnnoT3"
				);
		annotClass = (Class<Annotation>) targetClass(annotClassName);

		callerClazz = loadClassVersion("reflection.AnnotationsInvoker", "");
		callerInstance = newInstance(callerClazz);

		Method[] methods = ReflectiveInterceptor.jlClassGetDeclaredMethods(targetClass);
		//To be deterministic we must sort these methods in a predictable fashion:
		// Arrays.sort(methods, new ToStringComparator());
		sort(methods);

		method = choice(methods);
		toStringValue.append(method);
	}

	@Override
	public Result test() throws ResultException, Exception {
		Result r = runOnInstance(callerClazz, callerInstance, testedMethodCaller, method, annotClass);
		return r;
	}

}
