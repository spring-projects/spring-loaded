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
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.FieldGetMethod;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;


/**
 * Test for reflective field getting and setting: test all the different getter/setter methods on fields.
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
//@PredictResult
public class FieldGetAndSetTest extends GenerativeSpringLoadedTest {

	private static final String TARGET_PACKAGE = "reflection.fields";

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;

	private Object callerInstance;

	// Parameters that change for different test runs
	private List<Class<?>> loadedClasses = new ArrayList<Class<?>>();

	private Class<?> targetClass;

	private Field field; //Field we should get/set

	String calledMethod;

	boolean setAccess;

	@Override
	protected String getTargetPackage() {
		return TARGET_PACKAGE;
	}

	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {
		setAccess = choice();
		if (setAccess) {
			toStringValue.append("setAccess ");
		}
		calledMethod = choice("callGet", "callSetNull",

				"callSetAndGet", "callSetUnboxAndGet",

				"callSetAndGetBoolean", "callSetAndGetByte", "callSetAndGetChar", "callSetAndGetShort",
				"callSetAndGetInt",
				"callSetAndGetLong", "callSetAndGetDouble", "callSetAndGetFloat",

				"callSetBoolean", "callSetByte", "callSetChar", "callSetShort", "callSetInt", "callSetLong",
				"callSetDouble",
				"callSetFloat");
		toStringValue.append(calledMethod + ": ");
		targetClass = targetClass("reflection.nonrelfields.NonReloadableClassWithFields");
		loadedClasses.add(targetClass);
		if (choice()) {
			targetClass = targetClass("ClassTarget", choice("", "002"));
			loadedClasses.add(targetClass);
			if (choice()) {
				targetClass = targetClass("SubClassTarget", choice("", "002"));
				loadedClasses.add(targetClass);
			}
		}
		int i = choice(loadedClasses.size());
		toStringValue.append(" " + i + " ");

		field = targetFieldFrom(loadedClasses.get(i), FieldGetMethod.GET_DECLARED_FIELDS);
		//				if (!(field.getName().contains("nrlChar"))) {
		//					throw new RejectedChoice(); // Filter these tests for now
		//				}

		callerClazz = loadClassVersion("reflection.FieldInvoker", "");
		callerInstance = newInstance(callerClazz);
	}

	@Override
	public Result test() throws ResultException, Exception {
		Object targetInstance = newInstance(targetClass);
		field.setAccessible(setAccess);
		return runOnInstance(callerClazz, callerInstance, calledMethod, field, targetInstance);
	}

}
