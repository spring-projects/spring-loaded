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
import static org.junit.Assert.assertNull;

import java.lang.reflect.Modifier;

import org.junit.Test;
import org.springsource.loaded.FieldMember;
import org.springsource.loaded.MethodMember;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypeDescriptorExtractor;
import org.springsource.loaded.TypeRegistry;


/**
 * Tests for TypeDescriptor usage.
 * 
 * @author Andy Clement
 * @since 1.0
 */
public class TypeDescriptorTests extends SpringLoadedTests {

	@Test
	public void simpleMethodDescriptors() {
		TypeRegistry registry = getTypeRegistry("data.SimpleClass");
		byte[] bytes = loadBytesForClass("data.SimpleClass");
		TypeDescriptor typeDescriptor = new TypeDescriptorExtractor(registry).extract(bytes, true);
		assertEquals("data/SimpleClass", typeDescriptor.getName());
		assertEquals("java/lang/Object", typeDescriptor.getSupertypeName());
		assertEquals(0, typeDescriptor.getSuperinterfacesName().length);
		assertEquals(0x20, typeDescriptor.getModifiers());
		assertEquals(0, typeDescriptor.getFields().length);
		assertEquals(5, typeDescriptor.getMethods().length); // will include catchers
		assertEquals("0x1 foo()V", typeDescriptor.getMethods()[0].toString());
	}

	@Test
	public void complexMethodDescriptors() {
		TypeRegistry registry = getTypeRegistry("data.ComplexClass");
		byte[] bytes = loadBytesForClass("data.ComplexClass");
		TypeDescriptor typeDescriptor = new TypeDescriptorExtractor(registry).extract(bytes, true);
		assertEquals("data/ComplexClass", typeDescriptor.getName());
		assertEquals("data/SimpleClass", typeDescriptor.getSupertypeName());
		assertEquals(1, typeDescriptor.getSuperinterfacesName().length);
		assertEquals("java/io/Serializable", typeDescriptor.getSuperinterfacesName()[0]);
		assertEquals(0x20, typeDescriptor.getModifiers());
		assertEquals(3, typeDescriptor.getFields().length);
		assertEquals(9, typeDescriptor.getMethods().length);
		assertEquals("0x2 privateMethod()I", typeDescriptor.getMethods()[0].toString());
		assertEquals("0x1 publicMethod()Ljava/lang/String;", typeDescriptor.getMethods()[1].toString());
		assertEquals("0x0 defaultMethod()Ljava/util/List;", typeDescriptor.getMethods()[2].toString());
		assertEquals("0x0 thrower()V throws java/lang/Exception java/lang/IllegalStateException",
				typeDescriptor.getMethods()[3].toString());
	}

	@Test
	public void fieldDescriptors() {
		TypeRegistry registry = getTypeRegistry("");
		byte[] bytes = loadBytesForClass("data.SomeFields");
		TypeDescriptor typeDescriptor = new TypeDescriptorExtractor(registry).extract(bytes, false);
		FieldMember[] fields = typeDescriptor.getFields();
		assertEquals(4, fields.length);
		FieldMember privateField = fields[0];
		assertEquals(Modifier.PRIVATE, privateField.getModifiers());
		assertEquals("privateField", privateField.getName());
		assertEquals("I", privateField.getDescriptor());
		assertNull(privateField.getGenericSignature());
		assertEquals("0x2 I privateField", privateField.toString());

		FieldMember publicField = fields[1];
		assertEquals(Modifier.PUBLIC, publicField.getModifiers());
		assertEquals("publicField", publicField.getName());
		assertEquals("Ljava/lang/String;", publicField.getDescriptor());
		assertNull(publicField.getGenericSignature());
		assertEquals("0x1 Ljava/lang/String; publicField", publicField.toString());

		FieldMember defaultField = fields[2];
		assertEquals(0, defaultField.getModifiers());
		assertEquals("defaultField", defaultField.getName());
		assertEquals("Ljava/util/List;", defaultField.getDescriptor());
		assertEquals("Ljava/util/List<Ljava/lang/String;>;", defaultField.getGenericSignature());
		assertEquals("0x0 Ljava/util/List; defaultField [Ljava/util/List<Ljava/lang/String;>;]", defaultField.toString());

		FieldMember protectedField = fields[3];
		assertEquals(Modifier.PROTECTED, protectedField.getModifiers());
		assertEquals("protectedField", protectedField.getName());
		assertEquals("Ljava/util/Map;", protectedField.getDescriptor());
		assertEquals("Ljava/util/Map<Ljava/lang/String;Ljava/util/List<Ljava/lang/Integer;>;>;",
				protectedField.getGenericSignature());
		assertEquals(
				"0x4 Ljava/util/Map; protectedField [Ljava/util/Map<Ljava/lang/String;Ljava/util/List<Ljava/lang/Integer;>;>;]",
				protectedField.toString());
	}

