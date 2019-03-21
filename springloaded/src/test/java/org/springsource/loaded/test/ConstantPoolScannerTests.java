/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springsource.loaded.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springsource.loaded.ConstantPoolScanner;
import org.springsource.loaded.ConstantPoolScanner.References;
import org.springsource.loaded.Utils;

/**
 *
 * @author Andy Clement
 */
public class ConstantPoolScannerTests {

	static class Helper {

		public void methodOne() {
			foo(null);
		}

		public String foo(Serializable s) {
			return new String("hello world");
		}
	}

	@Test
	public void testConstantPoolScanner() throws Exception {
		File fileToCheck = new File("bin/org/springsource/loaded/test/ConstantPoolScannerTests$Helper.class");
		References references = ConstantPoolScanner.getReferences(load(fileToCheck));
		System.out.println(references);
		System.out.println(references.referencedClasses);
		assertContains(references.referencedMethods, "java/lang/String.<init>");
	}


	@Ignore
	@Test
	public void foo() throws Exception {

		//		File[] filesToCheck = new File("../testdata-java8/bin").listFiles();
		File[] filesToCheck = new File("/Users/aclement/play/rt71").listFiles();
		//		File[] filesToCheck = new File("../testdata-groovy/bin").listFiles();
		long[] info = checkThemAll(filesToCheck);
		System.out.println("Time spent checking = " + (info[0] / 1000000d));
		System.out.println("Referenced Classes  = #" + info[1]);
		System.out.println("Referenced Methods  = #" + info[2]);
	}

	// ---

	/**
	 * @param filesToCheck files to check
	 * @return [timeSpentCheckingTotal, referencedClassesTotal, referencedMethodsTotal]
	 */
	private static long[] checkThemAll(File[] filesToCheck) throws Exception {
		long timeSpentCheckingTotal = 0;
		long referencedClassesTotal = 0;
		long referencedMethodsTotal = 0;
		for (File f : filesToCheck) {
			if (f.isDirectory()) {
				long[] totals = checkThemAll(f.listFiles());
				timeSpentCheckingTotal += totals[0];
				referencedClassesTotal += totals[1];
				referencedMethodsTotal += totals[2];
			}
			else if (f.getName().endsWith(".class")) {
				//				System.out.println(f);
				byte[] data = Utils.loadFromStream(new FileInputStream(f));
				long stime = System.nanoTime();
				References refs = ConstantPoolScanner.getReferences(data);
				timeSpentCheckingTotal += (System.nanoTime() - stime);
				referencedClassesTotal += refs.referencedClasses.size();
				referencedMethodsTotal += refs.referencedMethods.size();
			}
		}
		return new long[] { timeSpentCheckingTotal, referencedClassesTotal, referencedMethodsTotal };
	}

	private byte[] load(File file) throws Exception {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			return Utils.loadFromStream(fis);
		}
		finally {
			fis.close();
		}
	}

	private void assertContains(List<String> toSearch, String toSearchFor) {
		for (String string : toSearch) {
			if (string.equals(toSearchFor)) {
				return;
			}
		}
		fail("Did not find '" + toSearchFor + "' in \n" + toSearch);
	}

}
