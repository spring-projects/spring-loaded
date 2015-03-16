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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;

import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;


/**
 * Test - Class.getFields - Class.getDeclaredFields
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
// @PredictResult
public class ClassGetFieldsTest extends GenerativeSpringLoadedTest {

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;

	private Object callerInstance;

	//	private Set<String> signatures;

	// Parameters that change for different test runs
	private Class<?> targetClass; //One class chosen to focus test on

	private String targetMethodName;

	@Override
	protected String getTargetPackage() {
		return "reflection.fields";
	}

	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {
		targetMethodName = "call" + choice("GetFields", "GetDeclaredFields");
		toStringValue.append(targetMethodName + ": ");

		if (choice()) {
			/* Test with a non reloadable class */
			targetClass = targetClass("java.awt.Frame");
		}
		else if (choice()) {
			targetClass("InterfaceTarget", choice("", "002"));
			targetClass = targetClass("ClassTarget", choice("", "002"));
			if (choice()) {
				targetClass = targetClass("SubClassTarget", choice("", "002"));
			}
		}
		else {
			targetClass = targetClass("InterfaceTarget", choice("", "002"));
			if (choice()) {
				targetClass = targetClass("S1InterfaceTarget", choice("", "002"));
				if (choice()) {
					targetClass = targetClass("S2InterfaceTarget", choice("", "002"));
				}
			}
		}

		callerClazz = loadClassVersion("reflection.ClassInvoker", "");
		callerInstance = newInstance(callerClazz);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Result test() throws ResultException, Exception {
		try {
			Result r = runOnInstance(callerClazz, callerInstance, targetMethodName, targetClass);
			collectFieldNames((List<Field>) r.returnValue);
			return r;
		}
		catch (ResultException e) {
			throw new Error(e);
		}
	}

	@Override
	protected void assertEqualResults(Result expected, Result actual) {
		assertEqualUnorderedToStringLists(expected, actual);
	}

	////////////////////////////////////////////////////////////////////////////////////////// 
	// Code below not really part of the test, but used to help gather field names to paste into
	// ClassGetField test

	private static HashSet<String> seen = new HashSet<String>();

	/**
	 * We use this to print our the field names
	 * 
	 * @param returnValue
	 */
	private void collectFieldNames(List<Field> fields) {
		for (Field f : fields) {
			if (!seen.contains(f.getName())) {
				System.out.println("\"" + f.getName() + "\",");
				seen.add(f.getName());
			}
		}
	}

}
