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

package org.springsource.loaded.test;

import org.junit.Test;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;


/**
 * Measuring performance of Java.
 * 
 * @author Andy Clement
 * @since 0.7.3
 */
public class JavaMicroBenchmarkTests extends SpringLoadedTests {

	/**
	 * Check calling it, reloading it and calling the new version.
	 * 
	 * The groovy test just makes 1000000 calls, this makes 50million calls each time.
	 * <p>
	 * <ul>
	 * <li>Run directly as a java program: 5ms !
	 * <li>2011Apr15 - 7112ms/7073ms
	 * <li>Changed to array access for TypeRegistry.getReloadableType
	 * <li>5663ms
	 * <li>Changed TypeRegistry instances from map to array
	 * <li>1684ms !! 1430ms
	 * </ul>
	 */
	@Test
	public void javaMethodInvocation() throws Exception {
		String t = "benchmarks.MethodInvoking";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		ReloadableType rtype = typeRegistry.addType(t, loadBytesForClass(t));

		warmup(rtype, 10);
		pause(10);
		average(rtype, 5);
	}

	// TODO fibonacci

	private void average(ReloadableType rtype, int count) throws Exception {
		long total = 0;
		for (int loop = 0; loop < count; loop++) {
			result = runUnguarded(rtype.getClazz(), "run");
			total += new Long((String) result.returnValue).longValue();
		}
		System.out.println("Average for " + count + " iterations is " + (total / count) + "ms");
	}

	private void warmup(ReloadableType rtype, int count) throws Exception {
		System.out.print("warmup ... ");
		for (int loop = 0; loop < count; loop++) {
			System.out.print(loop + " ");
			result = runUnguarded(rtype.getClazz(), "run");
		}
		System.out.println();
	}
}
