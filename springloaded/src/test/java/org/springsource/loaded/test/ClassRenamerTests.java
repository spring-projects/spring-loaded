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


import org.junit.Assert;
import org.junit.Test;
import org.springsource.loaded.ClassRenamer;


/**
 * Tests for renaming of a class.
 * 
 * @author Andy Clement
 */
public class ClassRenamerTests extends SpringLoadedTests {

	/**
	 * Load the byteform of a class, manipulate the bytes to rename it, then try and define it and use it under the new
	 * name
	 */
	@Test
	public void simpleRename() {
		byte[] classbytes = loadBytesForClass("data.Fruity002");
		byte[] renamedbytes = ClassRenamer.rename("data.Fruity", classbytes);
		Class<?> clazz = loadit("data.Fruity", renamedbytes);
		Object value = run(clazz, "getFruit");
		Assert.assertEquals("orange", value);
	}

}
