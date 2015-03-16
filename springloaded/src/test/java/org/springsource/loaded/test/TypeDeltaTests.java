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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Modifier;

import org.junit.Test;
import org.springsource.loaded.TypeDelta;
import org.springsource.loaded.TypeDiffComputer;
import org.springsource.loaded.Utils;


/**
 * Tests for TypeDeltas which tell us about the differences between two class objects.
 * 
 * @author Andy Clement
 * @since 1.0
 */
public class TypeDeltaTests extends SpringLoadedTests {

	@Test
	public void typesAreTheSame() {
		byte[] bytes = loadBytesForClass("differs.DiffOne");
		TypeDelta td = TypeDiffComputer.computeDifferences(bytes, bytes);
		assertFalse(td.hasAnythingChanged());
	}

	@Test
	public void basicTypeLevelChanges() {
		byte[] bytes = loadBytesForClass("differs.DiffOne");
		byte[] bytes2 = retrieveRename("differs.DiffOne", "differs.DiffOneX");
		TypeDelta td = TypeDiffComputer.computeDifferences(bytes, bytes2);
		assertTrue(td.hasAnythingChanged());
		assertTrue(td.hasTypeDeclarationChanged());
		assertTrue(td.hasTypeAccessChanged());
		assertTrue(Modifier.isPublic(td.oAccess));
		assertTrue(!Modifier.isPublic(td.nAccess));
	}

	@Test
	public void basicTypeLevelChanges2() {
		byte[] bytes = loadBytesForClass("differs.DiffOne");
		byte[] bytes2 = retrieveRename("differs.DiffOne", "differs.DiffOneY");
		TypeDelta td = TypeDiffComputer.computeDifferences(bytes, bytes2);
		assertTrue(td.hasAnythingChanged());
		assertTrue(td.hasTypeDeclarationChanged());
		assertTrue(td.hasTypeInterfacesChanged());
		assertEquals(0, td.oInterfaces.size());
		assertEquals("java/io/Serializable", td.nInterfaces.get(0));
	}

	@Test
	public void basicTypeLevelChanges3() {
		byte[] bytes = loadBytesForClass("differs.DiffThree");
		byte[] bytes2 = retrieveRename("differs.DiffThreeZ", "differs.DiffThreeX");
		TypeDelta td = TypeDiffComputer.computeDifferences(bytes, bytes2);
		assertTrue(td.hasAnythingChanged());
		assertTrue(td.hasTypeNameChanged());
		assertTrue(td.hasTypeSupertypeChanged());
		assertTrue(td.hasTypeInterfacesChanged());
		assertEquals(1, td.nInterfaces.size());
		assertFalse(td.hasTypeAccessChanged());
		assertFalse(td.haveFieldsChanged());
		assertFalse(td.haveFieldsChangedOrBeenAddedOrRemoved());
		assertFalse(td.hasTypeSignatureChanged());
		// As of May-2011 generic signature change is not a change
		//		byte[] bytes3 = retrieveRename("differs.DiffThreeYY", "differs.DiffThreeY");
		//		td = TypeDiffComputer.computeDifferences(bytes, bytes3);
		//		assertTrue(td.hasTypeSignatureChanged());
	}

	@Test
	public void addedAField() {
		byte[] bytes = loadBytesForClass("differs.DiffOne");
		byte[] bytes2 = retrieveRename("differs.DiffOne", "differs.DiffOneZ");
		TypeDelta td = TypeDiffComputer.computeDifferences(bytes, bytes2);
		assertTrue(td.hasAnythingChanged());
		assertFalse(td.hasTypeNameChanged());
		assertFalse(td.hasTypeSupertypeChanged());
		assertFalse(td.hasTypeInterfacesChanged());
		assertFalse(td.hasTypeDeclarationChanged());
		assertTrue(td.haveFieldsChangedOrBeenAddedOrRemoved());
		assertTrue(td.hasNewFields());
		assertEquals(1, td.getNewFields().size());
		assertEquals("public I newIntField", Utils.fieldNodeFormat(td.getNewFields().get("newIntField")));
	}

