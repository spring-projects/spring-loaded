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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.springsource.loaded.Constants;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils;
import org.springsource.loaded.Utils.ReturnType;
import org.springsource.loaded.test.infra.FakeMethodVisitor;


/**
 * Test the Util methods.
 * 
 * @author Andy Clement
 */
public class UtilsTests extends SpringLoadedTests implements Constants {

	// Test the encoding of a number to a string and the subsequent decoding
	@Test
	public void encoding() {
		// long l = 82348278L;
		Random rand = new Random(666);
		for (int r = 0; r < 2000; r++) {
			long l = Math.abs(rand.nextLong());
			String encoded = Utils.encode(l);
			// System.out.println("Encoded " + l + " to " + encoded);
			long decoded = Utils.decode(encoded);
			assertEquals(l, decoded);
		}
	}

	@Test
	public void testParamDescriptors() {
		assertEquals("(Ljava/lang/String;)", Utils.toParamDescriptor(String.class));
		assertEquals("([Ljava/lang/String;)", Utils.toParamDescriptor(String[].class));
		assertEquals("(I)", Utils.toParamDescriptor(Integer.TYPE));
		assertEquals("([I)", Utils.toParamDescriptor(int[].class));
	}

	@Test
	public void testReturnDescriptors() {
		assertEquals("java/lang/String", Utils.getReturnTypeDescriptor("(II)Ljava/lang/String;").descriptor);
		assertEquals("I", Utils.getReturnTypeDescriptor("(II)I").descriptor);
		assertEquals("[I", Utils.getReturnTypeDescriptor("(II)[I").descriptor);
		assertEquals("[Ljava/lang/String;", Utils.getReturnTypeDescriptor("(II)[Ljava/lang/String;").descriptor);
	}

