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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * @author Andy Clement
 * @since 0.5.0
 */
class ConstructorCopier extends MethodVisitor implements Constants {

	// It is important to know when an INVOKESPECIAL is hit, whether it is our actual one that delegates to the super or just
	// one being invoked due to some early object construction prior to the real INVOKESPECIAL running.  By tracking
	// how many unitialized objects there are (count the NEWs) and how many INVOKESPECIALs have occurred, it is possible
	// to identify the right one.
	private int state = preInvokeSpecial;

	private int unitializedObjectsCount = 0;

	private TypeDescriptor typeDescriptor;

	private String suffix;

	private String classname;

	public ConstructorCopier(MethodVisitor mv, TypeDescriptor typeDescriptor, String suffix, String classname) {
		super(ASM5, mv);
		this.typeDescriptor = typeDescriptor;
		this.suffix = suffix;
		this.classname = classname;
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

	@Override
	public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
		super.visitFieldInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		if (opcode == NEW) {
			unitializedObjectsCount++;
		}
		super.visitTypeInsn(opcode, type);
	}

	// TODO may need to pay attention itf==true
	@Override
	public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc,
			boolean itf) {
		// If this is an invokespecial, first determine if it is the one of interest (the one calling our super constructor)
		if (opcode == INVOKESPECIAL && name.charAt(0) == '<') {
			if (unitializedObjectsCount != 0) {
				unitializedObjectsCount--;
			}
			else {
				// This looks like our INVOKESPECIAL
				if (state == preInvokeSpecial) {
					// special case for calling jlObject, do nothing!
					if (owner.equals("java/lang/Object")) {
						mv.visitInsn(POP);
					}
					else {
						// Need to replace this INVOKESPECIAL call.
						String supertypename = typeDescriptor.getSupertypeName();
						ReloadableType superRtype = typeDescriptor.getReloadableType().getTypeRegistry().getReloadableSuperType(
								supertypename);
						if (superRtype == null) {
							// supertype was not reloadable.  This either means it really isn't (doesn't match what we consider reloadable)
							// or it just hasn't been loaded yet.
							// In a real scenario supertypes will get loaded first always and this can't happen (the latter case) - it happens in tests
							// because they don't actively load all their bits and pieces in a hierarchical way.  Given that on a reloadable boundary
							// the magic ctors are setup to call a default ctor, we can assume that above the boundary the object has been initialized.
							// this means we don't need to call a super __init__ or __execute...
							/*
																	if (typeDescriptor.getReloadableType().getTypeRegistry().isReloadableTypeName(supertypename)) {
																					superRtype = typeDescriptor.getReloadableType().getTypeRegistry()
																								.getReloadableSuperType(supertypename);
																						throw new IllegalStateException("The supertype " + supertypename.replace('/', '.')
																								+ " has not been loaded as a reloadabletype");
																					}
																					*/
							Utils.insertPopsForAllParameters(mv, desc);
							mv.visitInsn(POP); // pop 'this'
						}
						else {
							// Check the original form of the supertype for a constructor to call
							MethodMember existingCtor = (superRtype == null ? null
									: superRtype.getTypeDescriptor().getConstructor(
											desc));
							if (existingCtor == null) {
								// It did not exist in the original supertype version, need to use dynamic dispatch method
								// collapse the arguments on the stack
								Utils.collapseStackToArray(mv, desc);
								// now the stack is the instance then the params
								mv.visitInsn(SWAP);
								mv.visitInsn(DUP_X1);
								// no stack is instance then params then instance
								mv.visitLdcInsn("<init>" + desc);
								mv.visitMethodInsn(INVOKESPECIAL, typeDescriptor.getSupertypeName(),
										mDynamicDispatchName,
										mDynamicDispatchDescriptor, false);
								mv.visitInsn(POP);
							}
							else {
								// it did exist in the original, so there will be parallel constructor
								mv.visitMethodInsn(INVOKESPECIAL, typeDescriptor.getSupertypeName(), mInitializerName,
										desc, false);
							}
						}
					}

					state = postInvokeSpecial;

					return;
				}
			}
		}
		// Is it a private method call?
		// TODO r$ check here because we use invokespecial to avoid virtual dispatch on field changes...
		if (opcode == INVOKESPECIAL && name.charAt(0) != '<' && owner.equals(classname) && !name.startsWith("r$")) {
			// leaving the invokespecial alone will cause a verify error
			String descriptor = Utils.insertExtraParameter(owner, desc);
			super.visitMethodInsn(INVOKESTATIC, Utils.getExecutorName(classname, suffix), name, descriptor, false);
		}
		else {
			boolean done = false;
			// TODO dup of code in method copier - can we refactor?
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
	}
}
