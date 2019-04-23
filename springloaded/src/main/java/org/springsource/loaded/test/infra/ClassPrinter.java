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

package org.springsource.loaded.test.infra;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.springsource.loaded.Utils;


/**
 * @author Andy Clement
 */
public class ClassPrinter extends ClassVisitor implements Opcodes {

	private PrintStream destination;

	private boolean includeBytecode;

	private int includeFlags = 0x0000;

	public final static int INCLUDE_BYTECODE = 0x0001;

	public final static int INCLUDE_LINE_NUMBERS = 0x0002;

	public static void main(String[] argv) throws Exception {
		ClassReader reader = new ClassReader(Utils.loadBytesFromStream(new FileInputStream(new File(argv[0]))));
		reader.accept(new ClassPrinter(System.out, INCLUDE_BYTECODE), 0);
	}

	public ClassPrinter(PrintStream destination) {
		this(destination, INCLUDE_BYTECODE);
	}

	public ClassPrinter(PrintStream destination, int includeFlags) {
		super(ASM5);
		this.destination = destination;
		this.includeFlags = includeFlags;
	}

	public static void print(String message, byte[] bytes) {
		System.out.println(message);
		print(bytes, true);
	}

	public static void print(byte[] bytes) {
		print(bytes, true);
	}

	public static void print(byte[] bytes, int includeFlags) {
		ClassReader reader = new ClassReader(bytes);
		reader.accept(new ClassPrinter(System.out, includeFlags), 0);
	}

	public static void print(byte[] bytes, boolean includeBytecode) {
		ClassReader reader = new ClassReader(bytes);
		reader.accept(new ClassPrinter(System.out, includeBytecode ? INCLUDE_BYTECODE : 0), 0);
	}

	public static void print(PrintStream printStream, byte[] bytes, boolean includeBytecode) {
		ClassReader reader = new ClassReader(bytes);
		reader.accept(new ClassPrinter(printStream, includeBytecode ? INCLUDE_BYTECODE : 0), 0);
	}

	public static void print(String message, byte[] bytes, boolean includeBytecode) {
		System.out.println(message);
		print(bytes, includeBytecode);
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		destination.println("CLASS: " + name + " v" + Integer.toString(version) + " " + toHex(access, 4) + "("
				+ toAccessForClass(access) + ") super " + superName
				+ (interfaces == null || interfaces.length == 0 ? "" : " interfaces" + toString(interfaces)));
	}

	private String toString(Object[] os) {
		if (os == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Object o : os) {
			sb.append(o).append(" ");
		}
		return sb.toString();
	}

	private String toAccessForClass(int flags) {
		StringBuilder sb = new StringBuilder();
		if ((flags & Opcodes.ACC_PUBLIC) != 0) {
			sb.append("public ");
		}
		if ((flags & Opcodes.ACC_PRIVATE) != 0) {
			sb.append("private ");
		}
		if ((flags & Opcodes.ACC_PROTECTED) != 0) {
			sb.append("protected ");
		}
		if ((flags & Opcodes.ACC_STATIC) != 0) {
			sb.append("static ");
		}
		if ((flags & Opcodes.ACC_FINAL) != 0) {
			sb.append("final ");
		}
		if ((flags & Opcodes.ACC_SYNCHRONIZED) != 0) {
			sb.append("synchronized ");
		}
		if ((flags & Opcodes.ACC_BRIDGE) != 0) {
			sb.append("bridge ");
		}
		if ((flags & Opcodes.ACC_VARARGS) != 0) {
			sb.append("varargs ");
		}
		if ((flags & Opcodes.ACC_NATIVE) != 0) {
			sb.append("native ");
		}
		if ((flags & Opcodes.ACC_ABSTRACT) != 0) {
			sb.append("abstract ");
		}
		if ((flags & Opcodes.ACC_SYNTHETIC) != 0) {
			sb.append("synthetic ");
		}
		if ((flags & Opcodes.ACC_DEPRECATED) != 0) {
			sb.append("deprecated ");
		}
		if ((flags & Opcodes.ACC_INTERFACE) != 0) {
			sb.append("interface ");
		}
		return sb.toString().trim();
	}

