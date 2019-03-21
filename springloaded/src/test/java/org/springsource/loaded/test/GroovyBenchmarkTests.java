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

package org.springsource.loaded.test;

import org.junit.Before;
import org.junit.Test;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.test.infra.TestClassloaderWithRewriting;


public class GroovyBenchmarkTests extends SpringLoadedTests {

	@Before
	public void setUp() throws Exception {
		switchToGroovy();
	}

	/**
	 * I'm interested in checking the performance difference between having compilable call sites on and off. So let's
	 * load a program that simply makes a method call 1000s of times. No reloading, this is just to check the runtime
	 * cost of rewriting.
	 */
	@Test
	public void benchmarkingGroovyMethodInvocations() throws Exception {
		binLoader = new TestClassloaderWithRewriting();
		String t = "simple.BasicH";
		String target = "simple.BasicETarget";

		TypeRegistry r = getTypeRegistry(t + "," + target);

		ReloadableType rtype = r.addType(t, loadBytesForClass(t));
		//		ReloadableType rtypeTarget = 
		r.addType(target, loadBytesForClass(target));

		//		result = runUnguarded(rtype.getClazz(), "run");
		//		System.out.println(result.returnValue + "ms");
		// compilable off
		// 2200,2331,2117
		// arrays rather than collections:
		// 1775, 1660
		// compilable on (but still using collections)
		// 516, 716, 669
		// compilable on and arrays:
		// 238ms (compilable on configurable in GroovyPlugin)
		// ok - avoid rewriting field references to $callSiteArray (drops to 319ms)
		// 116ms! (this change not yet active)

		// changing it so that we dont intercept $getCallSiteArray either
		// 92ms

		// run directly (main method in BasicH)
		// 56 !

		System.out.print("warmup ... ");
		for (int loop = 0; loop < 10; loop++) {
			System.out.print(loop + " ");
			result = runUnguarded(rtype.getClazz(), "run");
			//			System.out.println(result.returnValue + "ms");
		}
		System.out.println();
		long total = 0;
		result = runUnguarded(rtype.getClazz(), "run");
		total += new Long((String) result.returnValue).longValue();
		result = runUnguarded(rtype.getClazz(), "run");
		total += new Long((String) result.returnValue).longValue();
		result = runUnguarded(rtype.getClazz(), "run");
		total += new Long((String) result.returnValue).longValue();
		System.out.println("Average for 3 iterations is " + (total / 3) + "ms");

		//		rtype.loadNewVersion("2", retrieveRename(t, t + "2"));
		//		rtypeTarget.loadNewVersion("2", retrieveRename(target, target + "2"));
		//
		//		result = runUnguarded(rtype.getClazz(), "run");
		//		assertEquals("foobar", result.returnValue);
	}
}
