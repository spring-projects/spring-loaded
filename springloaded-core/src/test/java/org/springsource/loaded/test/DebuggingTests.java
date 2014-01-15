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



import org.junit.Assert;
import org.junit.Test;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;


/**
 * @author Andy Clement
 */
public class DebuggingTests extends SpringLoadedTests {
	@Test
	public void rewrite() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.HelloWorld");
		ReloadableType rtype = typeRegistry.addType("data.HelloWorld", loadBytesForClass("data.HelloWorld"));
		runUnguarded(rtype.getClazz(), "greet");

		// Just transform the existing version into a dispatcher/executor
		rtype.loadNewVersion("000", rtype.bytesInitial);
		Assert.assertEquals("Greet from HelloWorld", runUnguarded(rtype.getClazz(), "greet").stdout);

		// Load a real new version
		rtype.loadNewVersion("002", retrieveRename("data.HelloWorld", "data.HelloWorld002"));
		Assert.assertEquals("Greet from HelloWorld 2", runUnguarded(rtype.getClazz(), "greet").stdout);
		//		ClassPrinter.print(rtype.getLatestExecutorBytes());
		//		ClassPrinter.print(rtype.bytesInitial);

	}
}