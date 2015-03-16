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
 * Tests for working with reloading of inner classes.
 * 
 * @author Andy Clement
 */
public class InnerClassesTests extends SpringLoadedTests {

	/**
	 * This tests what happens when referencing an inner type. When the reload occurs the new executor lives in a new
	 * classloader which would mean it cannot see a default visibility inner type. Default inner types (and default
	 * ctors) are being promoted to public so that they can be seen from the other classloader - that enables the test
	 * to work.
	 */
	@Test
	public void reloadDefaultVisInner() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("inners..*");

		typeRegistry.addType("inners.One$Inner", loadBytesForClass("inners.One$Inner"));
		ReloadableType rtype = typeRegistry.addType("inners.One", loadBytesForClass("inners.One"));
		runUnguarded(rtype.getClazz(), "runner");

		rtype.loadNewVersion("2", retrieveRename("inners.One", "inners.One2", "inners.One2$Inner:inners.One$Inner"));
		runUnguarded(rtype.getClazz(), "runner");
	}

	/**
	 * Similar to the first test but this is just using a regular default visibility class.
	 */
	@Test
	public void reloadDefaultVisClass() throws Exception {
		String tclass = "inners.Two";
		TypeRegistry typeRegistry = getTypeRegistry("inners..*");

		typeRegistry.addType("inners.TwoDefault", loadBytesForClass("inners.TwoDefault"));
		ReloadableType rtype = typeRegistry.addType(tclass, loadBytesForClass(tclass));
		runUnguarded(rtype.getClazz(), "runner");

		rtype.loadNewVersion("2", retrieveRename(tclass, tclass + "2"));
		runUnguarded(rtype.getClazz(), "runner");
	}

	/**
	 * Similar to the first test but this is just using a private visibility inner class. Private inner class becomes
	 * default visibility when compiled
	 */
	@Test
	public void reloadPrivateVisInner() throws Exception {
		String tclass = "inners.Three";
		TypeRegistry typeRegistry = getTypeRegistry("inners..*");


		ReloadableType rtype = typeRegistry.addType(tclass, loadBytesForClass(tclass));
		runUnguarded(rtype.getClazz(), "runner");

		//		ReloadableType rtypeInner = 
		typeRegistry.addType("inners.Three$Inner",
				retrieveRename("inners.Three$Inner", "inners.Three2$Inner", "inners.Three2:inners.Three"));

		rtype.loadNewVersion(
				"2",
				retrieveRename(tclass, tclass + "2", "inners.Three2$Inner:inners.Three$Inner",
						"inners.Three2:inners.Three"));
		runUnguarded(rtype.getClazz(), "runner");
	}

	/**
	 * Similar to the first test but this is just using a protected visibility inner class. Protected inner class
	 * becomes public visibility when compiled
	 */
	@Test
	public void reloadProtectedVisInner() throws Exception {
		String tclass = "inners.Four";
		TypeRegistry typeRegistry = getTypeRegistry("inners..*");
		typeRegistry.addType("inners.Four$Inner",
				retrieveRename("inners.Four$Inner", "inners.Four2$Inner", "inners.Four2:inners.Four"));
		ReloadableType rtype = typeRegistry.addType(tclass, loadBytesForClass(tclass));
		runUnguarded(rtype.getClazz(), "runner");

		rtype.loadNewVersion(
				"2",
				retrieveRename(tclass, tclass + "2", "inners.Four2$Inner:inners.Four$Inner", "inners.Four2:inners.Four"));
		runUnguarded(rtype.getClazz(), "runner");
	}
}
