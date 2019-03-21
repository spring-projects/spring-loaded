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

package org.springsource.loaded;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This is similar to SystemClassReflectionRewriter but this version just summarizes what it finds, rather than making
 * any changes. Using the results of this we can determine whether it needs proper rewriting by the
 * SystemClassReflectionRewriter (which would be done by adding this class to the list of those in the SLPP that should
 * be processed like that).
 * 
 * @author Andy Clement
 * @since 0.7.3
 */
public class SystemClassReflectionInvestigator {


	public static int investigate(String slashedClassName, byte[] bytes, boolean print) {
		ClassReader fileReader = new ClassReader(bytes);
		RewriteClassAdaptor classAdaptor = new RewriteClassAdaptor(print);
		fileReader.accept(classAdaptor, ClassReader.SKIP_FRAMES);
		return classAdaptor.hitCount;
	}

	static class RewriteClassAdaptor extends ClassVisitor implements Constants {

		int hitCount = 0;

		private ClassWriter cw;

		int bits = 0x0000;

		private boolean print;

		private String classname;

		private static boolean isInterceptable(String owner, String methodName) {
			return MethodInvokerRewriter.RewriteClassAdaptor.intercepted.contains(owner + "." + methodName);
		}

		public RewriteClassAdaptor(boolean print) {
			// TODO should it also compute frames?
			super(ASM5, new ClassWriter(ClassWriter.COMPUTE_MAXS));
			this.print = print;
			cw = (ClassWriter) cv;
		}

		public byte[] getBytes() {
			byte[] bytes = cw.toByteArray();
			return bytes;
		}

		public int getBits() {
			return bits;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			this.classname = name;
		}

		@Override
		public MethodVisitor visitMethod(int flags, String name, String descriptor, String signature,
				String[] exceptions) {
			MethodVisitor mv = super.visitMethod(flags, name, descriptor, signature, exceptions);
			return new RewritingMethodAdapter(mv);
		}

		class RewritingMethodAdapter extends MethodVisitor implements Opcodes, Constants {

			public RewritingMethodAdapter(MethodVisitor mv) {
				super(ASM5, mv);
			}

			private boolean interceptReflection(String owner, String name, String desc) {
				if (isInterceptable(owner, name)) {
					hitCount++;
					if (print) {
						System.out.println("SystemClassReflectionInvestigator: " + classname + "  uses " + owner + "."
								+ name + desc);
					}
				}
				return false;
			}

			int unitializedObjectsCount = 0;

			@Override
			public void visitTypeInsn(final int opcode, final String type) {
				if (opcode == NEW) {
					unitializedObjectsCount++;
				}
				super.visitTypeInsn(opcode, type);
			}

			@Override
			public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc,
					final boolean itf) {
				if (!GlobalConfiguration.interceptReflection || rewriteReflectiveCall(opcode, owner, name, desc)) {
					return;
				}
				if (opcode == INVOKESPECIAL) {
					unitializedObjectsCount--;
				}
				super.visitMethodInsn(opcode, owner, name, desc, itf);
			}

			/**
			 * Determine if a method call is a reflective call and an attempt should be made to rewrite it.
			 * 
			 * @return true if the call was rewritten
			 */
			private boolean rewriteReflectiveCall(int opcode, String owner, String name, String desc) {
				if (owner.length() > 10 && owner.charAt(8) == 'g'
						&& (owner.startsWith("java/lang/reflect/") || owner.equals("java/lang/Class"))) {
					boolean rewritten = interceptReflection(owner, name, desc);
					if (rewritten) {
						return true;
					}
				}
				return false;
			}

		}
	}
}
