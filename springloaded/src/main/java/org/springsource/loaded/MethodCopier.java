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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.springsource.loaded.Utils.ReturnType;


/**
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
class MethodCopier extends MethodVisitor implements Constants {

	private boolean isInterface;

	private String descriptor;

	private TypeDescriptor typeDescriptor;

	private String classname;

	private String suffix;

	private boolean hasFieldsRequiringAccessors;

	public MethodCopier(MethodVisitor mv, boolean isInterface, String descriptor, TypeDescriptor typeDescriptor,
			String classname,
			String suffix) {
		super(ASM5, mv);
		this.isInterface = isInterface;
		this.descriptor = descriptor;
		this.typeDescriptor = typeDescriptor;
		this.classname = classname;
		this.suffix = suffix;
		this.hasFieldsRequiringAccessors = this.typeDescriptor.getFieldsRequiringAccessors().length != 0;
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		// Rename 'this' to 'thiz' in executor otherwise Eclipse debugger will fail (static method with 'this')
		if (index == 0 && name.equals("this")) {
			super.visitLocalVariable("thiz", desc, signature, start, end, index);
		}
		else {
			super.visitLocalVariable(name, desc, signature, start, end, index);
		}
	}

	private FieldMember findFieldIfRequiresAccessorUsage(String owner, String name) {
		FieldMember[] fms = this.typeDescriptor.getFieldsRequiringAccessors();
		for (FieldMember fm : fms) {
			if (fm.getName().equals(name) && (owner.equals(classname) || isOneOfOurSupertypes(owner))) { // && fm.getDeclaringTypeName().equals(owner)) {
				// possibly a match - testcase scenario:
				// 'owner=prot/SubThree' - this is what the FIELD instruction is working on
				// 'dfm=prot/Three' - this is the type that declared the field
				// 'classname=prot/SubThree' - this is the type we are currently operating on

				// in our other case though (with JDK Proxies)
				// owner=java/lang/reflect/Proxy
				// dfm=java/lang/reflect/Proxy
				// classname=$Proxy6
				// (this is the funky InvocationHandler field called 'h' in Proxy)
				return fm;
			}
		}
		return null;
	}

	/**
	 * Determine if the supplied type is a supertype of the current type we are modifying. This is used to determine if
	 * the owner we have discovered for a field is one of our supertypes (and so, if it is protected, whether it is
	 * something that needs redirecting through an accessor).
	 * 
	 * @param type the type which may be one of this types supertypes
	 * @return true if it is a supertype
	 */
	private boolean isOneOfOurSupertypes(String type) {
		String stypeName = typeDescriptor.getSupertypeName();
		while (stypeName != null) {
			// TODO [bug] should stop at the first one that has a field in it? and check the field is protected, yada yada yada
			if (stypeName.equals(type)) {
				return true;
			}
			stypeName = typeDescriptor.getTypeRegistry().getDescriptorFor(stypeName).getSupertypeName();
		}
		return false;
	}

	private TypeDescriptor getType(String type) {
		TypeDescriptor typeDescriptor = this.typeDescriptor.getTypeRegistry().getDescriptorFor(type);
		return typeDescriptor;
	}

	@Override
	public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
		if (hasFieldsRequiringAccessors) {
			// Check if this field reference needs redirecting to an accessor
			FieldMember fm = findFieldIfRequiresAccessorUsage(owner, name);
			if (fm != null) {
				switch (opcode) {
					case GETFIELD:
						mv.visitMethodInsn(INVOKEVIRTUAL, classname, Utils.getProtectedFieldGetterName(name), "()"
								+ desc, false);
						return;
					case PUTFIELD:
						mv.visitMethodInsn(INVOKEVIRTUAL, classname, Utils.getProtectedFieldSetterName(name), "("
								+ desc + ")V", false);
						return;
					case GETSTATIC:
						mv.visitMethodInsn(INVOKESTATIC, classname, Utils.getProtectedFieldGetterName(name), "()"
								+ desc, false);
						return;
					case PUTSTATIC:
						mv.visitMethodInsn(INVOKESTATIC, classname, Utils.getProtectedFieldSetterName(name), "(" + desc
								+ ")V", false);
						return;
				}
			}
		}
		super.visitFieldInsn(opcode, owner, name, desc);
	}

	// TODO maybe do something if 'itf==true'
	@Override
	public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, boolean itf) {
		// Is it a private method call?
		// TODO r$ check here because we use invokespecial to avoid virtual dispatch on field changes...
		if (opcode == INVOKESPECIAL && name.charAt(0) != '<' && !name.startsWith("r$")) {
			if (owner.equals(classname)) {
				// private method call
				// leaving the invokespecial alone will cause a verify error
				String descriptor = Utils.insertExtraParameter(owner, desc);
				super.visitMethodInsn(INVOKESTATIC, Utils.getExecutorName(classname, suffix), name, descriptor, false);
				return;
			}
			else {
				// super call
				// TODO Check if this is true: we can just call the catcher directly if there was one, there is no need
				// for a superdispatcher

				// Only need to redirect to the superdispatcher if it was a protected method
				TypeDescriptor supertypeDescriptor = getType(owner);
				MethodMember target = supertypeDescriptor.getByNameAndDescriptor(name + desc);
				if (target != null && target.isProtected()) {
					// A null target means that method is not in the supertype, so didn't get a superdispatcher
					super.visitMethodInsn(INVOKESPECIAL, classname, name + methodSuffixSuperDispatcher, desc, false);
				}
				else {
					super.visitMethodInsn(opcode, owner, name, desc, itf);
				}
				return;
			}
		}
		// Might be a private static method
		boolean done = false;
		if (opcode == INVOKESTATIC) {
			MethodMember mm = typeDescriptor.getByDescriptor(name, desc);
			if (mm != null && mm.isPrivate()) {
				super.visitMethodInsn(INVOKESTATIC, Utils.getExecutorName(classname, suffix), name, desc, false);
				done = true;
			}
		}
		if (!done) {
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
	}

	@Override
	public void visitEnd() {
		if (isInterface) {
			// Create 'dummy methods' for an interface implementation
			createDummyMethodBody();
			super.visitEnd();
		}
	}

	private void createDummyMethodBody() {
		ReturnType returnType = Utils.getReturnTypeDescriptor(descriptor);
		int descriptorSize = Utils.getSize(descriptor);
		if (returnType.isVoid()) {
			super.visitInsn(RETURN);
			super.visitMaxs(1, descriptorSize);
		}
		else if (returnType.isPrimitive()) {
			super.visitLdcInsn(0);
			switch (returnType.descriptor.charAt(0)) {
				case 'B':
				case 'C':
				case 'I':
				case 'S':
				case 'Z':
					super.visitInsn(IRETURN);
					super.visitMaxs(2, descriptorSize);
					break;
				case 'D':
					super.visitInsn(DRETURN);
					super.visitMaxs(3, descriptorSize);
					break;
				case 'F':
					super.visitInsn(FRETURN);
					super.visitMaxs(2, descriptorSize);
					break;
				case 'J':
					super.visitInsn(LRETURN);
					super.visitMaxs(3, descriptorSize);
					break;
				default:
					throw new IllegalStateException(returnType.descriptor);
			}
		}
		else {
			// reference type
			super.visitInsn(ACONST_NULL);
			super.visitInsn(ARETURN);
			super.visitMaxs(1, descriptorSize);
		}
	}
}