	@Test
	public void removedAField() {
		byte[] bytes = loadBytesForClass("differs.DiffTwo");
		byte[] bytes2 = retrieveRename("differs.DiffTwo", "differs.DiffTwoX");
		TypeDelta td = TypeDiffComputer.computeDifferences(bytes, bytes2);
		assertTrue(td.hasAnythingChanged());
		assertFalse(td.hasTypeDeclarationChanged());
		assertTrue(td.haveFieldsChangedOrBeenAddedOrRemoved());
		assertTrue(td.hasLostFields());
		assertEquals(1, td.getLostFields().size());
		assertEquals("public I anIntField", Utils.fieldNodeFormat(td.getLostFields().get("anIntField")));
	}

	@Test
	public void changedAFieldType() {
		byte[] bytes = loadBytesForClass("differs.DiffTwo");
		byte[] bytes2 = retrieveRename("differs.DiffTwo", "differs.DiffTwoY");
		TypeDelta td = TypeDiffComputer.computeDifferences(bytes, bytes2);
		assertTrue(td.hasAnythingChanged());
		assertFalse(td.hasTypeDeclarationChanged());
		assertTrue(td.haveFieldsChangedOrBeenAddedOrRemoved());
		assertFalse(td.hasLostFields());
		assertFalse(td.hasNewFields());
		assertTrue(td.haveFieldsChanged());
		assertEquals(1, td.getChangedFields().size());
		assertEquals("FieldDelta[field:anIntField type:I>Ljava/lang/String;]",
				td.getChangedFields().get("anIntField").toString());
	}

	@Test
	public void changedAFieldAccess() {
		byte[] bytes = loadBytesForClass("differs.DiffTwo");
		byte[] bytes2 = retrieveRename("differs.DiffTwo", "differs.DiffTwoZ");
		TypeDelta td = TypeDiffComputer.computeDifferences(bytes, bytes2);
		assertTrue(td.hasAnythingChanged());
		assertFalse(td.hasTypeDeclarationChanged());
		assertTrue(td.haveFieldsChangedOrBeenAddedOrRemoved());
		assertFalse(td.hasLostFields());
		assertFalse(td.hasNewFields());
		assertTrue(td.haveFieldsChanged());
		assertEquals(1, td.getChangedFields().size());
		assertEquals("FieldDelta[field:anIntField access:1>2]", td.getChangedFields().get("anIntField").toString());
	}

	@Test
	public void changedFieldAnnotations() {
		byte[] bytes = loadBytesForClass("differs.AnnotFields");
		byte[] bytes2 = retrieveRename("differs.AnnotFields", "differs.AnnotFields2");
		TypeDelta td = TypeDiffComputer.computeDifferences(bytes, bytes2);
		assertTrue(td.hasAnythingChanged());
		assertFalse(td.hasTypeDeclarationChanged());
		assertTrue(td.haveFieldsChangedOrBeenAddedOrRemoved());
		assertFalse(td.hasLostFields());
		assertFalse(td.hasNewFields());
		assertTrue(td.haveFieldsChanged());
		assertEquals(1, td.getChangedFields().size());
		assertEquals("FieldDelta[field:i annotations:-differs/Annot]", td.getChangedFields().get("i").toString());
	}

	@Test
	public void changedFieldAnnotationValues() {
		byte[] bytes = loadBytesForClass("differs.AnnotFieldsTwo");
		byte[] bytes2 = retrieveRename("differs.AnnotFieldsTwo", "differs.AnnotFieldsTwo2");
		TypeDelta td = TypeDiffComputer.computeDifferences(bytes, bytes2);
		assertTrue(td.hasAnythingChanged());
		assertFalse(td.hasTypeDeclarationChanged());
		assertTrue(td.haveFieldsChangedOrBeenAddedOrRemoved());
		assertFalse(td.hasLostFields());
		assertFalse(td.hasNewFields());
		assertTrue(td.haveFieldsChanged());
		assertEquals(1, td.getChangedFields().size());
		assertEquals("FieldDelta[field:i annotations:-differs/Annot2(id=xyz)+differs/Annot2(id=xyz,value=24)]", td
				.getChangedFields().get("i").toString());
	}

}
