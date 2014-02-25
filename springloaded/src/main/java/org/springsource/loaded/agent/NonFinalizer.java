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

import java.lang.reflect.Modifier;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.springsource.loaded.Constants;


/**
 * Makes a field or fields non final.
 * 
 * @author Andy Clement
 * @since 0.7.0
 */
public class NonFinalizer extends ClassVisitor implements Constants {

	private String fieldname;

	/**
	 * This ClassAdapter will visit a class and within the constructors it will add a call to the specified method (assumed static)
	 * just before each constructor returns. The target of the call should be a collecting method that will likely do something with
	 * the instances later on class reload.
	 * 
	 * @param fieldname the name of the field to be made non final
	 */
	public NonFinalizer(String fieldname) {
		super(ASM5,new ClassWriter(0)); // TODO review 0 here
		this.fieldname = fieldname;
	}

	public byte[] getBytes() {
		return ((ClassWriter) cv).toByteArray();
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if (name.equals(fieldname)) {
			return super.visitField(access & (~Modifier.FINAL), name, desc, signature, value);
		} else {
			return super.visitField(access, name, desc, signature, value);
		}
	}

	//	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
	//		if (name.equals("<init>")) {
	//			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
	//			return new ConstructorAppender(mv);
	//		} else {
	//			return super.visitMethod(access, name, desc, signature, exceptions);
	//		}
	//	}

	/**
	 * This constructor appender includes a couple of instructions at the end of each constructor it is asked to visit. It
	 * recognizes the end by observing a RETURN instruction. The instructions are inserted just before the RETURN.
	 */
	//	class ConstructorAppender extends MethodAdapter implements Constants {
	//
	//		public ConstructorAppender(MethodVisitor mv) {
	//			super(mv);
	//		}
	//
	//		@Override
	//		public void visitInsn(int opcode) {
	//			if (opcode == RETURN) {
	//				mv.visitVarInsn(ALOAD, 0);
	//				mv.visitMethodInsn(INVOKESTATIC, calleeOwner, calleeName, "(Ljava/lang/Object;)V");
	//			}
	//			super.visitInsn(opcode);
	//		}
	//
	//	}
}