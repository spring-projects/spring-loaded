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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springsource.loaded.AnyTypePattern;
import org.springsource.loaded.ExactTypePattern;
import org.springsource.loaded.PrefixTypePattern;


/**
 * Tests for the TypePattern handling.
 * 
 * @author Andy Clement
 * @since 1.0
 */
public class TypePatternTests extends SpringLoadedTests {

	@Test
	public void prefix() {
		PrefixTypePattern tp = new PrefixTypePattern("com.foo..*");
		assertEquals("text:com.foo..*", tp.toString());
		assertTrue(tp.matches("com.foo.Bar"));
		assertFalse(tp.matches("com.food.Bar"));
	}

	@Test
	public void exact() {
		ExactTypePattern tp = new ExactTypePattern("com.foo.Bark");
		assertTrue(tp.matches("com.foo.Bark"));
		assertFalse(tp.matches("com.food.Bar"));
	}

	@Test
	public void any() {
		AnyTypePattern tp = new AnyTypePattern();
		assertTrue(tp.matches("com.foo.Bar"));
		assertTrue(tp.matches("com.food.Bar"));
	}

}
