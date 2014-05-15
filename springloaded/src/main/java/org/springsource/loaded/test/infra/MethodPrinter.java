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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.springsource.loaded.Utils;

/**
 * 
 * @author Andy Clement
 */
public class MethodPrinter extends MethodVisitor implements Opcodes {

	PrintStream to;

	List<Label> labels = new ArrayList<Label>();

	private String toString(Label label) {
		int idx = labels.indexOf(label);
		if (idx != -1) {
			return "L" + idx;
		}
		labels.add(label);
		return "L" + labels.indexOf(label);
	}

	public MethodPrinter(PrintStream destination) {
		super(ASM5);
		this.to = destination;
	}

	public void visitCode() {
		to.print("    CODE\n");
	}
	
	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		to.println("    INVOKEDYNAMIC " + name+"."+desc+"  bsm="+toString(bsm));
	}

	private String toString(Handle bsm) {
		return "#"+bsm.getTag()+" "+bsm.getOwner()+"."+bsm.getName()+bsm.getDesc();
	}

	// TODO include 'itf' flag in output (maybe only if true)
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		if (opcode == Opcodes.INVOKESTATIC) {
			to.println("    INVOKESTATIC " + owner + "." + name + desc);
		} else if (opcode == Opcodes.INVOKESPECIAL) {
			to.println("    INVOKESPECIAL " + owner + "." + name + desc);
		} else if (opcode == Opcodes.INVOKEVIRTUAL) {
			to.println("    INVOKEVIRTUAL " + owner + "." + name + desc);
		} else if (opcode == Opcodes.INVOKEINTERFACE) {
			to.println("    INVOKEINTERFACE " + owner + "." + name + desc);
		} else {
			throw new IllegalStateException(":" + opcode);
		}
	}

	// -- pas de implemented

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		to.print("ANNOTATION " + desc + " vis?" + visible + " VALUE ");
		return new AnnotationVisitorPrinter();
	}

	class AnnotationVisitorPrinter extends AnnotationVisitor {

		public AnnotationVisitorPrinter() {
			super(ASM5);
		}

		public void visit(String name, Object value) {
			to.print(name + "=" + value + " ");
		}

		public void visitEnum(String name, String desc, String value) {
			to.print(name + "=" + desc + "." + value + " ");
		}

		public AnnotationVisitor visitAnnotation(String name, String desc) {
			to.print(name + "=" + desc + " ");
			return new AnnotationVisitorPrinter();
		}

		public AnnotationVisitor visitArray(String name) {
			to.print(name + " ");
			return new AnnotationVisitorPrinter();
		}

		public void visitEnd() {
			to.println();
		}

	}

	public AnnotationVisitor visitAnnotationDefault() {
		return null;
	}

	public void visitAttribute(Attribute attr) {
	}

	public void visitEnd() {
	}

	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (opcode == Opcodes.GETSTATIC) {
			to.println("    GETSTATIC " + owner + "." + name + " " + desc);
		} else if (opcode == Opcodes.PUTSTATIC) {
			to.println("    PUTSTATIC " + owner + "." + name + " " + desc);
		} else if (opcode == Opcodes.GETFIELD) {
			to.println("    GETFIELD " + owner + "." + name + " " + desc);
		} else if (opcode == Opcodes.PUTFIELD) {
			to.println("    PUTFIELD " + owner + "." + name + " " + desc);
		} else {
			throw new IllegalStateException(":" + opcode);
		}
	}

	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
	}

	public void visitIincInsn(int var, int increment) {
	}

	public void visitInsn(int opcode) {
		to.println("    " + Utils.toOpcodeString(opcode));
	}

	public void visitIntInsn(int opcode, int operand) {
		to.println("    " + Utils.toOpcodeString(opcode) + " " + operand);
	}

	public void visitJumpInsn(int opcode, Label label) {
		to.println("    " + Utils.toOpcodeString(opcode) + " " + toString(label));
	}

	public void visitLabel(Label label) {
		to.println(" " + toString(label));
	}

	public void visitLdcInsn(Object cst) {
		to.println("    LDC " + cst);
	}

	public void visitLineNumber(int line, Label start) {
	}

	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
	}

	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
	}

	public void visitMaxs(int maxStack, int maxLocals) {
	}

	public void visitMultiANewArrayInsn(String desc, int dims) {
	}

	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
		return null;
	}

	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
	}

	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
	}

	public void visitTypeInsn(int opcode, String type) {
		if (opcode == Opcodes.NEW) { // 187
			to.println("    NEW " + type);
		} else if (opcode == Opcodes.ANEWARRAY) { // 189
			to.println("    ANEWARRAY " + type);
		} else if (opcode == Opcodes.CHECKCAST) { // 192
			to.println("    CHECKCAST " + type);
		} else if (opcode == Opcodes.INSTANCEOF) { // 193
			to.println("    INSTANCEOF " + type);
		} else {
			throw new IllegalStateException(":" + opcode);
		}
	}

	public void visitVarInsn(int opcode, int var) {
		if (opcode == Opcodes.ALOAD) {
			to.println("    ALOAD " + var);
		} else if (opcode == Opcodes.ASTORE) {
			to.println("    ASTORE " + var);
		} else if (opcode == Opcodes.ILOAD) {
			to.println("    ILOAD " + var);
		} else if (opcode == FLOAD) {
			to.println("    FLOAD " + var);
		} else if (opcode == LLOAD) {
			to.println("    LLOAD " + var);
		} else if (opcode == DLOAD) {
			to.println("    DLOAD " + var);
		} else if (opcode == ISTORE) {
			to.println("    ISTORE " + var);
		} else if (opcode == LSTORE) {
			to.println("    LSTORE " + var);
		} else {
			throw new IllegalStateException(":" + opcode);
		}
	}

}