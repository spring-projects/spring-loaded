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
import org.springsource.loaded.testgen.PredictResult;
import org.springsource.loaded.testgen.RejectedChoice;


@RunWith(ExploreAllChoicesRunner.class)
@PredictResult
public class FieldSetAccessTest extends GenerativeSpringLoadedTest {

	private static final String TARGET_PACKAGE = "reflection.fields";

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;

	private Object callerInstance;

	// Parameters that change for different test runs
	private Class<?> targetClass; //One class chosen to focus test on

	private String fieldName; //Field we should get

	boolean setAccess; //Should we call "setAccess" on the field

	String calledMethod;

	@Override
	protected String getTargetPackage() {
		return TARGET_PACKAGE;
	}

	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {
		calledMethod = choice("getFieldWithAccess", "setFieldWithAccess", "setAndGetFieldWithAccess");
		toStringValue.append(calledMethod + ": ");
		if (choice()) {
			//Try a non reloadable class
			targetClass = targetClass("java.awt.Frame");
			fieldName = choice("title", "base");
		}
		else {
			targetClass = targetClass("FieldSetAccessTarget", choice("", "002"));
			fieldName = choice("privateField", "protectedField", "defaultField", "publicField", "finalPublicField",
					"finalPrivateField", "deletedPublicField");
		}
		toStringValue.append(fieldName);
		setAccess = choice();
		toStringValue.append(setAccess ? " setAccess" : "");

		if (!targetClass.getName().equals("java.awt.Frame")
				&& (calledMethod.equals("getFieldWithAccess") || calledMethod.equals("setFieldWithAccess")) && choice()) {
			//For accessing a class from within itself, different access check behaviour expected!
			callerClazz = targetClass;
			toStringValue.append(" from " + callerClazz.getSimpleName());
		}
		else {
			callerClazz = loadClassVersion("reflection.FieldInvoker", "");
		}
		callerInstance = newInstance(callerClazz);
	}

	@Override
	public Result test() throws ResultException, Exception {
		return runOnInstance(callerClazz, callerInstance, calledMethod, targetClass, fieldName, setAccess);
	}

}
