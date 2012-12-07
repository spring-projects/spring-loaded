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

import java.util.List;

import org.springsource.loaded.test.infra.IResult;

import junit.framework.Assert;


/**
 * Helper class used by the test runner. An instance of this class stores a 'choice' configuration and the associated expected
 * result (the result is only stored if it was predicted ahead of time see {@link PredictResult}).
 * 
 * @author kdvolder
 */
public class GeneratedTest {

	private List<Boolean> choices;
	private IResult expectedResult = null;
	private String configDesc = null;

	public GeneratedTest(List<Boolean> choices, IResult expectedResult, String configDesc) {
		Assert.assertNotNull(choices);
		this.choices = choices;
		this.expectedResult = expectedResult;
		this.configDesc = configDesc;
	}

	public static String bitString(List<Boolean> choices) {
		StringBuffer result = new StringBuffer();
		for (boolean b : choices) {
			result.append(b ? '1' : '0');
		}
		return result.toString();
	}

	public List<Boolean> getChoices() {
		return choices;
	}

	@Override
	public String toString() {
		StringBuffer out = new StringBuffer();
		out.append("GeneratedTest " + bitString(choices) + "\n");
		if (configDesc != null)
			out.append("display name = " + configDesc + "\n");
		if (expectedResult != null)
			out.append(expectedResult);
		return out.toString();
	}

	public IResult getExpectedResult() {
		return expectedResult;
	}

	public String getDisplayName() {
		return getConfigDescription() + " => " + getResultSummary();
	}

	private String getResultSummary() {
		if (expectedResult != null)
			return expectedResult.getSummary();
		else {
			return "???";
		}
	}

	public String getConfigDescription() {
		String r;
		if (configDesc != null)
			r = configDesc;
		else
			r = bitString(choices);
		return r;
	}
}
