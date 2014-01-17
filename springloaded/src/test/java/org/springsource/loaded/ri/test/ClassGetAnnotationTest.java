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
public class ClassGetAnnotationTest extends GenerativeSpringLoadedTest {

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;
	private Object callerInstance;

	// Parameters that change for different test runs
	private Class<?> targetClass; //One class chosen to focus test on
	private String testedMethodCaller; // Method to call in 'invoker' class
	private Class<? extends Annotation> annotClass; //Annotation class to test for

	@Override
	protected String getTargetPackage() {
		return "reflection.classannotations";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {

		testedMethodCaller = "call" + choice("Class", "AnnotatedElement") + choice("GetAnnotation", "IsAnnotationPresent");

		toStringValue.append(testedMethodCaller + ": ");

		if (choice()) {
			toStringValue.append("NRL");
			targetClass = targetClass(getTargetPackage() + "." + "ClassTarget");
		} else if (choice()) {
			targetClass = targetClass("ClassTarget", choice("", "002", "003"));
			if (!choice()) {
				targetClass = targetClass("SubClassTarget", choice("", "002", "003"));
			}
		} else {
			targetClass = targetClass("InterfaceTarget", choice("", "002", "003"));
			if (!choice()) {
				targetClass = targetClass("SubInterfaceTarget", choice("", "002"));
			}
		}

		String annotClassName = choice(null, "reflection.AnnoT", "reflection.AnnoT3", "reflection.AnnoTInherit");
		annotClass = (Class<Annotation>) targetClass(annotClassName);

		callerClazz = loadClassVersion("reflection.AnnotationsInvoker", "");
		callerInstance = newInstance(callerClazz);
	}

	@Override
	public Result test() throws ResultException, Exception {
		Result r = runOnInstance(callerClazz, callerInstance, testedMethodCaller, targetClass, annotClass);
		return r;
	}

}
