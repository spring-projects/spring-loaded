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

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;

import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;


/**
 * Tests the following methods:
 * 
 * getAnnotation isAnnotationPresent
 * 
 * It is convenient to test both of these here, since they have the same kinds of argument types, which means generation of test
 * parameters is the same.
 * <p>
 * Note that these same methods are also tested by {@link MethodGetAnnotationTest} and {@link FieldGetAnnotationTest}. But that test
 * only passes Method/Field instances to tested method.
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
//@PredictResult
public class ConstructorGetAnnotationTest extends GenerativeSpringLoadedTest {

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;
	private Object callerInstance;
	private Class<?> targetClass; //One class chosen to focus test on

	// Parameters that change for different test runs
	private AccessibleObject member; //A field or constructor declared on this class
	private Class<Annotation> annotClass; //An annotation type to look for

	private String testedMethodCaller;

	@Override
	protected String getTargetPackage() {
		return "reflection.constructors";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {
		//We can test all of these methods, they have the same kinds of parameters.
		testedMethodCaller = "call" + choice("AnnotatedElement", "AccessibleObject", "Constructor")
				+ choice("IsAnnotationPresent", "GetAnnotation");

		toStringValue.append(testedMethodCaller + ": ");

		targetClass = targetClass("ClassWithAnnotatedConstructors", choice("", "002"));

		String annotClassName = choice("reflection.AnnoT", "reflection.AnnoT2", "reflection.AnnoT3");
		annotClass = (Class<Annotation>) targetClass(annotClassName);

		member = targetConstructorFrom(targetClass);

		callerClazz = loadClassVersion("reflection.AnnotationsInvoker", "");
		callerInstance = newInstance(callerClazz);
	}

	@Override
	public Result test() throws ResultException, Exception {
		Result r = runOnInstance(callerClazz, callerInstance, testedMethodCaller, member, annotClass);
		return r;
	}

}
