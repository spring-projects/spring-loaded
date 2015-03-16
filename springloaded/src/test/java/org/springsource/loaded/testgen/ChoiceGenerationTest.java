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


import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;


/**
 * This tests whether the choice generator setup in the runner produces test configurations that we expected.
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
// @PredictResult
public class ChoiceGenerationTest extends GenerativeTest {

	public static String invokerTypeName = "reflection.ClassInvoker";

	public static String[] targetTypeNames = { "reflection.targets.ClassTarget", "reflection.targets.SubClassTarget",
		"reflection.targets.SubClassImplementsInterface", "reflection.targets.InterfaceTarget" };

	/**
	 * Tests parameter to be chosen by the test based on injected choice generator.
	 */
	private String targetTypeName = null;

	@Override
	public void setup() throws Exception, RejectedChoice {
		super.setup();
		targetTypeName = choice(targetTypeNames);
	}

	@Override
	public Result test() throws ResultException {
		//		System.out.println(""+generative+" "+choiceGenerator+" "+targetTypeName);
		checkTestHistory();
		return new Result(targetTypeName, "", "");
	}

	void checkTestHistory() {
		String testId = choiceGenerator.toString();
		if (testId.equals("11")) {
			Assert.assertEquals("reflection.targets.ClassTarget", targetTypeName);
		}
		else if (testId.equals("10")) {
			Assert.assertEquals("reflection.targets.SubClassTarget", targetTypeName);
		}
		else if (testId.equals("01")) {
			Assert.assertEquals("reflection.targets.SubClassImplementsInterface", targetTypeName);
		}
		else if (testId.equals("00")) {
			Assert.assertEquals("reflection.targets.InterfaceTarget", targetTypeName);
		}
		else {
			Assert.fail("Unexpected test config: " + testId);
		}
	}

	@Override
	public String getConfigDescription() {
		return choiceGenerator + " => " + targetTypeName;
	}

	@Override
	public String toString() {
		return "TestChoiceGeneration: " + getConfigDescription();
	}
}