	/**
	 * Check the sequence of instructions created for a cast and unbox for all primitives
	 */
	@Test
	public void testUnboxing() {
		FakeMethodVisitor fmv = new FakeMethodVisitor();
		// First variant checks passing string rather than just one char
		Utils.insertUnboxInsnsIfNecessary(fmv, "F", true);
		assertEquals(
				"visitTypeInsn(CHECKCAST,java/lang/Float) visitMethodInsn(INVOKEVIRTUAL,java/lang/Float,floatValue,()F)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'F', true);
		assertEquals(
				"visitTypeInsn(CHECKCAST,java/lang/Float) visitMethodInsn(INVOKEVIRTUAL,java/lang/Float,floatValue,()F)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'Z', true);
		assertEquals(
				"visitTypeInsn(CHECKCAST,java/lang/Boolean) visitMethodInsn(INVOKEVIRTUAL,java/lang/Boolean,booleanValue,()Z)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'S', true);
		assertEquals(
				"visitTypeInsn(CHECKCAST,java/lang/Short) visitMethodInsn(INVOKEVIRTUAL,java/lang/Short,shortValue,()S)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'J', true);
		assertEquals(
				"visitTypeInsn(CHECKCAST,java/lang/Long) visitMethodInsn(INVOKEVIRTUAL,java/lang/Long,longValue,()J)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'D', true);
		assertEquals(
				"visitTypeInsn(CHECKCAST,java/lang/Double) visitMethodInsn(INVOKEVIRTUAL,java/lang/Double,doubleValue,()D)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'C', true);
		assertEquals(
				"visitTypeInsn(CHECKCAST,java/lang/Character) visitMethodInsn(INVOKEVIRTUAL,java/lang/Character,charValue,()C)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'B', true);
		assertEquals(
				"visitTypeInsn(CHECKCAST,java/lang/Byte) visitMethodInsn(INVOKEVIRTUAL,java/lang/Byte,byteValue,()B)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'I', true);
		assertEquals(
				"visitTypeInsn(CHECKCAST,java/lang/Integer) visitMethodInsn(INVOKEVIRTUAL,java/lang/Integer,intValue,()I)",
				fmv.getEvents());
		fmv.clearEvents();

		Utils.insertUnboxInsnsIfNecessary(fmv, "Rubbish", true);
		// should be a nop as nothing to do
		assertEquals(0, fmv.getEvents().length());
		fmv.clearEvents();

		try {
			Utils.insertUnboxInsns(fmv, '[', true);
			Assert.fail("Should have blown up due to invalid primitive being passed in");
		}
		catch (IllegalArgumentException iae) {
			// success
		}
	}

	/**
	 * Check the sequence of instructions created for an unbox (no cast)
	 */
	@Test
	public void testUnboxingNoCast() {
		FakeMethodVisitor fmv = new FakeMethodVisitor();
		Utils.insertUnboxInsns(fmv, 'F', false);
		assertEquals("visitMethodInsn(INVOKEVIRTUAL,java/lang/Float,floatValue,()F)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'Z', false);
		assertEquals("visitMethodInsn(INVOKEVIRTUAL,java/lang/Boolean,booleanValue,()Z)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'S', false);
		assertEquals("visitMethodInsn(INVOKEVIRTUAL,java/lang/Short,shortValue,()S)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'J', false);
		assertEquals("visitMethodInsn(INVOKEVIRTUAL,java/lang/Long,longValue,()J)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'D', false);
		assertEquals("visitMethodInsn(INVOKEVIRTUAL,java/lang/Double,doubleValue,()D)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'C', false);
		assertEquals("visitMethodInsn(INVOKEVIRTUAL,java/lang/Character,charValue,()C)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'B', false);
		assertEquals("visitMethodInsn(INVOKEVIRTUAL,java/lang/Byte,byteValue,()B)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertUnboxInsns(fmv, 'I', false);
		assertEquals("visitMethodInsn(INVOKEVIRTUAL,java/lang/Integer,intValue,()I)", fmv.getEvents());
		fmv.clearEvents();
	}

	/**
	 * Check the sequence of instructions created for an unbox (no cast)
	 */
	@Test
	public void testBoxing() {
		FakeMethodVisitor fmv = new FakeMethodVisitor();
		Utils.insertBoxInsns(fmv, 'F');
		assertEquals("visitMethodInsn(INVOKESTATIC,java/lang/Float,valueOf,(F)Ljava/lang/Float;)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertBoxInsns(fmv, 'Z');
		assertEquals("visitMethodInsn(INVOKESTATIC,java/lang/Boolean,valueOf,(Z)Ljava/lang/Boolean;)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertBoxInsns(fmv, 'S');
		assertEquals("visitMethodInsn(INVOKESTATIC,java/lang/Short,valueOf,(S)Ljava/lang/Short;)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertBoxInsns(fmv, 'J');
		assertEquals("visitMethodInsn(INVOKESTATIC,java/lang/Long,valueOf,(J)Ljava/lang/Long;)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertBoxInsns(fmv, 'D');
		assertEquals("visitMethodInsn(INVOKESTATIC,java/lang/Double,valueOf,(D)Ljava/lang/Double;)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertBoxInsns(fmv, 'C');
		assertEquals("visitMethodInsn(INVOKESTATIC,java/lang/Character,valueOf,(C)Ljava/lang/Character;)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.insertBoxInsns(fmv, 'B');
		assertEquals("visitMethodInsn(INVOKESTATIC,java/lang/Byte,valueOf,(B)Ljava/lang/Byte;)", fmv.getEvents());
		fmv.clearEvents();
		Utils.insertBoxInsns(fmv, 'I');
		assertEquals("visitMethodInsn(INVOKESTATIC,java/lang/Integer,valueOf,(I)Ljava/lang/Integer;)", fmv.getEvents());
		fmv.clearEvents();
	}

	@Test
	public void loads() {
		FakeMethodVisitor fmv = new FakeMethodVisitor();
		Utils.createLoadsBasedOnDescriptor(fmv, "(Ljava/lang/String;)V", 0);
		assertEquals("visitVarInsn(ALOAD,0)", fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "(B)V", 0);
		assertEquals("visitVarInsn(ILOAD,0)", fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "(C)V", 0);
		assertEquals("visitVarInsn(ILOAD,0)", fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "(D)V", 0);
		assertEquals("visitVarInsn(DLOAD,0)", fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "(Z)V", 0);
		assertEquals("visitVarInsn(ILOAD,0)", fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "(J)V", 0);
		assertEquals("visitVarInsn(LLOAD,0)", fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "(F)V", 0);
		assertEquals("visitVarInsn(FLOAD,0)", fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "(S)V", 0);
		assertEquals("visitVarInsn(ILOAD,0)", fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "(I)V", 0);
		assertEquals("visitVarInsn(ILOAD,0)", fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "([[S)V", 0);
		assertEquals("visitVarInsn(ALOAD,0)", fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "([[Ljava/lang/String;)V", 0);
		assertEquals("visitVarInsn(ALOAD,0)", fmv.getEvents());
	}

	@Test
	public void loads2() {
		FakeMethodVisitor fmv = new FakeMethodVisitor();
		Utils.createLoadsBasedOnDescriptor(fmv, "(BCDS)V", 0);
		assertEquals("visitVarInsn(ILOAD,0) visitVarInsn(ILOAD,1) visitVarInsn(DLOAD,2) visitVarInsn(ILOAD,4)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "(Ljava/lang/String;J[I)V", 0);
		assertEquals("visitVarInsn(ALOAD,0) visitVarInsn(LLOAD,1) visitVarInsn(ALOAD,3)", fmv.getEvents());
		fmv.clearEvents();
		Utils.createLoadsBasedOnDescriptor(fmv, "(Ljava/lang/String;J[I)V", 4);
		assertEquals("visitVarInsn(ALOAD,4) visitVarInsn(LLOAD,5) visitVarInsn(ALOAD,7)", fmv.getEvents());
		fmv.clearEvents();
	}

	@Test
	public void generateInstructionsToUnpackArrayAccordingToDescriptor() {
		FakeMethodVisitor fmv = new FakeMethodVisitor();
		Utils.generateInstructionsToUnpackArrayAccordingToDescriptor(fmv, "(Ljava/lang/String;)V", 1);
		assertEquals(
				"visitVarInsn(ALOAD,1) visitLdcInsn(0) visitInsn(AALOAD) visitTypeInsn(CHECKCAST,java/lang/String)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.generateInstructionsToUnpackArrayAccordingToDescriptor(fmv, "(I)V", 1);
		assertEquals(
				"visitVarInsn(ALOAD,1) visitLdcInsn(0) visitInsn(AALOAD) visitTypeInsn(CHECKCAST,java/lang/Integer) visitMethodInsn(INVOKEVIRTUAL,java/lang/Integer,intValue,()I)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.generateInstructionsToUnpackArrayAccordingToDescriptor(fmv, "(Ljava/lang/String;Ljava/lang/Integer;)V", 2);
		assertEquals(
				"visitVarInsn(ALOAD,2) visitLdcInsn(0) visitInsn(AALOAD) visitTypeInsn(CHECKCAST,java/lang/String) visitVarInsn(ALOAD,2) visitLdcInsn(1) visitInsn(AALOAD) visitTypeInsn(CHECKCAST,java/lang/Integer)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.generateInstructionsToUnpackArrayAccordingToDescriptor(fmv, "([Ljava/lang/String;)V", 2);
		assertEquals(
				"visitVarInsn(ALOAD,2) visitLdcInsn(0) visitInsn(AALOAD) visitTypeInsn(CHECKCAST,[Ljava/lang/String;)",
				fmv.getEvents());
		fmv.clearEvents();
		Utils.generateInstructionsToUnpackArrayAccordingToDescriptor(fmv, "([[I)V", 2);
		assertEquals("visitVarInsn(ALOAD,2) visitLdcInsn(0) visitInsn(AALOAD) visitTypeInsn(CHECKCAST,[[I)",
				fmv.getEvents());
		fmv.clearEvents();
		try {
			Utils.generateInstructionsToUnpackArrayAccordingToDescriptor(fmv, "(Y)V", 1);
			fail();
		}
		catch (IllegalStateException ise) {
		}
	}

	/**
	 * Test the helper that adds the correct return instructions based on the descriptor in use.
	 */
	@Test
	public void testReturning() {
		FakeMethodVisitor fmv = new FakeMethodVisitor();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.ReturnTypeVoid, true);
		assertEquals("visitInsn(RETURN)", fmv.getEvents());
		fmv.clearEvents();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.ReturnTypeFloat, true);
		assertEquals("visitInsn(FRETURN)", fmv.getEvents());
		fmv.clearEvents();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.ReturnTypeBoolean, true);
		assertEquals("visitInsn(IRETURN)", fmv.getEvents());
		fmv.clearEvents();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.ReturnTypeShort, true);
		assertEquals("visitInsn(IRETURN)", fmv.getEvents());
		fmv.clearEvents();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.ReturnTypeLong, true);
		assertEquals("visitInsn(LRETURN)", fmv.getEvents());
		fmv.clearEvents();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.ReturnTypeDouble, true);
		assertEquals("visitInsn(DRETURN)", fmv.getEvents());
		fmv.clearEvents();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.ReturnTypeChar, true);
		assertEquals("visitInsn(IRETURN)", fmv.getEvents());
		fmv.clearEvents();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.ReturnTypeByte, true);
		assertEquals("visitInsn(IRETURN)", fmv.getEvents());
		fmv.clearEvents();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.ReturnTypeInt, true);
		assertEquals("visitInsn(IRETURN)", fmv.getEvents());
		fmv.clearEvents();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.getReturnType("java/lang/String", ReturnType.Kind.REFERENCE),
				true);
		assertEquals("visitTypeInsn(CHECKCAST,java/lang/String) visitInsn(ARETURN)", fmv.getEvents());
		fmv.clearEvents();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.getReturnType("[[I", ReturnType.Kind.ARRAY), true);
		assertEquals("visitTypeInsn(CHECKCAST,[[I) visitInsn(ARETURN)", fmv.getEvents());
		fmv.clearEvents();
		Utils.addCorrectReturnInstruction(fmv, ReturnType.getReturnType("[[Ljava/lang/String;", ReturnType.Kind.ARRAY),
				true);
		assertEquals("visitTypeInsn(CHECKCAST,[[Ljava/lang/String;) visitInsn(ARETURN)", fmv.getEvents());
		fmv.clearEvents();
	}

	@Test
	public void descriptorSizes() {
		assertEquals(1, Utils.getSize("(I)V"));
		assertEquals(1, Utils.getSize("(B)V"));
		assertEquals(1, Utils.getSize("(C)V"));
		assertEquals(2, Utils.getSize("(D)V"));
		assertEquals(1, Utils.getSize("(S)V"));
		assertEquals(1, Utils.getSize("(Z)V"));
		assertEquals(2, Utils.getSize("(J)V"));
		assertEquals(1, Utils.getSize("(F)V"));
		assertEquals(1, Utils.getSize("(Ljava/lang/String;)V"));
		assertEquals(1, Utils.getSize("([Ljava/lang/String;)V"));
		assertEquals(1, Utils.getSize("([D)V"));
		assertEquals(2, Utils.getSize("(II)V"));
		assertEquals(5, Utils.getSize("(BLjava/lang/String;[[JD)V"));
		try {
			assertEquals(5, Utils.getSize("(Y)V"));
			fail();
		}
		catch (IllegalStateException ise) {
		}
	}

	@Test
	public void typeDescriptorSizes() {
		assertEquals(1, Utils.sizeOf("I"));
		assertEquals(1, Utils.sizeOf("B"));
		assertEquals(1, Utils.sizeOf("C"));
		assertEquals(2, Utils.sizeOf("D"));
		assertEquals(1, Utils.sizeOf("S"));
		assertEquals(1, Utils.sizeOf("Z"));
		assertEquals(2, Utils.sizeOf("J"));
		assertEquals(1, Utils.sizeOf("F"));
		assertEquals(1, Utils.sizeOf("Lava/lang/String;"));
		assertEquals(1, Utils.sizeOf("[D"));
	}

	@Test
	public void methodDescriptorParameterCounts() {
		assertEquals(0, Utils.getParameterCount("()V"));
		assertEquals(1, Utils.getParameterCount("(I)V"));
		assertEquals(1, Utils.getParameterCount("(Ljava/lang/String;)V"));
		assertEquals(1, Utils.getParameterCount("([Ljava/lang/String;)V"));
		assertEquals(2, Utils.getParameterCount("(IZ)V"));
		assertEquals(2, Utils.getParameterCount("(Ljava/lang/String;Z)V"));
		assertEquals(3, Utils.getParameterCount("(DZ[[J)V"));
		assertEquals(3, Utils.getParameterCount("([[D[[Z[[J)V"));
	}

	@Test
	public void paramSequencing() {
		assertNull(Utils.getParamSequence("()V"));
		assertEquals("I", Utils.getParamSequence("(I)V"));
		assertEquals("B", Utils.getParamSequence("(B)V"));
		assertEquals("C", Utils.getParamSequence("(C)V"));
		assertEquals("D", Utils.getParamSequence("(D)V"));
		assertEquals("F", Utils.getParamSequence("(F)V"));
		assertEquals("Z", Utils.getParamSequence("(Z)V"));
		assertEquals("J", Utils.getParamSequence("(J)V"));
		assertEquals("S", Utils.getParamSequence("(S)V"));
		assertEquals("O", Utils.getParamSequence("(Ljava/lang/String;)V"));
		assertEquals("O", Utils.getParamSequence("([[Ljava/lang/String;)V"));
		assertEquals("O", Utils.getParamSequence("([I)V"));
	}

	@Test
	public void appendDescriptor() {
		StringBuilder temp = new StringBuilder();
		Utils.appendDescriptor(Integer.TYPE, temp = new StringBuilder());
		assertEquals("I", temp.toString());
		Utils.appendDescriptor(Byte.TYPE, temp = new StringBuilder());
		assertEquals("B", temp.toString());
		Utils.appendDescriptor(Character.TYPE, temp = new StringBuilder());
		assertEquals("C", temp.toString());
		Utils.appendDescriptor(Boolean.TYPE, temp = new StringBuilder());
		assertEquals("Z", temp.toString());
		Utils.appendDescriptor(Short.TYPE, temp = new StringBuilder());
		assertEquals("S", temp.toString());
		Utils.appendDescriptor(Float.TYPE, temp = new StringBuilder());
		assertEquals("F", temp.toString());
		Utils.appendDescriptor(Double.TYPE, temp = new StringBuilder());
		assertEquals("D", temp.toString());
		Utils.appendDescriptor(Long.TYPE, temp = new StringBuilder());
		assertEquals("J", temp.toString());
		Utils.appendDescriptor(Void.TYPE, temp = new StringBuilder());
		assertEquals("V", temp.toString());
		Utils.appendDescriptor(String.class, temp = new StringBuilder());
		assertEquals("Ljava/lang/String;", temp.toString());
		Utils.appendDescriptor(Array.newInstance(String.class, 1).getClass(), temp = new StringBuilder());
		assertEquals("[Ljava/lang/String;", temp.toString());
		Utils.appendDescriptor(Array.newInstance(Array.newInstance(Integer.TYPE, 1).getClass(), 1).getClass(),
				temp = new StringBuilder());
		assertEquals("[[I", temp.toString());
	}

	@Test
	public void toMethodDescriptor() throws Exception {
		Method toStringMethod = Object.class.getDeclaredMethod("toString");
		assertEquals("()Ljava/lang/String;", Utils.toMethodDescriptor(toStringMethod, false));
		try {
			assertEquals("()Ljava/lang/String;", Utils.toMethodDescriptor(toStringMethod, true));
			fail();
		}
		catch (IllegalStateException ise) {
		}
		Method numberOfLeadingZerosMethod = Integer.class.getDeclaredMethod("numberOfLeadingZeros", Integer.TYPE);
		assertEquals("(I)I", Utils.toMethodDescriptor(numberOfLeadingZerosMethod, false));
		// First ( is skipped, caller is expected to build the first part
		assertEquals(")I", Utils.toMethodDescriptor(numberOfLeadingZerosMethod, true));
		Method valueOfMethod = Integer.class.getDeclaredMethod("valueOf", String.class, Integer.TYPE);
		assertEquals("(Ljava/lang/String;I)Ljava/lang/Integer;", Utils.toMethodDescriptor(valueOfMethod, false));
		assertEquals("(Ljava/lang/String;I)Ljava/lang/Integer;", Utils.toMethodDescriptor(valueOfMethod));
		assertEquals("I)Ljava/lang/Integer;", Utils.toMethodDescriptor(valueOfMethod, true));

	}

	@Test
	public void isAssignableFromWithClass() throws Exception {
		TypeRegistry reg = getTypeRegistry();
		assertTrue(Utils.isAssignableFrom(reg, String.class, "java.lang.Object"));
		assertTrue(Utils.isAssignableFrom(reg, String.class, "java.io.Serializable"));
		assertTrue(Utils.isAssignableFrom(reg, HashMap.class, "java.util.Map"));
		assertTrue(Utils.isAssignableFrom(reg, String.class, "java.lang.String"));
		assertFalse(Utils.isAssignableFrom(reg, Map.class, "java.lang.String"));
	}

	@Test
	public void toPaddedNumber() {
		assertEquals("01", Utils.toPaddedNumber(1, 2));
		assertEquals("0032768", Utils.toPaddedNumber(32768, 7));
	}

	@Test
	public void isInitializer() {
		assertTrue(Utils.isInitializer("<init>"));
		assertTrue(Utils.isInitializer("<clinit>"));
		assertFalse(Utils.isInitializer("foobar"));
	}

	@Test
	public void toCombined() {
		assertEquals(1, Utils.toCombined(1, 2) >>> 16);
		assertEquals(2, Utils.toCombined(1, 2) & 0xffff);
	}

	@Test
	public void dump() {
		byte[] basicBytes = loadBytesForClass("basic.Basic");
		String s = Utils.dump("basic/Basic", basicBytes);
		//		System.out.println(s);
		File f = new File(s);
		assertTrue(f.exists());
		// tidy up
		while (f.toString().indexOf("sl_") != -1) {
			f.delete();
			//			System.out.println("deleted " + f);
			f = f.getParentFile();
		}
	}

	@Test
	public void dumpAndLoad() throws Exception {
		byte[] basicBytes = loadBytesForClass("basic.Basic");
		String s = Utils.dump("basic/Basic", basicBytes);
		//		System.out.println(s);
		File f = new File(s);
		assertTrue(f.exists());

		byte[] loadedBytes = Utils.loadFromStream(new FileInputStream(f));
		assertEquals(basicBytes.length, loadedBytes.length);
		// tidy up
		while (f.toString().indexOf("sl_") != -1) {
			f.delete();
			//			System.out.println("deleted " + f);
			f = f.getParentFile();
		}
	}

	@Test
	public void toOpcodeString() throws Exception {
		assertEquals("ACONST_NULL", Utils.toOpcodeString(ACONST_NULL));

		assertEquals("ICONST_0", Utils.toOpcodeString(ICONST_0));
		assertEquals("ICONST_1", Utils.toOpcodeString(ICONST_1));
		assertEquals("ICONST_2", Utils.toOpcodeString(ICONST_2));
		assertEquals("ICONST_3", Utils.toOpcodeString(ICONST_3));
		assertEquals("ICONST_4", Utils.toOpcodeString(ICONST_4));
		assertEquals("ICONST_5", Utils.toOpcodeString(ICONST_5));

		assertEquals("FCONST_0", Utils.toOpcodeString(FCONST_0));
		assertEquals("FCONST_1", Utils.toOpcodeString(FCONST_1));
		assertEquals("FCONST_2", Utils.toOpcodeString(FCONST_2));

		assertEquals("BIPUSH", Utils.toOpcodeString(BIPUSH));
		assertEquals("SIPUSH", Utils.toOpcodeString(SIPUSH));

		assertEquals("IALOAD", Utils.toOpcodeString(IALOAD));
		assertEquals("LALOAD", Utils.toOpcodeString(LALOAD));
		assertEquals("FALOAD", Utils.toOpcodeString(FALOAD));
		assertEquals("AALOAD", Utils.toOpcodeString(AALOAD));
		assertEquals("IASTORE", Utils.toOpcodeString(IASTORE));
		assertEquals("AASTORE", Utils.toOpcodeString(AASTORE));

		assertEquals("BASTORE", Utils.toOpcodeString(BASTORE));
		assertEquals("POP", Utils.toOpcodeString(POP));
		assertEquals("POP2", Utils.toOpcodeString(POP2));
		assertEquals("DUP", Utils.toOpcodeString(DUP));
		assertEquals("DUP_X1", Utils.toOpcodeString(DUP_X1));
		assertEquals("DUP_X2", Utils.toOpcodeString(DUP_X2));
		assertEquals("DUP2", Utils.toOpcodeString(DUP2));
		assertEquals("DUP2_X1", Utils.toOpcodeString(DUP2_X1));
		assertEquals("DUP2_X2", Utils.toOpcodeString(DUP2_X2));
		assertEquals("IADD", Utils.toOpcodeString(IADD));
		assertEquals("LMUL", Utils.toOpcodeString(LMUL));
		assertEquals("FMUL", Utils.toOpcodeString(FMUL));
		assertEquals("DMUL", Utils.toOpcodeString(DMUL));
		assertEquals("I2D", Utils.toOpcodeString(I2D));
		assertEquals("L2F", Utils.toOpcodeString(L2F));
		assertEquals("I2C", Utils.toOpcodeString(I2C));
		assertEquals("I2S", Utils.toOpcodeString(I2S));
		assertEquals("IFNE", Utils.toOpcodeString(IFNE));
		assertEquals("IFLT", Utils.toOpcodeString(IFLT));
		assertEquals("IFGE", Utils.toOpcodeString(IFGE));
		assertEquals("IFGT", Utils.toOpcodeString(IFGT));
		assertEquals("IFLE", Utils.toOpcodeString(IFLE));
		assertEquals("IFLE", Utils.toOpcodeString(IFLE));
		assertEquals("IF_ICMPEQ", Utils.toOpcodeString(IF_ICMPEQ));
		assertEquals("IF_ICMPNE", Utils.toOpcodeString(IF_ICMPNE));
		assertEquals("IF_ICMPLT", Utils.toOpcodeString(IF_ICMPLT));
		assertEquals("IF_ICMPGE", Utils.toOpcodeString(IF_ICMPGE));
		assertEquals("IF_ICMPGT", Utils.toOpcodeString(IF_ICMPGT));
		assertEquals("IF_ICMPLE", Utils.toOpcodeString(IF_ICMPLE));
		assertEquals("IF_ACMPEQ", Utils.toOpcodeString(IF_ACMPEQ));
		assertEquals("IF_ACMPNE", Utils.toOpcodeString(IF_ACMPNE));
		assertEquals("INVOKESPECIAL", Utils.toOpcodeString(INVOKESPECIAL));
		assertEquals("INVOKESTATIC", Utils.toOpcodeString(INVOKESTATIC));
		assertEquals("INVOKEINTERFACE", Utils.toOpcodeString(INVOKEINTERFACE));
		assertEquals("NEWARRAY", Utils.toOpcodeString(NEWARRAY));
		assertEquals("ANEWARRAY", Utils.toOpcodeString(ANEWARRAY));
		assertEquals("ARRAYLENGTH", Utils.toOpcodeString(ARRAYLENGTH));
		assertEquals("IFNONNULL", Utils.toOpcodeString(IFNONNULL));
	}

	@Test
	public void getDispatcherName() throws Exception {
		assertEquals("A$$D123", Utils.getDispatcherName("A", "123"));
	}

	@Test
	public void toResultCheckIfNull() throws Exception {
		assertEquals(1, Utils.toResultCheckIfNull(1, "I"));
		assertEquals(new Integer(1), Utils.toResultCheckIfNull(1, "Ljava/lang/Integer;"));
		assertEquals(0, Utils.toResultCheckIfNull(null, "I"));
		assertEquals((byte) 0, Utils.toResultCheckIfNull(null, "B"));
		assertEquals((char) 0, Utils.toResultCheckIfNull(null, "C"));
		assertEquals((short) 0, Utils.toResultCheckIfNull(null, "S"));
		assertEquals((long) 0, Utils.toResultCheckIfNull(null, "J"));
		assertEquals(0f, Utils.toResultCheckIfNull(null, "F"));
		assertEquals(0d, Utils.toResultCheckIfNull(null, "D"));
		assertEquals(false, Utils.toResultCheckIfNull(null, "Z"));
		assertNull(Utils.toResultCheckIfNull(null, "Ljava/lang/String;"));
		try {
			assertEquals((long) 0, Utils.toResultCheckIfNull(null, "L"));
			fail();
		}
		catch (IllegalStateException ise) {
			// success
		}

		//		public static final Integer DEFAULT_INT = Integer.valueOf(0);
		//		public static final Byte DEFAULT_BYTE = Byte.valueOf((byte) 0);
		//		public static final Character DEFAULT_CHAR = Character.valueOf((char) 0);
		//		public static final Short DEFAULT_SHORT = Short.valueOf((short) 0);
		//		public static final Long DEFAULT_LONG = Long.valueOf(0);
		//		public static final Float DEFAULT_FLOAT = Float.valueOf(0);
		//		public static final Double DEFAULT_DOUBLE = Double.valueOf(0);
	}

	@Test
	public void isObjectUnboxableTo() throws Exception {
		assertFalse(Utils.isObjectIsUnboxableTo(String.class, 'I'));
		assertTrue(Utils.isObjectIsUnboxableTo(Integer.class, 'I'));
		assertFalse(Utils.isObjectIsUnboxableTo(String.class, 'S'));
		assertTrue(Utils.isObjectIsUnboxableTo(Short.class, 'S'));
		assertFalse(Utils.isObjectIsUnboxableTo(String.class, 'J'));
		assertTrue(Utils.isObjectIsUnboxableTo(Long.class, 'J'));
		assertFalse(Utils.isObjectIsUnboxableTo(String.class, 'F'));
		assertTrue(Utils.isObjectIsUnboxableTo(Float.class, 'F'));
		assertFalse(Utils.isObjectIsUnboxableTo(String.class, 'D'));
		assertTrue(Utils.isObjectIsUnboxableTo(Double.class, 'D'));
		assertFalse(Utils.isObjectIsUnboxableTo(String.class, 'B'));
		assertTrue(Utils.isObjectIsUnboxableTo(Byte.class, 'B'));
		assertFalse(Utils.isObjectIsUnboxableTo(String.class, 'C'));
		assertTrue(Utils.isObjectIsUnboxableTo(Character.class, 'C'));
		assertFalse(Utils.isObjectIsUnboxableTo(String.class, 'Z'));
		assertTrue(Utils.isObjectIsUnboxableTo(Boolean.class, 'Z'));
		try {
			assertTrue(Utils.isObjectIsUnboxableTo(Boolean.class, 'V'));
			fail("Should not know about 'V'");
		}
		catch (IllegalStateException ise) {
			// success
		}
	}

	@Test
	public void getExecutorName() throws Exception {
		assertEquals("A$$E123", Utils.getExecutorName("A", "123"));
	}

	@Test
	public void stripFirstParameter() throws Exception {
		assertEquals("(Ljava/lang/Object;)V", Utils.stripFirstParameter("(Ljava/lang/String;Ljava/lang/Object;)V"));
		if (GlobalConfiguration.assertsMode) {
			try {
				Utils.stripFirstParameter("()V");
				fail();
			}
			catch (IllegalStateException ise) {
				// success
			}
		}
	}

	@Test
	public void getReturnType() throws Exception {
		assertEquals(ReturnType.ReturnTypeVoid, Utils.ReturnType.getReturnType("V()", ReturnType.Kind.PRIMITIVE));
		assertEquals(ReturnType.ReturnTypeFloat, Utils.ReturnType.getReturnType("F()", ReturnType.Kind.PRIMITIVE));
		assertEquals(ReturnType.ReturnTypeBoolean, Utils.ReturnType.getReturnType("Z()", ReturnType.Kind.PRIMITIVE));
		assertEquals(ReturnType.ReturnTypeShort, Utils.ReturnType.getReturnType("S()", ReturnType.Kind.PRIMITIVE));
		assertEquals(ReturnType.ReturnTypeInt, Utils.ReturnType.getReturnType("I()", ReturnType.Kind.PRIMITIVE));
		assertEquals(ReturnType.ReturnTypeByte, Utils.ReturnType.getReturnType("B()", ReturnType.Kind.PRIMITIVE));
		assertEquals(ReturnType.ReturnTypeChar, Utils.ReturnType.getReturnType("C()", ReturnType.Kind.PRIMITIVE));
		assertEquals(ReturnType.ReturnTypeLong, Utils.ReturnType.getReturnType("J()", ReturnType.Kind.PRIMITIVE));
		assertEquals(ReturnType.ReturnTypeDouble, Utils.ReturnType.getReturnType("D()", ReturnType.Kind.PRIMITIVE));
	}

	@Test
	public void toSuperAccessor() throws Exception {
		assertEquals("__super$FooType$Foomethod", Utils.toSuperAccessor("a/b/c/FooType", "Foomethod"));
	}

	@Test
	public void isConvertableFrom() throws Exception {
		assertTrue(Utils.isConvertableFrom(short.class, byte.class));
		assertTrue(Utils.isConvertableFrom(int.class, byte.class));
		assertTrue(Utils.isConvertableFrom(long.class, byte.class));
		assertTrue(Utils.isConvertableFrom(float.class, byte.class));
		assertTrue(Utils.isConvertableFrom(double.class, byte.class));

		assertTrue(Utils.isConvertableFrom(short.class, short.class));
		assertTrue(Utils.isConvertableFrom(int.class, short.class));
		assertTrue(Utils.isConvertableFrom(long.class, short.class));
		assertTrue(Utils.isConvertableFrom(float.class, short.class));
		assertTrue(Utils.isConvertableFrom(double.class, short.class));

		assertTrue(Utils.isConvertableFrom(int.class, char.class));
		assertTrue(Utils.isConvertableFrom(long.class, char.class));
		assertTrue(Utils.isConvertableFrom(float.class, char.class));
		assertTrue(Utils.isConvertableFrom(double.class, char.class));

		assertTrue(Utils.isConvertableFrom(long.class, int.class));
		assertTrue(Utils.isConvertableFrom(float.class, int.class));
		assertTrue(Utils.isConvertableFrom(double.class, int.class));
		assertFalse(Utils.isConvertableFrom(byte.class, int.class));

		assertTrue(Utils.isConvertableFrom(float.class, long.class));
		assertTrue(Utils.isConvertableFrom(double.class, long.class));

		assertTrue(Utils.isConvertableFrom(double.class, float.class));

		assertTrue(Utils.isConvertableFrom(String.class, String.class));
		assertFalse(Utils.isConvertableFrom(Integer.class, String.class));
	}
}
