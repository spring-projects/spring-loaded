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
package org.springsource.loaded.test.infra;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.springsource.loaded.Constants;
import org.springsource.loaded.Utils;


/**
 * MethodVisitor that records events - very useful for testing
 */
public class FakeMethodVisitor extends MethodVisitor implements Constants {

	public FakeMethodVisitor() {
		super(ASM5);
	}

	StringBuilder events = new StringBuilder();

	public String getEvents() {
		return events.toString().trim();
	}

	public void clearEvents() {
		events = new StringBuilder();
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return null;
	}

	public AnnotationVisitor visitAnnotationDefault() {
		return null;
	}

	public void visitAttribute(Attribute attr) {

	}

	public void visitCode() {
	}

	public void visitEnd() {
	}

	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
	}

	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
	}

	public void visitIincInsn(int var, int increment) {
	}

	public void visitInsn(int opcode) {
		events.append("visitInsn(" + Utils.toOpcodeString(opcode) + ") ");
	}

	public void visitIntInsn(int opcode, int operand) {
	}

	public void visitJumpInsn(int opcode, Label label) {
	}

	public void visitLabel(Label label) {
	}

	public void visitLdcInsn(Object cst) {
		events.append("visitLdcInsn(" + cst + ") ");
	}

	public void visitLineNumber(int line, Label start) {
	}

	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
	}

	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
	}

	public void visitMaxs(int maxStack, int maxLocals) {
	}

	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		events.append("visitMethodInsn(" + Utils.toOpcodeString(opcode) + "," + owner + "," + name + "," + desc + ") ");
	}

	public void visitMultiANewArrayInsn(String desc, int dims) {
	}

	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
		return null;
	}

	public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
	}

	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {

	}

	public void visitTypeInsn(int opcode, String type) {
		events.append("visitTypeInsn(" + Utils.toOpcodeString(opcode) + "," + type + ") ");
	}

	public void visitVarInsn(int opcode, int var) {
		events.append("visitVarInsn(" + Utils.toOpcodeString(opcode) + "," + var + ") ");
	}

}