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
import static org.junit.Assert.assertEquals;
import org.springsource.loaded.ClassRenamer;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypeRegistry;

/**
 * Checking the computation of catchers.
 * 
 * @author Andy Clement
 */
@SuppressWarnings("unused")
public class CatcherTests extends SpringLoadedTests {

	/* 
	 * Details on catchers
	 * 
	 * Four types of method in the super type to think about:
	 * - private
	 * - protected
	 * - default
	 * - public
	 * 
	 * And things to keep in mind:
	 * - private methods are not overridable (invokespecial is used to call them)
	 * - visibility cannot be reduced, only widened
	 * - static methods are not overridable
	 * 
	 * Catching rules:
	 * - don't need a catcher for a private method, there cannot be code out there that calls it with INVOKEVIRTUAL
	 * - visibility is preserved except for protected/default, which is widened to public - this enables the executor to call the
	 *   catcher.  Doesn't seem to have any side effects (doesn't limit the ability for an overriding method in a further
	 *   subclass to have been declared initially protected).
	 */

	@Test
	public void rewrite() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("catchers.B");
		loadClass("catchers.A");
		TypeDescriptor typeDescriptor = typeRegistry.getExtractor().extract(loadBytesForClass("catchers.B"), true);
		checkDoesNotContain(typeDescriptor, "privateMethod");
		checkDoesContain(typeDescriptor, "0x1 publicMethod");
		checkDoesContain(typeDescriptor, "0x1 protectedMethod");
		checkDoesContain(typeDescriptor, "0x1 defaultMethod");

		ReloadableType rtype = typeRegistry.addType("catchers.B", loadBytesForClass("catchers.B"));

		reload(rtype, "2");
	}

	/**
	 * Exercising the two codepaths for a catcher. The first 'run' will run the super version. The second 'run' will
	 * dispatch to our new implementation.
	 */
	@Test
	public void exerciseCatcher() throws Exception {
		TypeRegistry registry = getTypeRegistry("catchers..*");
		String a = "catchers.A";
		String b = "catchers.B";
		ReloadableType rtypeA = registry.addType(a, loadBytesForClass(a));
		ReloadableType rtypeB = registry.addType(b, loadBytesForClass(b));

		Class<?> clazz = loadit("catchers.Runner",
				ClassRenamer.rename("catchers.Runner", loadBytesForClass("catchers.Runner")));

		assertStartsWith("catchers.B@", runUnguarded(clazz, "runToString").returnValue);
		Assert.assertEquals(65, runUnguarded(clazz, "runPublicMethod").returnValue);
		Assert.assertEquals(23, runUnguarded(clazz, "runProtectedMethod").returnValue);

		rtypeB.loadNewVersion("2", retrieveRename(b, b + "2"));

		Assert.assertEquals("hey!", runUnguarded(clazz, "runToString").returnValue);
		Assert.assertEquals(66, runUnguarded(clazz, "runPublicMethod").returnValue);
		Assert.assertEquals(32, runUnguarded(clazz, "runProtectedMethod").returnValue);

		// 27-Aug-2010 - typical catcher - TODO should we shorten some type names/method names to reduce class file size? 
		//		METHOD: 0x0001(public) publicMethod()V
		//	    CODE
		//	    GETSTATIC catchers/B.r$typeLorg/springsource/loaded/ReloadableType;
		//	    LDC 0
		//	    INVOKEVIRTUAL org/springsource/loaded/ReloadableType.fetchLatestIfExists(I)Ljava/lang/Object;
		//	    DUP
		//	    IFNULL L0
		//	    CHECKCAST catchers/B__I
		//	    ALOAD 0
		//	    INVOKEINTERFACE catchers/B__I.publicMethod(Lcatchers/B;)V
		//	    RETURN
		//	 L0
		//	    POP
		//	    ALOAD 0
		//	    INVOKESPECIAL catchers/A.publicMethod()V
		//	    RETURN

	}

	/**
	 * Now we work with a mixed hierarchy. Type X declares the methods, type Y extends X does not, type Z extends Y
	 * does.
	 */
	@Test
	public void exerciseCatcher2() throws Exception {
		TypeRegistry registry = getTypeRegistry("catchers..*");

		String x = "catchers.X";
		String y = "catchers.Y";
		String z = "catchers.Z";

		ReloadableType rtypeX = registry.addType(x, loadBytesForClass(x));
		ReloadableType rtypeY = registry.addType(y, loadBytesForClass(y));
		ReloadableType rtypeZ = registry.addType(z, loadBytesForClass(z));

		Class<?> clazz = loadRunner("catchers.Runner2");

		Assert.assertEquals(1, runUnguarded(clazz, "runPublicX").returnValue);
		Assert.assertEquals(1, runUnguarded(clazz, "runPublicY").returnValue); // Y does not override
		Assert.assertEquals(3, runUnguarded(clazz, "runPublicZ").returnValue);

		Assert.assertEquals('a', runUnguarded(clazz, "runDefaultX").returnValue);
		Assert.assertEquals('a', runUnguarded(clazz, "runDefaultY").returnValue); // Y does not override
		Assert.assertEquals('c', runUnguarded(clazz, "runDefaultZ").returnValue);

		Assert.assertEquals(100L, runUnguarded(clazz, "runProtectedX").returnValue);
		Assert.assertEquals(100L, runUnguarded(clazz, "runProtectedY").returnValue); // Y does not override
		Assert.assertEquals(300L, runUnguarded(clazz, "runProtectedZ").returnValue);

		rtypeY.loadNewVersion("2", retrieveRename(y, y + "2"));

		Assert.assertEquals(1, runUnguarded(clazz, "runPublicX").returnValue);
		Assert.assertEquals(22, runUnguarded(clazz, "runPublicY").returnValue); // now Y does
		Assert.assertEquals(3, runUnguarded(clazz, "runPublicZ").returnValue);

		Assert.assertEquals('a', runUnguarded(clazz, "runDefaultX").returnValue);
		Assert.assertEquals('B', runUnguarded(clazz, "runDefaultY").returnValue); // now Y does
		Assert.assertEquals('c', runUnguarded(clazz, "runDefaultZ").returnValue);

		// Runner2.runProtectedX invokes x.callProtectedMethod() which simply returns 'protectedMethod()'
		Assert.assertEquals(100L, runUnguarded(clazz, "runProtectedX").returnValue);
		Assert.assertEquals(200L, runUnguarded(clazz, "runProtectedY").returnValue); // now Y does
		Assert.assertEquals(300L, runUnguarded(clazz, "runProtectedZ").returnValue);
	}

	// TODO are reloadings happening too frequently now that ctors will force them?

	protected Class<?> loadRunner(String name) {
		return loadit(name, loadBytesForClass(name));
	}
}
