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

package org.springsource.loaded.testgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.springsource.loaded.test.infra.IResult;
import org.springsource.loaded.test.infra.ResultException;


/**
 * This test runner in injects an IChoiceGenerator into the test class it is running. The choice generator is injected
 * by setting choiceGenerator field in the test instance.
 * <p>
 * For now this test runner assumes the test class is using JUnit 3 style method conventions for setup and teardown, and
 * should provide only a single 'test' method to run the tests.
 * 
 * @author kdvolder
 */
public class ExploreAllChoicesRunner extends ParentRunner<GeneratedTest> {

	private static boolean generatedTestsOn = Boolean.parseBoolean(System.getProperty(
			"springloaded.tests.generatedTests", "true"));

	private Class<? extends GenerativeSpringLoadedTest> testClass;

	private List<GeneratedTest> children = null;

	private boolean predictResults;

	public ExploreAllChoicesRunner(Class<? extends GenerativeSpringLoadedTest> testClass) throws InitializationError {
		super(testClass);
		this.testClass = testClass;
		this.predictResults = testClass.isAnnotationPresent(PredictResult.class);
	}

	@Override
	protected List<GeneratedTest> getChildren() {
		if (!generatedTestsOn)
			return Collections.emptyList();
		if (children != null)
			return children;
		List<GeneratedTest> newChildren = new ArrayList<GeneratedTest>();
		try {
			SystematicChoiceGenerator choiceGenerator = new SystematicChoiceGenerator();
			do {
				GenerativeTest test = testClass.newInstance();

				//Inject a choice generator into the test!
				test.choiceGenerator = choiceGenerator;
				test.generative = true;

				try {
					test.setup();
					IResult r = null;
					if (predictResults) {
						try {
							r = test.test(); //run test in generative mode
						}
						catch (ResultException e) {
							r = e;
						}
						catch (RejectedChoice e) {
							//Choices shouldn't be rejected during test run only during setup!
							throw new IllegalStateException(e);
						}
					}
					addTest(newChildren, new GeneratedTest(choiceGenerator.choices, r, test.getConfigDescription()));
				}
				catch (RejectedChoice e) {
					//Ignore this test
				}
				finally {
					test.teardown();
				}
			}
			while (choiceGenerator.backtrack());
			children = newChildren;
			return newChildren;
		}
		catch (Exception e) {
			//TODO: [...] use JUnit framework to handle this more gracefully
			//  This probably means overriding one of the validation methods and, moving most of this code into validation
			//  and storing the children during validation so that this method here will only have to return the stored
			//  children and should never raise any errors.
			throw new Error(e);
		}
	}

	protected void addTest(List<GeneratedTest> newChildren, GeneratedTest test) {
		//		System.out.println("Adding "+test.getDisplayName());
		newChildren.add(test);
	}

	@Override
	protected Description describeChild(GeneratedTest child) {
		return Description.createTestDescription(testClass, sanitise(child.getDisplayName()));
	}

	/**
	 * Certain characters confuse the Eclipse JUnit view... replace those with harmless ones.
	 */
	private String sanitise(String displayName) {
		return displayName.replace('(', '[').replace(')', ']').replace('\n', ' ').replace('\r', ' ');
	}

	@Override
	protected void runChild(GeneratedTest testPredicted, RunNotifier notifier) {
		notifier.fireTestStarted(describeChild(testPredicted));
		try {
			//Determine expected test result first:
			IResult expectedResult = testPredicted.getExpectedResult();
			if (expectedResult == null) {
				//Suite was not created with @PredictResult must predict it now
				GenerativeTest test = testClass.newInstance();

				//Inject a choice generator into the test!
				test.choiceGenerator = new SystematicChoiceGenerator(testPredicted.getChoices());
				test.generative = true;
				try {
					test.setup();
					expectedResult = test.test();
				}
				catch (ResultException e) {
					expectedResult = e;
				}
				finally {
					test.teardown();
				}
			}

			//Run the test again and verify
			GenerativeTest test = testClass.newInstance();

			//Inject a choice generator into the test!
			test.choiceGenerator = new SystematicChoiceGenerator(testPredicted.getChoices());
			test.generative = false;
			try {
				test.setup();
				Assert.assertEquals(testPredicted.getConfigDescription(), test.getConfigDescription());
				IResult actual;
				try {
					actual = test.test(); //run test in verify mode
				}
				catch (ResultException e) {
					actual = e;
				}
				test.assertEqualIResults(expectedResult, actual);
			}
			finally {
				test.teardown();
			}
		}
		catch (Throwable e) {
			notifier.fireTestFailure(new Failure(describeChild(testPredicted), e));
		}
		finally {
			notifier.fireTestFinished(describeChild(testPredicted));
		}
	}

}
