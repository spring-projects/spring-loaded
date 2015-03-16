/*
 * Copyright 2014 Pivotal Software Inc. and contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.test.infra.Result;

/**
 * Test reloading of Java 8.
 * 
 * @author Andy Clement
 * @since 1.2
 */
public class Java8Tests extends SpringLoadedTests {

	@Test
	public void theBasics() {
		String t = "basic.FirstClass";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = new ReloadableType(t, sc, 1, typeRegistry, null);

		assertEquals(1, rtype.getId());
		assertEquals(t, rtype.getName());
		assertEquals(slashed(t), rtype.getSlashedName());
		assertNotNull(rtype.getTypeDescriptor());
		assertEquals(typeRegistry, rtype.getTypeRegistry());
	}

	@Test
	public void callBasicType() throws Exception {
		String t = "basic.FirstClass";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals(8, r.returnValue);

		rtype.loadNewVersion("002", rtype.bytesInitial);

		r = runUnguarded(simpleClass, "run");
		assertEquals(8, r.returnValue);
	}

	@Test
	public void lambdaA() throws Exception {
		String t = "basic.LambdaA";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals(77, r.returnValue);

		rtype.loadNewVersion("002", rtype.bytesInitial);
		r = runUnguarded(simpleClass, "run");
		assertEquals(77, r.returnValue);
	}

	@Test
	public void changingALambda() throws Exception {
		String t = "basic.LambdaA";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals(77, r.returnValue);

		byte[] renamed = retrieveRename(t, t + "2", t + "2$Foo:" + t + "$Foo");
		rtype.loadNewVersion("002", renamed);
		r = runUnguarded(simpleClass, "run");
		assertEquals(88, r.returnValue);
	}

	@Test
	public void lambdaWithParameter() throws Exception {
		String t = "basic.LambdaB";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals(99L, r.returnValue);

		byte[] renamed = retrieveRename(t, t + "2", t + "2$Foo:" + t + "$Foo");
		rtype.loadNewVersion("002", renamed);
		r = runUnguarded(simpleClass, "run");
		assertEquals(176L, r.returnValue);
	}


	@Test
	public void lambdaWithTwoParameters() throws Exception {
		String t = "basic.LambdaC";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals(6L, r.returnValue);

		byte[] renamed = retrieveRename(t, t + "2", t + "2$Boo:" + t + "$Boo");
		rtype.loadNewVersion("002", renamed);
		r = runUnguarded(simpleClass, "run");
		assertEquals(5L, r.returnValue);
	}

	@Test
	public void lambdaWithThreeMixedTypeParameters() throws Exception {
		String t = "basic.LambdaD";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals("true342abc", r.returnValue);

		byte[] renamed = retrieveRename(t, t + "2", t + "2$Boo:" + t + "$Boo");
		rtype.loadNewVersion("002", renamed);
		r = runUnguarded(simpleClass, "run");
		assertEquals("def264true", r.returnValue);
	}

	@Test
	public void lambdaWithCapturedVariable() throws Exception {
		String t = "basic.LambdaE";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals("aaaa", r.returnValue);

		byte[] renamed = retrieveRename(t, t + "2", t + "2$Boo:" + t + "$Boo");
		rtype.loadNewVersion("002", renamed);
		r = runUnguarded(simpleClass, "run");
		assertEquals("aaaaaaaa", r.returnValue);
	}

	@Test
	public void lambdaWithThis() throws Exception {
		String t = "basic.LambdaF";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals("aaaaaaa", r.returnValue);

		byte[] renamed = retrieveRename(t, t + "2", t + "2$Boo:" + t + "$Boo");
		rtype.loadNewVersion("002", renamed);

		r = runUnguarded(simpleClass, "run");
		assertEquals("a:a:a:", r.returnValue);
	}

	@Test
	public void lambdaWithNonPublicInnerInterface() throws Exception {
		String t = "basic.LambdaG";
		TypeRegistry typeRegistry = getTypeRegistry("basic..*");

		// Since Boo needs promoting to public, have to ensure it is directly loaded:
		typeRegistry.addType(t + "$Boo", loadBytesForClass(t + "$Boo"));

		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals(99, r.returnValue);

		byte[] renamed = retrieveRename(t, t + "2", t + "2$Boo:" + t + "$Boo");
		rtype.loadNewVersion("002", renamed);
		r = runUnguarded(simpleClass, "run");
		assertEquals(44, r.returnValue);
	}

