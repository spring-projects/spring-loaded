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

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.springsource.loaded.Utils.ReturnType;


/**
 * Builder that creates the dispatcher. The dispatcher is the implementation of the interface extracted for a type which then
 * delegates to the executor. A new dispatcher (and executor) is built for each class reload.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class DispatcherBuilder {

	/**
	 * Factory method that builds the dispatcher for a specified reloadabletype.
	 * 
	 * @param rtype the reloadable type
	 * @param newVersionTypeDescriptor the descriptor of the new version (the executor will be generated according to this)
	 * @param versionstamp the suffix that should be appended to the generated dispatcher
	 * @return the bytecode for the new dispatcher
	 */
	public static byte[] createFor(ReloadableType rtype, IncrementalTypeDescriptor newVersionTypeDescriptor, String versionstamp) {
		ClassReader fileReader = new ClassReader(rtype.interfaceBytes);
		DispatcherBuilderVisitor dispatcherVisitor = new DispatcherBuilderVisitor(rtype, newVersionTypeDescriptor, versionstamp);
		fileReader.accept(dispatcherVisitor, 0);
		return dispatcherVisitor.getBytes();
	}

	/**
	 * Whilst visiting the interface, the implementation is created.
	 */
	static class DispatcherBuilderVisitor extends ClassVisitor implements Opcodes, Constants {

		private ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		private String classname;
		private String executorClassName;
		private String suffix;
		private ReloadableType rtype;
		private IncrementalTypeDescriptor typeDescriptor;

		public DispatcherBuilderVisitor(ReloadableType rtype, IncrementalTypeDescriptor typeDescriptor, String suffix) {
			super(ASM5);
			this.classname = rtype.getSlashedName();
			this.typeDescriptor = typeDescriptor;
			this.suffix = suffix;
			this.rtype = rtype;
			this.executorClassName = Utils.getExecutorName(classname, suffix);
		}

		public byte[] getBytes() {
			return cw.toByteArray();
		}

		public void visit(int version, int flags, String name, String signature, String superclassName, String[] interfaceNames) {
			String dispatcherName = Utils.getDispatcherName(classname, suffix);
			cw.visit(version, Opcodes.ACC_PUBLIC, dispatcherName, null, "java/lang/Object",
					new String[] { Utils.getInterfaceName(classname), "org/springsource/loaded/__DynamicallyDispatchable" });
			generateDefaultConstructor();
		}

		private void generateDefaultConstructor() {
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		private void generateClinitDispatcher() {
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, mStaticInitializerName, "()V", null, null);
			mv.visitCode();
			mv.visitMethodInsn(INVOKESTATIC, executorClassName, mStaticInitializerName, "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
			return null;
		}

		public void visitAttribute(Attribute arg0) {
		}

		public void visitEnd() {
		}

		public FieldVisitor visitField(int arg0, String arg1, String arg2, String arg3, Object arg4) {
			return null;
		}

		public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
		}

		public MethodVisitor visitMethod(int flags, String name, String descriptor, String signature, String[] exceptions) {
			if (name.equals(mDynamicDispatchName)) {
				generateDynamicDispatchMethod(name, descriptor, signature, exceptions);
			} else if (!name.equals("<init>")) {
				generateRegularMethod(name, descriptor, signature, exceptions);
			}
			return null;
		}

		/**
		 * Generate the body of the dynamic dispatcher method. This method is responsible for calling all the methods that are added
		 * to a type after the first time it is defined.
		 */
		private void generateDynamicDispatchMethod(String name, String descriptor, String signature, String[] exceptions) {
			final int indexDispatcherInstance = 0;
			final int indexArgs = 1;
			final int indexTarget = 2;
			final int indexNameAndDescriptor = 3;

			// Should be generating the code for each additional method in
			// the executor (new version) that wasn't in the original. 
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC , name, descriptor, signature, exceptions);
			mv.visitCode();

			// Entries required here for all methods that exist in the new version but didn't exist in the original version
			// There should be no entries for catchers

			int maxStack = 0;
			// Basically generate a long if..else sequence for each method
			List<MethodMember> methods = new ArrayList<MethodMember>(typeDescriptor.getNewOrChangedMethods());

			// these are added because we may be calling through the dynamic dispatcher if calling from an invokeinterface - the invokeinterface
			// will call __execute on the interface, which is then implemented by the real class - but it may be that the
			// actual type implementing the interface already implements that method - if the dispatcher doesn't recognize
			// it then we may go bang

			//			System.out.println("Generating __execute in type " + classname);
			for (MethodMember m : typeDescriptor.getOriginal().getMethods()) {
				methods.add(m);
			}

			for (MethodMember method : methods) {
				if (MethodMember.isCatcher(method) || MethodMember.isSuperDispatcher(method)) { // for reason above, may also need to consider catchers here - what if an interface is changed to add a toString() method, for example
					continue;
					// would the implementation for a catcher call the super catcher?
				}
				//				System.out.println("Generating handler for " + method.name);
				String nameWithDescriptor = new StringBuilder(method.name).append(method.descriptor).toString();

				// 2. Load the input name+descriptor and compare it with this method:
				mv.visitVarInsn(ALOAD, 3);
				mv.visitLdcInsn(nameWithDescriptor);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
				Label label = new Label();
				mv.visitJumpInsn(IFEQ, label); // means if false

				// 3. Generate the code that will call the method on the executor:
				if (!method.isStatic()) {
					mv.visitVarInsn(Opcodes.ALOAD, 2);
					mv.visitTypeInsn(CHECKCAST, classname);
				}
				String callDescriptor = method.isStatic() ? method.descriptor : Utils.insertExtraParameter(classname,
						method.descriptor);

				int pcount = Utils.getParameterCount(method.descriptor);
				if (pcount > maxStack) {
					pcount = maxStack;
				}

				// 4. Unpack parameter array to fit the descriptor for that method
				Utils.generateInstructionsToUnpackArrayAccordingToDescriptor(mv, method.descriptor, 1);

				ReturnType returnType = Utils.getReturnTypeDescriptor(method.descriptor);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, executorClassName, method.name, callDescriptor, false);
				if (returnType.isVoid()) {
					mv.visitInsn(ACONST_NULL);
				} else if (returnType.isPrimitive()) {
					Utils.insertBoxInsns(mv, returnType.descriptor);
				}
				mv.visitInsn(Opcodes.ARETURN);
				mv.visitLabel(label);
			}
			for (MethodMember ctor : typeDescriptor.getLatestTypeDescriptor().getConstructors()) {
				String nameWithDescriptor = new StringBuilder(ctor.name).append(ctor.descriptor).toString();

				// 2. Load the input name+descriptor and compare it with this method:
				//    if (nameAndDescriptor.equals(xxx)) {
				mv.visitVarInsn(ALOAD, indexNameAndDescriptor);
				mv.visitLdcInsn(nameWithDescriptor);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
				Label label = new Label();
				mv.visitJumpInsn(IFEQ, label); // means if false

				// 3. Generate the code that will call the method on the executor:
				mv.visitVarInsn(Opcodes.ALOAD, 2);
				mv.visitTypeInsn(CHECKCAST, classname);
				String callDescriptor = Utils.insertExtraParameter(classname, ctor.descriptor);

				int pcount = Utils.getParameterCount(ctor.descriptor);
				if (pcount > maxStack) {
					pcount = maxStack;
				}

				// 4. Unpack parameter array to fit the descriptor for that method
				Utils.generateInstructionsToUnpackArrayAccordingToDescriptor(mv, ctor.descriptor, 1);

				//				ReturnType returnType = Utils.getReturnTypeDescriptor(method.descriptor);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, executorClassName, "___init___", callDescriptor, false);
				//				if (returnType.isVoid()) {
				mv.visitInsn(ACONST_NULL);
				//				} else if (returnType.isPrimitive()) {
				//					Utils.insertBoxInsns(mv, returnType.descriptor);
				//				}
				mv.visitInsn(Opcodes.ARETURN);
				mv.visitLabel(label);
			}

			// 5. Throw exception as dynamic dispatcher has been called for something it shouldn't have

			// At this point we failed to find it as a method we can dispatch to our executor, so we want
			// to pass it 'up' to our supertype.  We need to get the dispatcher for our superclass
			// and then call the __execute() on it, assuming that it will be able to handle this request.		

			// alternative 1: use the dispatcher for the superclass

			// Determine the supertype
			String slashedSupertypeName = rtype.getTypeDescriptor().getSupertypeName();

			// getDispatcher will give us the dispatcher for the supertype
			mv.visitFieldInsn(Opcodes.GETSTATIC, slashedSupertypeName, fReloadableTypeFieldName, lReloadableType);
			mv.visitMethodInsn(INVOKEVIRTUAL, tReloadableType, "getDispatcher",
					"()Lorg/springsource/loaded/__DynamicallyDispatchable;", false);

			// alternative 2: find the right dispatcher - i.e. who in the super hierarchy provides that nameAndDescriptor

			// now invoke the dynamic dispatch call on that dispatcher
			mv.visitVarInsn(ALOAD, indexArgs);
			mv.visitVarInsn(ALOAD, indexTarget);
			mv.visitVarInsn(ALOAD, indexNameAndDescriptor);
			mv.visitMethodInsn(INVOKEINTERFACE, tDynamicallyDispatchable, mDynamicDispatchName, mDynamicDispatchDescriptor, false);
			mv.visitInsn(ARETURN);

			//			mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
			//			mv.visitInsn(DUP);
			//			mv.visitVarInsn(ALOAD, 3);
			//			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V");
			//			mv.visitInsn(ATHROW);
			mv.visitMaxs(maxStack, 6);
			mv.visitEnd();
		}

		/**
		 * Called to generate the implementation of a normal method on the interface - a normal method is one that did exist when
		 * the type was first defined. Might be a catcher.
		 */
		private void generateRegularMethod(String name, String descriptor, String signature, String[] exceptions) {
			// The original descriptor is how it was defined on the original type and how it is defined in the executor class.
			// The original descriptor is this descriptor with the first parameter trimmed off. 
			boolean isClinit = name.equals("___clinit___");
			String originalDescriptor = isClinit ? descriptor : Utils.stripFirstParameter(descriptor);
			MethodMember method = null;

			// Detect if the name has been modified for clash avoidance reasons
			if (name.equals("___init___")) {
				// it is a ctor
				method = rtype.getConstructor(originalDescriptor);
			} else {
				if (isClinit) {
					generateClinitDispatcher();
					return;
				} else {
					// TODO need a better solution that these __
					if (name.startsWith("__") && !name.equals("__$swapInit")) { // __$swapInit is the groovy reset method
						// clash avoidance name
						method = rtype.getMethod(name.substring(2), originalDescriptor);
					} else {
						method = rtype.getMethod(name, originalDescriptor);
					}
				}
			}
			boolean isStatic = method.isStatic();

			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, descriptor, signature, exceptions);
			mv.visitCode();
			// The input descriptor will include the extra initial parameter (the instance, or null for static methods)
			ReturnType returnTypeDescriptor = Utils.getReturnTypeDescriptor(descriptor);
			// For a static method the first parameter can be ignored
			int params = Utils.getParameterCount(descriptor);
			String callDescriptor = isStatic ? originalDescriptor : descriptor;
			Utils.createLoadsBasedOnDescriptor(mv, callDescriptor, isStatic ? 2 : 1);
			mv.visitMethodInsn(INVOKESTATIC, executorClassName, name, callDescriptor, false);
			Utils.addCorrectReturnInstruction(mv, returnTypeDescriptor, false);
			mv.visitMaxs(params, params + 1);
			mv.visitEnd();
		}

		public void visitOuterClass(String arg0, String arg1, String arg2) {
		}

		public void visitSource(String arg0, String arg1) {
		}

	}

}
