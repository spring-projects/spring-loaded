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

import org.junit.ComparisonFailure;
import org.junit.runner.RunWith;
import org.springsource.loaded.test.infra.IResult;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;


/**
 * This class is intended to be subclassed to create 'generated' tests. It needs to be run with the
 * {@link ExploreAllChoicesRunner} test runner, using the {@link RunWith} annotation.
 * <p>
 * To create a generative test two things come together:
 * 
 * <ul>
 * <li>A mechanism to create different test configurations based on 'random' choices. These random choices are made by
 * the test's 'setup' method calling the provided 'choice' methods.
 * 
 * <li>A mechanism to run the same test twice in two different execution contexts. It is the responsibility of the test
 * subclass to setup the appropriate execution context. See {@link GenerativeSpringLoadedTest} for an example.
 * </ul>
 * 
 * The test runner is responsible for injecting implementations of the IChoiceGenerator interface.
 * <p>
 * On a first run, the test runner will provide a 'recording' choice generator. The test is run multiple times until all
 * possible choices have been explored. For each test run the choices are recorded together with the observed test
 * result for those choices. This is used to populate the test tree.
 * <p>
 * Then the tests are run again replaying the recorded choices. The result is compared with the result from the first
 * run. The test fails if the results are not equal (using whatever implementation of equals is provided by the result
 * objects.
 * 
 * @author kdvolder
 */
public abstract class GenerativeTest {

	/**
	 * Injected by the test runner. Use the ChoiceGenerator to implement some logic to choose test parameters. Either in
	 * your setup method or your actual test method.
	 */
	public IChoiceGenerator choiceGenerator = null;

	/**
	 * This field is set by the test runner to indicate whether the test is currently in 'generative' mode, or
	 * 'replay/verify' mode. This flag is mostly intended for the setup method so it can setup an appropriate execution
	 * context.
	 */
	public boolean generative;

	/**
	 * This method should setup the test, using the provided choice generator to construct/choose a test configuration.
	 */
	public void setup() throws Exception, RejectedChoice {
	}

	public void teardown() throws Exception {
	}

	/**
	 * There should be only one test method in this type of test, this is it!
	 * <p>
	 * The test method will be run twice by the runner, once in a 'generative' mode and once in 'verifying' mode.
	 * <p>
	 * In generative mode, it is ok to throw RejectedChoice exception, this will cause the test to be ignored. The test
	 * method itself shouldn't need to know what mode it is running in. The test runner should inject the necessary
	 * context dependencies for the code to be identical in both cases.
	 * <p>
	 * The only obligation the test method has is to ensure that, given a deterministic set of choices is made by the
	 * injected ChoiceGenerator, the test method's behavior should also be deterministic.
	 * 
	 * @throws ResultExeption if the test produces an expected exception as result.
	 * @throws RejectedChoice (in generative mode only) if the generated test should be ignored.
	 * @throws Exception any other exception should be treated as an unexpected error and make the test fail.
	 * @return Result encapsulating the expected result of the test.
	 */
	public abstract Result test() throws ResultException, Exception;

	/**
	 * @return Use choice generator to pick an element from a bunch of Strings
	 * @throws RejectedChoice
	 */
	protected <T> T choice(T... options) throws RejectedChoice {
		return options[choice(options.length)];
	}

	/**
	 * @return number in range 0 (inclusive) to 'hi' (exclusive)
	 * @throws RejectedChoice if 'hi' is negative
	 */
	protected int choice(int hi) throws RejectedChoice {
		return choice(0, hi);
	}

	/**
	 * @return number in range 'lo' (inclusive) to 'hi' (exclusive)
	 * @throws RejectedChoice if 'lo' >= 'hi'
	 */
	protected int choice(int lo, int hi) throws RejectedChoice {
		if (lo >= hi) {
			throw new RejectedChoice(); //Nothing to choose from
		}
		if (hi - lo == 1) {
			//only one choice
			return lo;
		}

		//Use kind of 'binary search' for efficient choice making
		if (choice())
			return choice(lo, (lo + hi) / 2);
		else
			return choice((lo + hi) / 2, hi);
	}

	protected boolean choice() {
		boolean b = choiceGenerator.nextBoolean();
		return b;
	}

	/**
	 * Override this and make it return something other than null to create a nicer name in the JUnit runner view.
	 * Beware that this name must be unique or the Eclipse JUnit runner view will get confused displaying the results
	 * (though tests should still run ok).
	 * <p>
	 * Typically, you should override this to return a string that describes the values for the configuration parameter
	 * values of the test instance.
	 * <p>
	 * If not overridden, the choices 'bitString' will be displayed. This is unique, but not very informative.
	 * 
	 * @return A String uniquely identifying the test or null.
	 */
	public String getConfigDescription() {
		return null;
	}

	/**
	 * This method is called by the test runner to compare predicted results against actual results.
	 * <p>
	 * Override this method to customise how you want these compared.
	 * 
	 * @param expected Result from the 'generative' test run.
	 * @param actual Result from the actual test run.
	 */
	final protected void assertEqualIResults(IResult expected, IResult actual) {
		if (expected.equals(actual)) {
			return;
		}
		if (expected.getClass() != actual.getClass()) {
			//One is an Exception and the other one isn't. these's no way these should
			//ever be treated as equivalent!
			throw new ComparisonFailure(null, expected.toString(), actual.toString());
		}
		if (expected instanceof Result) {
			assertEqualResults((Result) expected, (Result) actual);
		}
		else if (expected instanceof ResultException) {
			assertEqualExceptions((ResultException) expected, (ResultException) actual);
		}
		else {
			//I don't what it is?? There are only two implementations of the interface
			throw new ComparisonFailure(null, expected.toString(), actual.toString());
		}
	}

	/**
	 * This method gets called to compare two ResultExceptions, but only when the standard equals method returned false.
	 * Subclasses may override this to relax the equality check.
	 */
	protected void assertEqualExceptions(ResultException expected, ResultException actual) {
		throw new ComparisonFailure(null, expected.toString(), actual.toString());
	}

	/**
	 * This method gets called to compare two Results, but only when the standard equals method returned false.
	 * Subclasses may override this to relax the equality check.
	 */
	protected void assertEqualResults(Result expected, Result actual) {
		throw new ComparisonFailure(null, expected.toString(), actual.toString());
	}

}
