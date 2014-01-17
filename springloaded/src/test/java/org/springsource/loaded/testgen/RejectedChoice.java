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

/**
 * Exception to be thrown by Generated tests when they choice generator produced an invalid/uninteresting test parameter. Tests
 * throwing 'rejected choice' during the setup phase of the test will be ignored rather than reported as errors.
 * <p>
 * However, tests that throw this exception during the actual test run will not swallow the exception. If test throws 'rejected
 * choice' in the running stage this should only happen because the test behaved differently than it did before, or because of bug
 * in the replay logic of the IChoiceGenerator implementation.
 * 
 * @author kdvolder
 */
@SuppressWarnings("serial")
public class RejectedChoice extends Exception {

}
