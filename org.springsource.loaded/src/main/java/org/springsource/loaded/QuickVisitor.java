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
package org.springsource.loaded;

import org.objectweb.asm.ClassReader;

/**
 * Can be used to take a quick look in the bytecode for something. The various static get* methods are the things that the quick
 * visitor can discover.
 * 
 * @author Andy Clement
 * @since 0.7.3
 */
public class QuickVisitor {

	public static String[] getImplementedInterfaces(byte[] bytes) {
		ClassReader fileReader = new ClassReader(bytes);
		QuickVisitor1 qv = new QuickVisitor1();
		try {
			fileReader.accept(qv, ClassReader.SKIP_FRAMES);// TODO more flags to skip other things?
		} catch (EarlyExitException eee) {
		}
		return qv.interfaces;
	}

	static class QuickVisitor1 extends EmptyClassVisitor {
		String[] interfaces;

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			this.interfaces = interfaces;
			// TODO is it truly easier to exit via exception than to visit the rest of it?
			throw new EarlyExitException();
		}
	}

	@SuppressWarnings("serial")
	private static class EarlyExitException extends RuntimeException {

	}

	//	public static int investigate(String slashedClassName, byte[] bytes) {
	//		ClassReader fileReader = new ClassReader(bytes);
	//		RewriteClassAdaptor classAdaptor = new RewriteClassAdaptor();
	//		fileReader.accept(classAdaptor, ClassReader.SKIP_FRAMES);
	//		return classAdaptor.hitCount;
	//	}
	//
	//	static class RewriteClassAdaptor extends ClassAdapter implements Constants {
	//
	//		int hitCount = 0;
	//		private ClassWriter cw;
	//		int bits = 0x0000;
	//		private String classname;
	//
	//		private static boolean isInterceptable(String owner, String methodName) {
	//			return MethodInvokerRewriter.RewriteClassAdaptor.intercepted.contains(owner + "." + methodName);
	//		}
	//
	//		public RewriteClassAdaptor() {
	//			// TODO should it also compute frames?
	//			super(new ClassWriter(ClassWriter.COMPUTE_MAXS));
	//			cw = (ClassWriter) cv;
	//		}
	//
	//		public byte[] getBytes() {
	//			byte[] bytes = cw.toByteArray();
	//			return bytes;
	//		}
	//
	//		public int getBits() {
	//			return bits;
	//		}
	//
	//		@Override
	//		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
	//			super.visit(version, access, name, signature, superName, interfaces);
	//			this.classname = name;
	//		}
	//
	//		@Override
	//		public MethodVisitor visitMethod(int flags, String name, String descriptor, String signature, String[] exceptions) {
	//			MethodVisitor mv = super.visitMethod(flags, name, descriptor, signature, exceptions);
	//			return new RewritingMethodAdapter(mv);
	//		}
	//
	//		class RewritingMethodAdapter extends MethodAdapter implements Opcodes, Constants {
	//
	//			public RewritingMethodAdapter(MethodVisitor mv) {
	//				super(mv);
	//			}
	//
	//			private boolean interceptReflection(String owner, String name, String desc) {
	//				if (isInterceptable(owner, name)) {
	//					hitCount++;
	//					System.out.println("SystemClassReflectionInvestigator: " + classname + "  uses " + owner + "." + name + desc);
	//				}
	//				return false;
	//			}
	//
	//			int unitializedObjectsCount = 0;
	//
	//			@Override
	//			public void visitTypeInsn(final int opcode, final String type) {
	//				if (opcode == NEW) {
	//					unitializedObjectsCount++;
	//				}
	//				super.visitTypeInsn(opcode, type);
	//			}
	//
	//			@Override
	//			public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
	//				if (!GlobalConfiguration.interceptReflection || rewriteReflectiveCall(opcode, owner, name, desc)) {
	//					return;
	//				}
	//				if (opcode == INVOKESPECIAL) {
	//					unitializedObjectsCount--;
	//				}
	//				super.visitMethodInsn(opcode, owner, name, desc);
	//			}
	//
	//			/**
	//			 * Determine if a method call is a reflective call and an attempt should be made to rewrite it.
	//			 * 
	//			 * @return true if the call was rewritten
	//			 */
	//			private boolean rewriteReflectiveCall(int opcode, String owner, String name, String desc) {
	//				if (owner.length() > 10 && owner.charAt(8) == 'g'
	//						&& (owner.startsWith("java/lang/reflect/") || owner.equals("java/lang/Class"))) {
	//					boolean rewritten = interceptReflection(owner, name, desc);
	//					if (rewritten) {
	//						return true;
	//					}
	//				}
	//				return false;
	//			}
	//
	//		}
	//	}
}