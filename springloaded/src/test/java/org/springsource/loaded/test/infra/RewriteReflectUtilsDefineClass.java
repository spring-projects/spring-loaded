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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.springsource.loaded.Constants;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;


/**
 * Need to intercept the defineClass() here and do the reloadable thing - won't be needed at real runtime because all classloaders
 * will be getting intercepted through the SpringLoadedPreProcessor.
 * 
 * @author Andy Clement
 * @version 0.8.3
 */
public class RewriteReflectUtilsDefineClass extends ClassVisitor implements Constants {

	public static byte[] rewriteReflectUtilsDefineClass(byte[] data) {
		ClassReader cr = new ClassReader(data);
		RewriteReflectUtilsDefineClass ca = new RewriteReflectUtilsDefineClass();
		cr.accept(ca, 0);
		byte[] newbytes = ca.getBytes();
		return newbytes;
	}

	private RewriteReflectUtilsDefineClass() {
		super(ASM5,new ClassWriter(0)); // TODO review 0 here
	}

	public byte[] getBytes() {
		return ((ClassWriter) cv).toByteArray();
	}

	public static byte[] defineClass(String className, byte[] b, ClassLoader loader) throws Exception {
		if (!className.startsWith("net.sf")) {
			//			System.out.println("Intercepted " + className);
			TypeRegistry tr = TypeRegistry.getTypeRegistryFor(loader);
			boolean bb = tr.shouldDefineClasses();
			tr.setShouldDefineClasses(false);
			ReloadableType rt = tr.addType(className, b);
			//	    	ClassPrinter.print(rt.bytesInitial);
			//	    	ClassPrinter.print(rt.bytesLoaded);
			tr.setShouldDefineClasses(bb);
			return rt.bytesLoaded;
		}
		return b;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (name.equals("defineClass")) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			return new DefineClassInterceptor(mv);
		} else {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
	}

	class DefineClassInterceptor extends MethodVisitor implements Constants {

		public DefineClassInterceptor(MethodVisitor mv) {
			super(ASM5,mv);
		}

		@Override
		public void visitCode() {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKESTATIC, "org/springsource/loaded/test/infra/RewriteReflectUtilsDefineClass", "defineClass",
					"(Ljava/lang/String;[BLjava/lang/ClassLoader;)[B");
			mv.visitVarInsn(ASTORE, 1);
		}

	}

}