	public static String toAccessForMember(int flags) {
		StringBuilder sb = new StringBuilder();
		if ((flags & Opcodes.ACC_PUBLIC) != 0) {
			sb.append("public ");
		}
		if ((flags & Opcodes.ACC_PRIVATE) != 0) {
			sb.append("private ");
		}
		if ((flags & Opcodes.ACC_STATIC) != 0) {
			sb.append("static ");
		}
		if ((flags & Opcodes.ACC_PROTECTED) != 0) {
			sb.append("protected ");
		}
		if ((flags & Opcodes.ACC_FINAL) != 0) {
			sb.append("final ");
		}
		if ((flags & Opcodes.ACC_SUPER) != 0) {
			sb.append("super ");
		}
		if ((flags & Opcodes.ACC_INTERFACE) != 0) {
			sb.append("interface ");
		}
		if ((flags & Opcodes.ACC_ABSTRACT) != 0) {
			sb.append("abstract ");
		}
		if ((flags & Opcodes.ACC_SYNTHETIC) != 0) {
			sb.append("synthetic ");
		}
		if ((flags & Opcodes.ACC_ANNOTATION) != 0) {
			sb.append("annotation ");
		}
		if ((flags & Opcodes.ACC_ENUM) != 0) {
			sb.append("enum ");
		}
		if ((flags & Opcodes.ACC_DEPRECATED) != 0) {
			sb.append("deprecated ");
		}
		return sb.toString().trim();
	}

	private String toHex(int i, int len) {
		StringBuilder sb = new StringBuilder("00000000");
		sb.append(Integer.toHexString(i));
		return "0x" + sb.substring(sb.length() - len);
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		destination.print("ANNOTATION " + desc + " vis?" + visible + " VALUE ");
		return new AnnotationVisitorPrinter();
	}

	class AnnotationVisitorPrinter extends AnnotationVisitor {

		public AnnotationVisitorPrinter() {
			super(ASM5);
		}

		public void visit(String name, Object value) {
			destination.print(name + "=" + value + " ");
		}

		public void visitEnum(String name, String desc, String value) {
			destination.print(name + "=" + desc + "." + value + " ");
		}

		public AnnotationVisitor visitAnnotation(String name, String desc) {
			destination.print(name + "=" + desc + " ");
			return new AnnotationVisitorPrinter();
		}

		public AnnotationVisitor visitArray(String name) {
			destination.print(name + " ");
			return new AnnotationVisitorPrinter();
		}

		public void visitEnd() {
			destination.println();
		}

	}

	public void visitAttribute(Attribute attr) {
	}

	public void visitEnd() {
		destination.println();
	}

	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		StringBuilder sb = new StringBuilder();
		sb.append("FIELD " + toHex(access, 4) + "(" + toAccessForMember(access) + ") " + name + " " + desc
				+ (signature != null ? " " + signature : ""));
		destination.println(sb.toString().trim());
		return null;
	}

	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		destination.println("INNERCLASS: " + name + " " + outerName + " " + innerName + " " + access);
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		StringBuilder s = new StringBuilder();
		s.append("METHOD: " + toHex(access, 4) + "(" + toAccessForMember(access) + ") " + name + desc + " "
				+ fromArray(exceptions));
		destination.println(s.toString().trim());
		return (includeFlags & INCLUDE_BYTECODE) != 0 ? new MethodPrinter(destination, includeFlags) : null;
	}

	private String fromArray(Object[] os) {
		if (os == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Object o : os) {
			sb.append(o).append(" ");
		}
		return sb.toString();
	}

	public void visitOuterClass(String owner, String name, String desc) {
		destination.println("OUTERCLASS: " + owner + " " + name + " " + desc);
	}

	public void visitSource(String source, String debug) {
		destination.println("SOURCE: " + source + " " + debug);
	}

}
