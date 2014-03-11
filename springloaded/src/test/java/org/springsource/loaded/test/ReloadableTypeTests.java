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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Modifier;

import org.junit.Test;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.SpringLoaded;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils.ReturnType;
import org.springsource.loaded.test.infra.Result;

/**
 * Tests for the TypeRegistry that exercise it in the same way it will actively be used when managing ReloadableType instances.
 * 
 * @author Andy Clement
 * @since 1.0
 */
public class ReloadableTypeTests extends SpringLoadedTests {

	/**
	 * Check the basics.
	 */
	@Test
	public void loadType() {
		TypeRegistry typeRegistry = getTypeRegistry("data.SimpleClass");
		byte[] sc = loadBytesForClass("data.SimpleClass");
		ReloadableType rtype = new ReloadableType("data.SimpleClass", sc, 1, typeRegistry, null);

		assertEquals(1, rtype.getId());
		assertEquals("data.SimpleClass", rtype.getName());
		assertEquals("data/SimpleClass", rtype.getSlashedName());
		assertNotNull(rtype.getTypeDescriptor());
		assertEquals(typeRegistry, rtype.getTypeRegistry());
	}

	/**
	 * Check calling it, reloading it and calling the new version.
	 */
	@Test
	public void callIt() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("basic.Basic");
		byte[] sc = loadBytesForClass("basic.Basic");
		ReloadableType rtype = typeRegistry.addType("basic.Basic", sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "foo");

		r = runUnguarded(simpleClass, "getValue");
		assertEquals(5, r.returnValue);

		rtype.loadNewVersion("002", retrieveRename("basic.Basic", "basic.Basic002"));

