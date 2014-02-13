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
package org.springsource.loaded.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.springsource.loaded.Constants;


/**
 * 
 * @author Andy Clement
 * @since 0.7.0
 */
public class FalseReturner extends ClassVisitor implements Constants {

	private String methodname;

	public FalseReturner(String methodname) {
		super(ASM5,new ClassWriter(0)); // TODO review 0 here
		this.methodname = methodname;
	}

	public byte[] getBytes() {
		return ((ClassWriter) cv).toByteArray();
	}

	//	@Override
	//	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
	//		if (name.equals(methodname)) {
	//			return super.visitField(access & (~Modifier.FINAL), name, desc, signature, value);
	//		} else {
	//			return super.visitField(access, name, desc, signature, value);
	//		}
	//	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (name.equals(methodname)) {
			//			return new FakeMethodVisitor();
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			mv.visitCode();
			mv.visitInsn(ICONST_0);
			mv.visitInsn(IRETURN);
			mv.visitMaxs(3, 1);
			mv.visitEnd();
			return mv;
			//			return new FalseReturnerMV(mv);
		} else {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
	}

	public boolean m() {
		return false;
	}

	//	class FakeMethodVisitor implements MethodVisitor, Constants {
	//
	//	}
}