	@Test
	public void constructorDescriptors() {
		TypeRegistry registry = getTypeRegistry("");
		byte[] bytes = loadBytesForClass("data.SomeConstructors");
		TypeDescriptor typeDescriptor = new TypeDescriptorExtractor(registry).extract(bytes, false);
		MethodMember[] ctors = typeDescriptor.getConstructors();
		assertEquals(3, ctors.length);

		MethodMember publicCtor = ctors[0];
		assertEquals(Modifier.PUBLIC, publicCtor.getModifiers());
		assertEquals("<init>", publicCtor.getName());
		assertEquals("()V", publicCtor.getDescriptor());
		assertNull(publicCtor.getGenericSignature());
		assertEquals("0x1 <init>()V", publicCtor.toString());

		MethodMember privateCtor = ctors[1];
		assertEquals(Modifier.PRIVATE, privateCtor.getModifiers());
		assertEquals("<init>", privateCtor.getName());
		assertEquals("(Ljava/lang/String;I)V", privateCtor.getDescriptor());
		assertNull(privateCtor.getGenericSignature());
		assertEquals("0x2 <init>(Ljava/lang/String;I)V", privateCtor.toString());

		MethodMember protCtor = ctors[2];
		assertEquals(Modifier.PROTECTED, protCtor.getModifiers());
		assertEquals("<init>", protCtor.getName());
		assertEquals("(J)V", protCtor.getDescriptor());
		assertNull(protCtor.getGenericSignature());
		assertEquals("0x4 <init>(J)V", protCtor.toString());
	}

	@Test
	public void constructorDescriptorsAfterReloading() {
		TypeRegistry registry = getTypeRegistry("");
		String d = "data.SomeConstructors";
		ReloadableType rtype = registry.addType(d, loadBytesForClass(d));
		MethodMember[] latestConstructors = rtype.getLatestTypeDescriptor().getConstructors();
		assertEquals(3, latestConstructors.length);
		rtype.loadNewVersion("2", retrieveRename(d, d + "002"));
		latestConstructors = rtype.getLatestTypeDescriptor().getConstructors();
		assertEquals(1, latestConstructors.length);
	}

	@Test
	public void defaultConstructorDescriptor() {
		TypeRegistry registry = getTypeRegistry("");
		byte[] bytes = loadBytesForClass("data.SomeConstructors2");
		TypeDescriptor typeDescriptor = new TypeDescriptorExtractor(registry).extract(bytes, false);
		MethodMember[] ctors = typeDescriptor.getConstructors();
		assertEquals(1, ctors.length);

		MethodMember publicCtor = ctors[0];
		// visibility matches type vis (for public/default)
		assertEquals(Modifier.PUBLIC, publicCtor.getModifiers());
		assertEquals("<init>", publicCtor.getName());
		assertEquals("()V", publicCtor.getDescriptor());
		assertNull(publicCtor.getGenericSignature());
		assertEquals("0x1 <init>()V", publicCtor.toString());
	}

}