	@Test
	public void multipleLambdasInOneMethod() throws Exception {
		String t = "basic.LambdaH";
		TypeRegistry typeRegistry = getTypeRegistry("basic..*");

		// Since Foo needs promoting to public, have to ensure it is directly loaded:
		typeRegistry.addType(t + "$Foo", loadBytesForClass(t + "$Foo"));

		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals(56, r.returnValue);

		rtype.loadNewVersion("002", rtype.bytesInitial);
		r = runUnguarded(simpleClass, "run");
		assertEquals(56, r.returnValue);
	}

	@Test
	public void lambdaSignatureChange() throws Exception {
		String t = "basic.LambdaI";
		TypeRegistry typeRegistry = getTypeRegistry("basic..*");

		// Since Foo needs promoting to public, have to ensure it is directly loaded:
		ReloadableType itype = typeRegistry.addType(t + "$Foo", loadBytesForClass(t + "$Foo"));

		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals("a", r.returnValue);

		itype.loadNewVersion("002", retrieveRename(t + "$Foo", t + "2$Foo"));
		rtype.loadNewVersion("002", retrieveRename(t, t + "2", t + "2$Foo:" + t + "$Foo"));

		r = runUnguarded(simpleClass, "run");
		assertEquals("ab", r.returnValue);
	}


	@Test
	public void lambdaInvokeVirtual() throws Exception {
		String t = "basic.LambdaJ";
		TypeRegistry typeRegistry = getTypeRegistry("basic..*");

		// Since Foo needs promoting to public, have to ensure it is directly loaded:
		ReloadableType itype = typeRegistry.addType(t + "$Foo", loadBytesForClass(t + "$Foo"));

		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals("fooa", r.returnValue);

		itype.loadNewVersion("002", retrieveRename(t + "$Foo", t + "2$Foo"));
		rtype.loadNewVersion("002", retrieveRename(t, t + "2", t + "2$Foo:" + t + "$Foo"));

		r = runUnguarded(simpleClass, "run");
		assertEquals("fooab", r.returnValue);
	}

	@Test
	public void streamWithLambda() throws Exception {
		String t = "basic.StreamA";
		TypeRegistry typeRegistry = getTypeRegistry("basic..*");
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");
		assertEquals(3, r.returnValue);

		byte[] renamed = retrieveRename(t, t + "2", t + "2$Foo:" + t + "$Foo");
		rtype.loadNewVersion("002", renamed);
		r = runUnguarded(simpleClass, "run");
		assertEquals(4, r.returnValue);
	}

	// inner interface (for the invokeinterface BSM)
	@Test
	public void streamWithLambdaInvokedVirtually() throws Exception {
		String t = "basic.StreamB";
		TypeRegistry typeRegistry = getTypeRegistry("basic..*");
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");
		assertEquals(3, r.returnValue);

		byte[] renamed = retrieveRename(t, t + "2", t + "2$Foo:" + t + "$Foo");
		rtype.loadNewVersion("002", renamed);
		r = runUnguarded(simpleClass, "run");
		assertEquals(4, r.returnValue);
	}

	// not an inner interface this time (for the invokeinterface BSM)
	@Test
	public void streamWithLambdaInvokedVirtually2() throws Exception {
		String t = "basic.StreamBB";
		TypeRegistry typeRegistry = getTypeRegistry("basic..*");
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");
		assertEquals(3, r.returnValue);

		byte[] renamed = retrieveRename(t, t + "2");
		rtype.loadNewVersion("002", renamed);
		r = runUnguarded(simpleClass, "run");
		assertEquals(4, r.returnValue);
	}

	@Test
	public void streamWithoutLambda() throws Exception {
		String t = "basic.StreamC";
		TypeRegistry typeRegistry = getTypeRegistry("basic..*");
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");
		assertEquals(3, r.returnValue);

		byte[] renamed = retrieveRename(t, t + "2");
		rtype.loadNewVersion("002", renamed);
		r = runUnguarded(simpleClass, "run");
		assertEquals(4, r.returnValue);
	}

	@Ignore
	@Test
	public void lambdaWithVirtualMethodUse() throws Exception {
		// not yet written
	}

	@Ignore
	@Test
	public void altMetaFactoryUsage() throws Exception {
		// not yet written
	}

	// TODO catchers and lambda methods (non static ones)

}
