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
package org.springsource.loaded.testgen;

import java.lang.reflect.Method;

import junit.framework.Assert;

import org.junit.runner.RunWith;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.ri.ReflectiveInterceptor;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;


/**
 * This tests whether the test runner sets up an appropriate context for executing the tests two times (once to predict the result
 * and once to verify it).
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
@PredictResult
public class ExecutionContextsTest extends GenerativeSpringLoadedTest {

	private String targetTypeName = getTargetPackage() + ".ClassTarget";

	private String version = null;
	private Class<?> targetType = null;

	@Override
	protected String getTargetPackage() {
		return "reflection.targets";
	}

	@Override
	public Result test() throws ResultException, RejectedChoice, Exception {

		//Check that each context is loading the correct version of the class
		if (version.equals("")) {
			try {
				getDeclaredMethod();
				Assert.fail("lateMethod should not exist in first version of the test");
			} catch (NoSuchMethodException e) {
				//OK
			}
		} else {
			Method m = getDeclaredMethod();
			Assert.assertEquals("lateMethod", m.getName());
		}

		//Check that tests are generated/executed in the order they are presumed to:
		checkTestHistory();

		return new Result(targetTypeName, "", "");
	}

	protected void chooseTestParameters() throws RejectedChoice {
		version = choice("", "002");
		targetType = loadClassVersion(targetTypeName, version);
	}

	private Method getDeclaredMethod() throws NoSuchMethodException {
		return ReflectiveInterceptor.jlClassGetDeclaredMethod(targetType, "lateMethod");
	}

	//////////////////////
	// This test is a bit unusual, normally tests should not have mutable static state!
	//
	// Here we abuse static state so we can determine whether we are running in the 'generative' (just java)
	// or the verifying (springloaded) mode. This so that we can check in each context whether we get what we
	// expected to get.

	static int testNum = 0;

	void checkTestHistory() {
		testNum++;
		boolean springLoaded = testNum > 2; // We expect to get two tests, so the 3rd run we should be using
											// SpringLoaded

		int versionNum = testNum % 2;
		Assert.assertEquals(versionNum == 1 ? "" : "002", version);

		if (springLoaded) {
			//Check that we have the right execution context.
			SpringLoadedClassProvider slClassProvider = (SpringLoadedClassProvider) classProvider;
			//Check that classes were loaded with correct class loader
			Assert.assertEquals(slClassProvider.getClassLoader(), targetType.getClassLoader());
			//Check that we have some funky SpringLoaded stuff in this class
			Assert.assertEquals(ReloadableType.class, ReflectiveInterceptor.getRType(targetType).getClass());
		} else { /* Just Java */
			//Check that we have the right execution context.
			JustJavaClassProvider jClassProvider = (JustJavaClassProvider) classProvider;
			//Check that classes were loaded with correct class loader
			Assert.assertEquals(jClassProvider.getClassLoader(), targetType.getClassLoader());
			//Check that we don't have funky SpringLoaded stuff in this class
			Assert.assertNull(ReflectiveInterceptor.getRType(targetType));
		}

	}

	@Override
	public String getConfigDescription() {
		return targetTypeName + version;
	}

	@Override
	public String toString() {
		return "ExecutionContextTest: " + targetTypeName + version + " in " + classProvider;
	}
}