		r = runUnguarded(simpleClass, "getValue");
		assertEquals(7, r.returnValue);
	}

	@Test
	public void removingStaticMethod() throws Exception {
		String t = "remote.Perf1";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> clazz = rtype.getClazz();
		runUnguarded(clazz, "time");

		rtype.loadNewVersion("002", retrieveRename(t, "remote.Perf2"));

		runUnguarded(clazz, "time");
	}
	
	@Test
	public void protectedFieldAccessors() throws Exception {
		TypeRegistry tr = getTypeRegistry("prot.SubOne");
		ReloadableType rtype = tr.addType("prot.SubOne", loadBytesForClass("prot.SubOne"));

		Object instance = rtype.getClazz().newInstance();

		runOnInstance(rtype.getClazz(), instance, "setPublicField", 3);
		assertEquals(3, runOnInstance(rtype.getClazz(), instance, "getPublicField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedField", 3);
		assertEquals(3, runOnInstance(rtype.getClazz(), instance, "getProtectedField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedShortField", (short) 33);
		assertEquals((short) 33, runOnInstance(rtype.getClazz(), instance, "getProtectedShortField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedByteField", (byte) 133);
		assertEquals((byte) 133, runOnInstance(rtype.getClazz(), instance, "getProtectedByteField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedCharField", (char) 12);
		assertEquals((char) 12, runOnInstance(rtype.getClazz(), instance, "getProtectedCharField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedBooleanField", true);
		assertEquals(true, runOnInstance(rtype.getClazz(), instance, "isProtectedBooleanField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedDoubleField", 3.1d);
		assertEquals(3.1d, runOnInstance(rtype.getClazz(), instance, "getProtectedDoubleField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedFloatField", 8f);
		assertEquals(8f, runOnInstance(rtype.getClazz(), instance, "getProtectedFloatField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedLongField", 888L);
		assertEquals(888L, runOnInstance(rtype.getClazz(), instance, "getProtectedLongField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedArrayOfInts", new int[] { 1, 2, 3 });
		assertEquals("[1,2,3]", toString(runOnInstance(rtype.getClazz(), instance, "getProtectedArrayOfInts").returnValue));

		runOnInstance(rtype.getClazz(), instance, "setProtectedArrayOfStrings", (Object) new String[] { "a", "b", "c" });
		assertEquals("[a,b,c]", toString(runOnInstance(rtype.getClazz(), instance, "getProtectedArrayOfStrings").returnValue));

		runOnInstance(rtype.getClazz(), instance, "setProtectedArrayOfArrayOfLongs", (Object) new long[][] { new long[] { 3L },
				new long[] { 4L, 45L }, new long[] { 7L } });
		assertEquals("[[3],[4,45],[7]]",
				toString(runOnInstance(rtype.getClazz(), instance, "getProtectedArrayOfArrayOfLongs").returnValue));

		runOnInstance(rtype.getClazz(), instance, "setProtectedStaticField", 3);
		assertEquals(3, runOnInstance(rtype.getClazz(), instance, "getProtectedStaticField").returnValue);

		rtype.loadNewVersion(rtype.bytesInitial);

		runOnInstance(rtype.getClazz(), instance, "setPublicField", 4);
		assertEquals(4, runOnInstance(rtype.getClazz(), instance, "getPublicField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedField", 3);
		assertEquals(3, runOnInstance(rtype.getClazz(), instance, "getProtectedField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedShortField", (short) 33);
		assertEquals((short) 33, runOnInstance(rtype.getClazz(), instance, "getProtectedShortField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedByteField", (byte) 133);
		assertEquals((byte) 133, runOnInstance(rtype.getClazz(), instance, "getProtectedByteField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedCharField", (char) 12);
		assertEquals((char) 12, runOnInstance(rtype.getClazz(), instance, "getProtectedCharField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedBooleanField", true);
		assertEquals(true, runOnInstance(rtype.getClazz(), instance, "isProtectedBooleanField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedDoubleField", 3.1d);
		assertEquals(3.1d, runOnInstance(rtype.getClazz(), instance, "getProtectedDoubleField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedFloatField", 8f);
		assertEquals(8f, runOnInstance(rtype.getClazz(), instance, "getProtectedFloatField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedLongField", 888L);
		assertEquals(888L, runOnInstance(rtype.getClazz(), instance, "getProtectedLongField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setProtectedArrayOfInts", new int[] { 1, 2, 3 });
		assertEquals("[1,2,3]", toString(runOnInstance(rtype.getClazz(), instance, "getProtectedArrayOfInts").returnValue));

		runOnInstance(rtype.getClazz(), instance, "setProtectedArrayOfStrings", (Object) new String[] { "a", "b", "c" });
		assertEquals("[a,b,c]", toString(runOnInstance(rtype.getClazz(), instance, "getProtectedArrayOfStrings").returnValue));

		runOnInstance(rtype.getClazz(), instance, "setProtectedArrayOfArrayOfLongs", (Object) new long[][] { new long[] { 3L },
				new long[] { 4L, 45L }, new long[] { 7L } });
		assertEquals("[[3],[4,45],[7]]",
				toString(runOnInstance(rtype.getClazz(), instance, "getProtectedArrayOfArrayOfLongs").returnValue));

		runOnInstance(rtype.getClazz(), instance, "setProtectedStaticField", 3);
		assertEquals(3, runOnInstance(rtype.getClazz(), instance, "getProtectedStaticField").returnValue);

	}
	
	
	// github issue 4
	@Test
	public void invokeStaticReloading_gh4_1() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokestatic..*");
		tr.addType("invokestatic.issue4.A", loadBytesForClass("invokestatic.issue4.A"));
		ReloadableType B = tr.addType("invokestatic.issue4.B", loadBytesForClass("invokestatic.issue4.B"));
		
		Result r = runUnguarded(B.getClazz(), "getMessage");
		assertEquals("String1",(String)r.returnValue);
		
		B.loadNewVersion(B.bytesInitial);

		r = runUnguarded(B.getClazz(), "getMessage");
		assertEquals("String1",(String)r.returnValue);
	}
	
	@Test
	public void invokeStaticReloading_gh4_2() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokestatic..*");
		tr.addType("invokestatic.issue4.AA", loadBytesForClass("invokestatic.issue4.AA"));
		ReloadableType BB = tr.addType("invokestatic.issue4.BB", loadBytesForClass("invokestatic.issue4.BB"));
		
		Result r = runUnguarded(BB.getClazz(), "getMessage");
		assertEquals("String1",(String)r.returnValue);
		
		BB.loadNewVersion(BB.bytesInitial);

		r = runUnguarded(BB.getClazz(), "getMessage");
		assertEquals("String1",(String)r.returnValue);
	}
	
	@Test
	public void invokeStaticReloading_gh4_3() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokestatic..*");
		ReloadableType AAA = tr.addType("invokestatic.issue4.AAA", loadBytesForClass("invokestatic.issue4.AAA"));
		ReloadableType BBB = tr.addType("invokestatic.issue4.BBB", loadBytesForClass("invokestatic.issue4.BBB"));
		
		Result r = runUnguarded(BBB.getClazz(), "getMessage");
		assertEquals("String1",(String)r.returnValue);
		
		AAA.loadNewVersion(AAA.bytesInitial);

		r = runUnguarded(BBB.getClazz(), "getMessage");
		assertEquals("String1",(String)r.returnValue);
	}
	
	@Test
	public void invokeStaticReloading_gh4_4() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokestatic..*");
		ReloadableType A = tr.addType("invokestatic.issue4.A", loadBytesForClass("invokestatic.issue4.A"));
		ReloadableType B = tr.addType("invokestatic.issue4.B", loadBytesForClass("invokestatic.issue4.B"));
		
		Result r = runUnguarded(B.getClazz(), "getMessage");
		assertEquals("String1",(String)r.returnValue);

		A.loadNewVersion(A.bytesInitial);
		B.loadNewVersion(B.bytesInitial);

		r = runUnguarded(B.getClazz(), "getMessage");
		assertEquals("String1",(String)r.returnValue);
	}
	
	// The supertype is not reloadable,it is in a jar
	@Test
	public void invokeStaticReloading_gh4_5() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokestatic.issue4..*");
		ReloadableType B = tr.addType("invokestatic.issue4.BBBB", loadBytesForClass("invokestatic.issue4.BBBB"));
		
		Result r = runUnguarded(B.getClazz(), "getMessage");
		assertEquals("Hello",(String)r.returnValue);
		
		ReloadableType thesuper = B.getSuperRtype();
		assertNull(thesuper);
		thesuper = tr.getReloadableType("invokestatic/issue4/subpkg/AAAA");
		assertNull(thesuper);
	
		B.loadNewVersion(B.bytesInitial);

		r = runUnguarded(B.getClazz(), "getMessage");
		assertEquals("Hello",(String)r.returnValue);
	}
	
	// Basic write/read then reload then write/read again
	@Test
	public void serialization1() throws Exception {
		TypeRegistry tr = getTypeRegistry("remote..*");
		ReloadableType person = tr.addType("remote.Person", loadBytesForClass("remote.Person"));
		
		// When the Serialize class is run directly, we see: byteinfo:len=98:crc=c1047cf6
		// When run via this test, we see: byteinfo:len=98:crc=7e07276a
		// Tried running the Serialize code directly but with a clinit in the Person class: 2b4c0df4

		ReloadableType runner = tr.addType("remote.Serialize", loadBytesForClass("remote.Serialize"));
		
		Result r = null;
		r = runUnguarded(runner.getClazz(), "run");
		assertContains("check ok", r.stdout);

		person.loadNewVersion("2",retrieveRename("remote.Person", "remote.Person2"));
		
		r = runUnguarded(runner.getClazz(), "run");
		assertContains("check ok", r.stdout);		
	}
	

	// Unlike the first test, this one will reload the class in between serialize and deserialize
	@Test
	public void serialization2() throws Exception {
		TypeRegistry tr = getTypeRegistry("remote..*");
		ReloadableType person = tr.addType("remote.Person", loadBytesForClass("remote.Person"));
		
		// byteinfo:len=98:crc=7e07276a
		ReloadableType runner = tr.addType("remote.Serialize", loadBytesForClass("remote.Serialize"));
		
		Class<?> clazz = runner.getClazz();
		Object instance = clazz.newInstance();
		
		Result r = null;
		
		// Basic: write and read the same Person
		r = runOnInstance(clazz,instance,"writePerson");
		assertStdoutContains("Person stored ok", r);
		r = runOnInstance(clazz,instance,"readPerson");
		assertContains("Person read ok", r.stdout);

		// Advanced: write it, reload, then read back from the written form
		r = runOnInstance(clazz,instance,"writePerson");
		assertStdoutContains("Person stored ok", r);
		person.loadNewVersion("2",retrieveRename("remote.Person", "remote.Person2"));
		r = runOnInstance(clazz,instance,"readPerson");
		assertContains("Person read ok", r.stdout);
	}
	
	// Variant of the second test but using serialVersionUID and adding methods to the class on reload
	@Test
	public void serialization3() throws Exception {
		TypeRegistry tr = getTypeRegistry("remote..*");
		ReloadableType person = tr.addType("remote.PersonB", loadBytesForClass("remote.PersonB"));
		ReloadableType runner = tr.addType("remote.SerializeB", loadBytesForClass("remote.SerializeB"));
		
		Class<?> clazz = runner.getClazz();
		Object instance = clazz.newInstance();
		
		Result r = null;
		
		// Basic: write and read the same Person
		r = runOnInstance(runner.getClazz(),instance,"writePerson");
		assertStdoutContains("Person stored ok", r);
		r = runOnInstance(runner.getClazz(),instance,"readPerson");
		assertContains("Person read ok", r.stdout);

		// Advanced: write it, reload, then read back from the written form
		r = runOnInstance(runner.getClazz(),instance,"writePerson");
		assertStdoutContains("Person stored ok", r);
		person.loadNewVersion("2",retrieveRename("remote.PersonB", "remote.PersonB2"));
		r = runOnInstance(runner.getClazz(),instance,"readPerson");
		assertContains("Person read ok", r.stdout);
		
		r = runOnInstance(clazz,instance,"printInitials");
		assertContains("Person read ok\nWS", r.stdout);
	}
	
	// Deserialize something we serialized earlier
	// This test cannot work without the agent. The agent must intercept java.lang.ObjectStream and its use of reflection
	// There is a test that will work in the SpringLoadedTestsInSeparateJVM
	public void serialization4() throws Exception {
		TypeRegistry tr = getTypeRegistry("remote..*");
//		ReloadableType person = 
		tr.addType("remote.Person", loadBytesForClass("remote.Person"));
		
		// When the Serialize class is run directly, we see: byteinfo:len=98:crc=c1047cf6
		// When run via this test, we see: byteinfo:len=98:crc=7e07276a
		ReloadableType runner = tr.addType("remote.Serialize", loadBytesForClass("remote.Serialize"));
		Class<?> clazz = runner.getClazz();
		Object instance = clazz.newInstance();		
		Result r = runOnInstance(clazz,instance,"checkPredeserializedData");
		assertStdoutContains("Person stored ok", r);
	}

	// extra class in the middle: A in jar, subtype AB reloadable, subtype BBBBB reloadable
	@Test
	public void invokeStaticReloading_gh4_6() throws Exception {
		TypeRegistry tr = getTypeRegistry("invokestatic.issue4..*");
		tr.addType("invokestatic.issue4.AB", loadBytesForClass("invokestatic.issue4.AB"));
		ReloadableType B = tr.addType("invokestatic.issue4.BBBBB", loadBytesForClass("invokestatic.issue4.BBBBB"));
		
		Result r = runUnguarded(B.getClazz(), "getMessage");
		assertEquals("Hello",(String)r.returnValue);
		
		ReloadableType thesuper = B.getSuperRtype();
		System.out.println(thesuper);
		assertNull(thesuper);
		thesuper = tr.getReloadableType("invokestatic/issue4/subpkg/AAAA");
		assertNull(thesuper);
		
		B.loadNewVersion(B.bytesInitial);

		r = runUnguarded(B.getClazz(), "getMessage");
		assertEquals("Hello",(String)r.returnValue);
	}


	@Test
	public void protectedFieldAccessors2() throws Exception {
		TypeRegistry tr = getTypeRegistry("prot.SubTwo");
		ReloadableType rtype = tr.addType("prot.SubTwo", loadBytesForClass("prot.SubTwo"));

		Object instance = rtype.getClazz().newInstance();

		runOnInstance(rtype.getClazz(), instance, "setSomeField", 3);
		assertEquals(3, runOnInstance(rtype.getClazz(), instance, "getSomeField").returnValue);

		rtype.loadNewVersion(rtype.bytesInitial);

		runOnInstance(rtype.getClazz(), instance, "setSomeField", 3);
		assertEquals(3, runOnInstance(rtype.getClazz(), instance, "getSomeField").returnValue);
	}

	/**
	 * In this test a protected field has the same name as another field being referenced from the reloadable type. Check only the
	 * right one is redirect to the accessor.
	 * 
	 */
	@Test
	public void protectedFieldAccessors3() throws Exception {
		TypeRegistry tr = getTypeRegistry("prot.SubThree,prot.PeerThree");
//		ReloadableType rtypePeer = 
		tr.addType("prot.PeerThree", loadBytesForClass("prot.PeerThree"));
		ReloadableType rtype = tr.addType("prot.SubThree", loadBytesForClass("prot.SubThree"));

		Object instance = rtype.getClazz().newInstance();

		runOnInstance(rtype.getClazz(), instance, "setField", 3);
		assertEquals(3, runOnInstance(rtype.getClazz(), instance, "getField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setPeerField", 5);
		assertEquals(5, runOnInstance(rtype.getClazz(), instance, "getPeerField").returnValue);

		// if this returns 5, the wrong field got set in setPeerField!
		assertEquals(3, runOnInstance(rtype.getClazz(), instance, "getField").returnValue);

		rtype.loadNewVersion(rtype.bytesInitial);
		runOnInstance(rtype.getClazz(), instance, "setField", 3);
		assertEquals(3, runOnInstance(rtype.getClazz(), instance, "getField").returnValue);

		runOnInstance(rtype.getClazz(), instance, "setPeerField", 5);
		assertEquals(5, runOnInstance(rtype.getClazz(), instance, "getPeerField").returnValue);

		// if this returns 5, the wrong field got set in setPeerField!
		assertEquals(3, runOnInstance(rtype.getClazz(), instance, "getField").returnValue);
	}

	private static String toString(Object o) {
		if (o instanceof int[]) {
			int[] intArray = (int[]) o;
			StringBuilder s = new StringBuilder("[");
			for (int i = 0; i < intArray.length; i++) {
				if (i > 0)
					s.append(",");
				s.append(intArray[i]);
			}
			s.append("]");
			return s.toString();
		} else if (o instanceof long[]) {
			long[] array = (long[]) o;
			StringBuilder s = new StringBuilder("[");
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					s.append(",");
				s.append(array[i]);
			}
			s.append("]");
			return s.toString();
		} else if (o.getClass().isArray()) {
			Object[] array = (Object[]) o;
			StringBuilder s = new StringBuilder("[");
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					s.append(",");
				s.append(toString(array[i]));
			}
			s.append("]");
			return s.toString();
		} else {
			return o.toString();
		}
	}

	@Test
	public void testReturnTypeFactoryMethod() throws Exception {
		ReturnType rt = ReturnType.getReturnType("I");
		assertEquals(ReturnType.Kind.PRIMITIVE, rt.kind);
		assertEquals("I", rt.descriptor);
		assertTrue(rt.isPrimitive());
		assertFalse(rt.isDoubleSlot());
		assertFalse(rt.isVoid());

		rt = ReturnType.getReturnType("[Ljava/lang/String;");
		assertEquals(ReturnType.Kind.ARRAY, rt.kind);
		assertEquals("[Ljava/lang/String;", rt.descriptor);
		assertFalse(rt.isPrimitive());
		assertFalse(rt.isDoubleSlot());
		assertFalse(rt.isVoid());

		rt = ReturnType.getReturnType("Ljava/lang/String;");
		assertEquals(ReturnType.Kind.REFERENCE, rt.kind);
		assertEquals("java/lang/String", rt.descriptor);
		assertFalse(rt.isPrimitive());
		assertFalse(rt.isDoubleSlot());
		assertFalse(rt.isVoid());

		rt = ReturnType.getReturnType("[I");
		assertEquals(ReturnType.Kind.ARRAY, rt.kind);
		assertEquals("[I", rt.descriptor);
		assertFalse(rt.isPrimitive());
		assertFalse(rt.isDoubleSlot());
		assertFalse(rt.isVoid());
	}

	@Test
	public void preventingBadReloadsInterfaceChange() {
		boolean original = GlobalConfiguration.verifyReloads;
		try {
			GlobalConfiguration.verifyReloads = true;
			TypeRegistry tr = getTypeRegistry("baddata.One");
			ReloadableType rt = loadType(tr, "baddata.One");
			assertFalse(rt.loadNewVersion("002", retrieveRename("baddata.One", "baddata.OneA")));
		} finally {
			GlobalConfiguration.verifyReloads = original;
		}
	}

	@Test
	public void useReloadingAPI() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("basic.Basic");
		byte[] sc = loadBytesForClass("basic.Basic");
		ReloadableType rtype = typeRegistry.addType("basic.Basic", sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "foo");

		r = runUnguarded(simpleClass, "getValue");
		assertEquals(5, r.returnValue);

		int rc = SpringLoaded.loadNewVersionOfType(rtype.getClazz(), retrieveRename("basic.Basic", "basic.Basic002"));
		assertEquals(0, rc);
		assertEquals(7, runUnguarded(simpleClass, "getValue").returnValue);

		rc = SpringLoaded.loadNewVersionOfType(rtype.getClazz().getClassLoader(), rtype.dottedtypename,
				retrieveRename("basic.Basic", "basic.Basic003"));
		assertEquals(0, rc);
		assertEquals(3, runUnguarded(simpleClass, "getValue").returnValue);

		// null classloader
		rc = SpringLoaded.loadNewVersionOfType(null, rtype.dottedtypename, retrieveRename("basic.Basic", "basic.Basic003"));
		assertEquals(1, rc);

		// fake typename
		rc = SpringLoaded.loadNewVersionOfType(rtype.getClazz().getClassLoader(), "a.b.C",
				retrieveRename("basic.Basic", "basic.Basic003"));
		assertEquals(2, rc);
	}
	
	@Test
	public void innerTypesLosingStaticModifier() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("inners.Outer$Inner");
		byte[] sc = loadBytesForClass("inners.Outer$Inner");
		ReloadableType rtype = typeRegistry.addType("inners.Outer$Inner", sc);
				
		Class<?> simpleClass = rtype.getClazz();
		Result r = null;
		
		r = runUnguarded(simpleClass, "foo");
		assertEquals("foo!", r.returnValue);
		assertTrue(Modifier.isPublic((Integer)runUnguarded(simpleClass, "getModifiers").returnValue));
		assertTrue(Modifier.isStatic((Integer)runUnguarded(simpleClass, "getModifiers").returnValue));

		rtype.loadNewVersion("002", retrieveRename("inners.Outer$Inner", "inners.Outer2$Inner2"));

		r = runUnguarded(simpleClass, "foo");
		assertEquals("bar!", r.returnValue);
		assertTrue(Modifier.isPublic((Integer)runUnguarded(simpleClass, "getModifiers").returnValue));
		assertTrue(Modifier.isStatic((Integer)runUnguarded(simpleClass, "getModifiers").returnValue));
	}
}