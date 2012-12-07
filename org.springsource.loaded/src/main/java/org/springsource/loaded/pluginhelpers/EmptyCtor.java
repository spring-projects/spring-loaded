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
package org.springsource.loaded.pluginhelpers;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.springsource.loaded.Constants;
import org.springsource.loaded.test.infra.ClassPrinter;


/**
 * Modifies a class and empties the specified constructors (not a common thing to do!)
 * 
 * @author Andy Clement
 * @since 0.8.3
 */
public class EmptyCtor extends ClassAdapter implements Constants {

	private String[] descriptors;

	/**
	 * Empty the constructors with the specified descriptors.
	 * 
	 * @param bytesIn input class as bytes
	 * @param descriptors descriptors of interest (e.g. "()V")
	 * @return modified class as byte array
	 */
	public static byte[] invoke(byte[] bytesIn, String... descriptors) {
		ClassReader cr = new ClassReader(bytesIn);
		EmptyCtor ca = new EmptyCtor(descriptors);
		cr.accept(ca, 0);
		byte[] newbytes = ca.getBytes();
		return newbytes;
	}

	private EmptyCtor(String... descriptors) {
		super(new ClassWriter(0)); // TODO review 0 here
		this.descriptors = descriptors;
	}

	public byte[] getBytes() {
		byte[] bs = ((ClassWriter) cv).toByteArray();
		ClassPrinter.print(bs);
		return bs;
	}

	private boolean isInterestingDescriptor(String desc) {
		for (int i = 0, max = descriptors.length; i < max; i++) {
			if (descriptors[i].equals(desc)) {
				return true;
			}
		}
		return false;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (name.equals("<init>") && isInterestingDescriptor(desc)) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			return new Emptier(mv);
		} else {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
	}

	static class Emptier implements MethodVisitor, Constants {

		MethodVisitor mv;

		public Emptier(MethodVisitor mv) {
			this.mv = mv;
		}

		public AnnotationVisitor visitAnnotationDefault() {
			return null;
		}

		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			return null;
		}

		public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
			return null;
		}

		public void visitAttribute(Attribute attr) {
		}

		public void visitCode() {
		}

		public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		}

		public void visitInsn(int opcode) {
		}

		public void visitIntInsn(int opcode, int operand) {
		}

		public void visitVarInsn(int opcode, int var) {
		}

		public void visitTypeInsn(int opcode, String type) {
		}

		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		}

		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		}

		public void visitJumpInsn(int opcode, Label label) {
		}

		public void visitLabel(Label label) {
		}

		public void visitLdcInsn(Object cst) {
		}

		public void visitIincInsn(int var, int increment) {
		}

		public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
		}

		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		}

		public void visitMultiANewArrayInsn(String desc, int dims) {
		}

		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		}

		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		}

		public void visitLineNumber(int line, Label start) {
		}

		public void visitMaxs(int maxStack, int maxLocals) {
			mv.visitMaxs(1, 1); // TODO adjust visit max numbers based on descriptor length
		}

		public void visitEnd() {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
			mv.visitInsn(RETURN);
		}

	}

}