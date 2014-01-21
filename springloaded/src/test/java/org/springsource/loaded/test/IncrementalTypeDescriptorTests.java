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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springsource.loaded.ClassRenamer;
import org.springsource.loaded.IncrementalTypeDescriptor;
import org.springsource.loaded.MethodMember;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypeDescriptorExtractor;
import org.springsource.loaded.TypeRegistry;


/**
 * Tests for TypeDescriptor usage.
 * 
 * @author Andy Clement
 * @since 1.0
 */
/**
 * @author Andy Clement
 * 
 */
public class IncrementalTypeDescriptorTests extends SpringLoadedTests {

	/**
	 * Test comparison of two simple classes.
	 */
	@Test
	public void simpleExtractor() {
		TypeRegistry registry = getTypeRegistry("");
		byte[] bytes = loadBytesForClass("data.SimpleClass");
		TypeDescriptor typeDescriptor = new TypeDescriptorExtractor(registry).extract(bytes, true);

		byte[] bytes2 = ClassRenamer.rename("data.SimpleClass", loadBytesForClass("data.SimpleClass002"));
		TypeDescriptor typeDescriptor2 = new TypeDescriptorExtractor(registry).extract(bytes2, true);

		IncrementalTypeDescriptor itd = new IncrementalTypeDescriptor(typeDescriptor);
		itd.setLatestTypeDescriptor(typeDescriptor2);

		List<MethodMember> newMethods = itd.getNewOrChangedMethods();
		Assert.assertEquals(1, newMethods.size());
		Assert.assertEquals("0x1 bar()Ljava/lang/String;", newMethods.get(0).toString());

		List<MethodMember> deletedMethods = itd.getDeletedMethods();
		Assert.assertEquals(1, deletedMethods.size());
		Assert.assertEquals("0x1 foo()V", deletedMethods.get(0).toString());
	}

	//	@Test
	//	public void newversionDescriptor() {
	//		byte[] classBytes = loadBytesForClass("data.SimpleClassFour");
	//		TypeDescriptor td = TypeDescriptorExtractor.extractFor(classBytes);
	//
	//		byte[] classBytes2 = retrieveRename("data.SimpleClassFour", "data.SimpleClassFour002");
	//		TypeDescriptor td2 = TypeDescriptorExtractor.extractFor(classBytes2);
	//
	//		IncrementalTypeDescriptor nvtd = new IncrementalTypeDescriptor(td);
	//		nvtd.setLatestTypeDescriptor(td2);
	//
	//		// Now ask it questions about the changes
	//		List<MethodMember> ms = nvtd.getNewOrChangedMethods();
	//		Assert.assertEquals(2, ms.size());
	//
	//		MethodMember rm = grabFrom(ms, "extraOne");
	//		Assert.assertNotNull(rm);
	//		Assert.assertEquals("0x1 extraOne(Ljava/lang/String;)V", rm.toString());
	//
	//		rm = grabFrom(ms, "extraTwo");
	//		Assert.assertNotNull(rm);
	//		Assert.assertEquals("0x9 extraTwo(I)Ljava/lang/Double;", rm.toString());
	//		//
	//		//		boolean b = nvtd.defines(false, "extraOne", "(Ljava/lang/String;)V");
	//		//		Assert.assertTrue(b);
	//		//
	//		//		b = nvtd.defines(true, "extraOne", "(Ljava/lang/String;)V");
	//		//		Assert.assertFalse(b);
	//	}

	// regular method deleted
	@Test
	public void deletedMethods() throws Exception {
		TypeRegistry registry = getTypeRegistry("");
		byte[] bytes = loadBytesForClass("typedescriptor.A");
		TypeDescriptor typeDescriptor = new TypeDescriptorExtractor(registry).extract(bytes, true);
		byte[] bytes2 = ClassRenamer.rename("typedescriptor.A", loadBytesForClass("typedescriptor.A2"));
		TypeDescriptor typeDescriptor2 = new TypeDescriptorExtractor(registry).extract(bytes2, true);
		IncrementalTypeDescriptor itd = new IncrementalTypeDescriptor(typeDescriptor);
		itd.setLatestTypeDescriptor(typeDescriptor2);
		Assert.assertEquals(1, itd.getDeletedMethods().size());
		Assert.assertEquals("0x1 m()V", itd.getDeletedMethods().get(0).toString());
	}

	// overridden (caught) method deleted
	@Test
	public void deletedMethods2() throws Exception {
		TypeRegistry registry = getTypeRegistry("");
		byte[] bytes = loadBytesForClass("typedescriptor.B");
		TypeDescriptor typeDescriptor = registry.getExtractor().extract(bytes, true);
		byte[] bytes2 = ClassRenamer.rename("typedescriptor.B", loadBytesForClass("typedescriptor.B2"));
		TypeDescriptor typeDescriptor2 = registry.getExtractor().extract(bytes2, true);
		IncrementalTypeDescriptor itd = new IncrementalTypeDescriptor(typeDescriptor);
		itd.setLatestTypeDescriptor(typeDescriptor2);
		List<MethodMember> deleted = itd.getDeletedMethods();
		System.out.println(deleted);
		Assert.assertEquals(1, deleted.size());
		Assert.assertEquals("0x1 m()V", deleted.get(0).toString());
	}

	// More subtle changes (modifier flags)
	@Test
	public void changedModifiers() throws Exception {
		TypeRegistry registry = getTypeRegistry("");
		byte[] bytes = loadBytesForClass("typedescriptor.C");
		TypeDescriptor typeDescriptor = registry.getExtractor().extract(bytes, true);
		byte[] bytes2 = ClassRenamer.rename("typedescriptor.C", loadBytesForClass("typedescriptor.C2"));
		TypeDescriptor typeDescriptor2 = registry.getExtractor().extract(bytes2, true);
		IncrementalTypeDescriptor itd = new IncrementalTypeDescriptor(typeDescriptor);
		itd.setLatestTypeDescriptor(typeDescriptor2);
		System.out.println(itd.toString(true));
		List<MethodMember> changed = itd.getNewOrChangedMethods();
		MethodMember m = getMethod(changed, "staticMethod");
		Assert.assertTrue(IncrementalTypeDescriptor.isNowNonStatic(m));
		m = getMethod(changed, "instanceMethod");
		Assert.assertTrue(IncrementalTypeDescriptor.isNowStatic(m));
		m = getMethod(changed, "publicMethod1");
		Assert.assertTrue(IncrementalTypeDescriptor.hasVisibilityChanged(m));
		// TODO Not detected as protected methods always made public in reloadable types... is that OK?
		//		m = getMethod(changed, "publicMethod2");
		//		Assert.assertTrue(IncrementalTypeDescriptor.hasVisibilityChanged(m));
		m = getMethod(changed, "publicMethod3");
		Assert.assertTrue(IncrementalTypeDescriptor.hasVisibilityChanged(m));
	}

	private MethodMember getMethod(List<MethodMember> changed, String methodName) {
		for (MethodMember m : changed) {
			if (m.getName().equals(methodName)) {
				return m;
			}
		}
		return null;
	}
}