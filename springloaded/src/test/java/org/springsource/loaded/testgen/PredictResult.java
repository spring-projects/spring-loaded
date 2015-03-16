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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Attach this annotation to a test class that uses the ExploreAllChoices runner. To enable result prediction in the
 * construction of the suite. Without this tag results will be predicted only during the actual test run, which speeds
 * up the construction of the test suite.
 * <p>
 * The price to pay for this speedup is that predicted results will not be shown as part of the test name.
 * 
 * @author kdvolder
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PredictResult {
}
