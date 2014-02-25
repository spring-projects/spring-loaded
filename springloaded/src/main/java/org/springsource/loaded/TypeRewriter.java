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

import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.springsource.loaded.Utils.ReturnType;


/**
 * Rewrites a class such that it is amenable to reloading. This involves:
 * <ul>
 * <li>In every method, introduce logic to check it it the latest version of that method - if it isn't dispatch to the latest
 * <li>Creates additional methods to aid with field setting/getting
 * <li>Creates additional fields to help reloading (reloadable type instance, new field value holders)
 * <li>Creates catchers for inherited methods. Catchers are simply passed through unless a new version of the class provides an
 * implementation
 * </ul>
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class TypeRewriter implements Constants {

	private static Logger log = Logger.getLogger(TypeRewriter.class.getName());

	public static byte[] rewrite(ReloadableType rtype, byte[] bytes) {
		ClassReader fileReader = new ClassReader(bytes);
		RewriteClassAdaptor classAdaptor = new RewriteClassAdaptor(rtype);
		fileReader.accept(classAdaptor, 0);
		return classAdaptor.getBytes();
	}

	static class RewriteClassAdaptor extends ClassVisitor implements Constants {

		private ClassWriter cw;
		private String slashedname;
		private ReloadableType rtype;
		private TypeDescriptor typeDescriptor;
		private boolean clinitDone = false;
		private String supertypeName;
		private boolean isInterface;
		private boolean isEnum;
		private boolean isGroovy;

		public RewriteClassAdaptor(ReloadableType rtype) {
			this(rtype, new ClassWriter(ClassWriter.COMPUTE_MAXS));
		}

		public RewriteClassAdaptor(ReloadableType rtype, ClassWriter classWriter) {
			super(ASM5,classWriter);
			this.rtype = rtype;
			this.slashedname = rtype.getSlashedName();
			this.cw = (ClassWriter) cv;
			this.typeDescriptor = rtype.getTypeDescriptor();
			this.isInterface = typeDescriptor.isInterface();
			this.isEnum = typeDescriptor.isEnum();
			this.isGroovy = typeDescriptor.isGroovyType();
		}

		public byte[] getBytes() {
			return cw.toByteArray();
		}

		public ClassVisitor getClassVisitor() {
			return cw;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			access = Utils.promoteDefaultOrPrivateOrProtectedToPublic(access);
			super.visit(version, access, name, signature, superName, interfaces);

			this.supertypeName = superName;
			// Extra members in a reloadable type
			createReloadableTypeField();
			if (GlobalConfiguration.fieldRewriting) {
				if (!isInterface) {
					if (isTopmostReloadable()) {
						createInstanceStateManagerInstance();
					}
					createInstanceFieldGetterMethod();
					createInstanceFieldSetterMethod();
					createStaticFieldSetterMethod();
					createStaticFieldGetterMethod();
					createStaticInitializerForwarderMethod();
				}
				if (isTopmostReloadable()) {
					createStaticStateManagerInstance();
				}
			}
			if (!isInterface) {
				createManagedConstructors();
				createDispatcherCallingInitCtors();
			}
		}

		private boolean isTopmostReloadable() {
			TypeRegistry typeRegistry = rtype.getTypeRegistry();
			if (!typeRegistry.isReloadableTypeName(typeDescriptor.getSupertypeName())) {
				return true;
			} else {
				return false;
			}
		}

		private void createStaticInitializerForwarderMethod() {
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC_STATIC, mStaticInitializerName, "()V", null, null);
			mv.visitFieldInsn(Opcodes.GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
			mv.visitMethodInsn(INVOKEVIRTUAL, tReloadableType, "fetchLatest", "()Ljava/lang/Object;");
			mv.visitTypeInsn(CHECKCAST, Utils.getInterfaceName(slashedname));
			mv.visitMethodInsn(INVOKEINTERFACE, Utils.getInterfaceName(slashedname), mStaticInitializerName, "()V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 0);
			mv.visitEnd();
		}

		// TODO review whether we get these up and down the hierarchy or just at the top?
		/**
		 * Create the static field getter method which ensures the static state manager is initialized.
		 */
		private void createStaticFieldGetterMethod() {
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC_STATIC, mStaticFieldGetterName, "(Ljava/lang/String;)Ljava/lang/Object;",
					null, null);
			mv.visitFieldInsn(GETSTATIC, slashedname, fStaticFieldsName, lStaticStateManager);
			Label l2 = new Label();
			mv.visitJumpInsn(IFNONNULL, l2);
			mv.visitTypeInsn(NEW, tStaticStateManager);
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, tStaticStateManager, "<init>", "()V");
			mv.visitFieldInsn(PUTSTATIC, slashedname, fStaticFieldsName, lStaticStateManager);
			mv.visitLabel(l2);
			mv.visitFieldInsn(GETSTATIC, slashedname, fStaticFieldsName, lStaticStateManager);
			mv.visitFieldInsn(GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, tStaticStateManager, "getValue", "(" + lReloadableType
					+ "Ljava/lang/String;)Ljava/lang/Object;");
			mv.visitInsn(ARETURN);
			mv.visitMaxs(3, 2);
			mv.visitEnd();
		}

		private void createDispatcherCallingInitCtors() {
			MethodMember[] ctors = typeDescriptor.getConstructors();
			for (MethodMember ctor : ctors) {
				String desc = ctor.getDescriptor();
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "___init___", desc, null, null);
				mv.visitFieldInsn(GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
				mv.visitInsn(ICONST_1);
				mv.visitMethodInsn(INVOKEVIRTUAL, tReloadableType, "getLatestDispatcherInstance", "(Z)Ljava/lang/Object;");
				mv.visitTypeInsn(CHECKCAST, Utils.getInterfaceName(slashedname));
				String desc2 = new StringBuffer("(L").append(slashedname).append(";").append(desc.substring(1)).toString();
				mv.visitVarInsn(ALOAD, 0);
				Utils.createLoadsBasedOnDescriptor(mv, desc, 1);
				mv.visitMethodInsn(INVOKEINTERFACE, Utils.getInterfaceName(slashedname), "___init___", desc2);
				mv.visitInsn(RETURN);
				mv.visitMaxs(3, Utils.getParameterCount(desc) + 1);
				mv.visitEnd();
			}
		}

		private void createManagedConstructors() {
			String slashedSupertypeName = typeDescriptor.getSupertypeName();
			if (slashedSupertypeName.equals("java/lang/Enum")) { // assert isEnum
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/String;ILorg/springsource/loaded/C;)V", null,
						null);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitVarInsn(ILOAD, 2);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V");
				mv.visitInsn(RETURN);
				mv.visitMaxs(3, 3);
				mv.visitEnd();
				return;
			}

			if (slashedSupertypeName.equals("groovy/lang/Closure")) { // assert this is a closure
				// create the special constructor we want to use, not the one below
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>",
						"(Ljava/lang/Object;Ljava/lang/Object;Lorg/springsource/loaded/C;)V", null, null);
				TypeDescriptor superDescriptor = rtype.getTypeRegistry().getDescriptorFor(supertypeName);
				MethodMember ctor = superDescriptor.getConstructor("(Ljava/lang/Object;Ljava/lang/Object;)V");
				if (ctor == null) {
					throw new IllegalStateException("Unable to find expected constructor on Closure type");
				}
				mv.visitVarInsn(ALOAD, 0); // this (uninitialized)
				mv.visitVarInsn(ALOAD, 1); // 'owner'
				mv.visitVarInsn(ALOAD, 2); // 'this'
				mv.visitMethodInsn(INVOKESPECIAL, "groovy/lang/Closure", "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
				mv.visitInsn(RETURN);
				mv.visitMaxs(3, 4);
				mv.visitEnd();
			} else {
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lorg/springsource/loaded/C;)V", null, null);
				mv.visitVarInsn(ALOAD, 0);
				if (slashedSupertypeName.equals("java/lang/Object")) {
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
					mv.visitInsn(RETURN);
					mv.visitMaxs(1, 2);
				} else if (slashedSupertypeName.equals("java/lang/Enum")) { // assert isEnum
					// Call Enum.<init>(null,0)
					mv.visitInsn(ACONST_NULL);
					mv.visitLdcInsn(0);
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V");
					mv.visitInsn(RETURN);
					mv.visitMaxs(3, 3);
				} else {
					ReloadableType superRtype = rtype.getTypeRegistry().getReloadableType(slashedSupertypeName);
					if (superRtype == null) {
						// This means we are crossing a reloadable boundary (this type is reloadable, the supertype is not).
						// At this point we may be in trouble.  The only time we'll be OK is if the supertype declares a no-arg lructor
						// we can see from here
						TypeDescriptor superDescriptor = rtype.getTypeRegistry().getDescriptorFor(supertypeName);
						MethodMember ctor = superDescriptor.getConstructor("()V");
						if (ctor != null) {
							mv.visitMethodInsn(INVOKESPECIAL, slashedSupertypeName, "<init>", "()V");
						} else {
							String warningMessage = "SERIOUS WARNING (current limitation): At reloadable boundary of "
									+ typeDescriptor.getDottedName()
									+ " supertype="
									+ supertypeName
									+ " - no available default ctor for that supertype, problems may occur on reload if constructor changes";
							if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.SEVERE)) {
								log.log(Level.SEVERE, warningMessage);
							}
							// suppress for closure subtypes
							if (!(supertypeName.equals("groovy/lang/Closure") || supertypeName
									.startsWith("org/codehaus/groovy/runtime/callsite"))) {
								if (GlobalConfiguration.verboseMode) {
									System.out.println(warningMessage);
								}
							}
							// TODO [verify] running enumtests we see that there is an issue here with leaving the special constructor without a super call in it.
							// this will fail verification.

							// throw new IllegalStateException("at reloadable boundary, not sure how to construct " + supertypeName);
						}
					} else {
						mv.visitInsn(ACONST_NULL);
						mv.visitMethodInsn(INVOKESPECIAL, slashedSupertypeName, "<init>", "(Lorg/springsource/loaded/C;)V");
					}
					mv.visitInsn(RETURN);
					mv.visitMaxs(2, 2);
				}
				mv.visitEnd();
			}

		}

		/**
		 * Create the static field setter method. Looks a bit like this:
		 * <p>
		 * <code><pre>
		 * public static r$sets(Object newValue, String name) {
		 *   if (r$fields==null) {
		 *     r$fields = new SSMgr();
		 *   }
		 *   r$fields.setValue(reloadableTypeInstance, newValue, name);
		 * }
		 * </pre></code>
		 */
		private void createStaticFieldSetterMethod() {
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC_STATIC, mStaticFieldSetterName, mStaticFieldSetterDescriptor, null, null);
			mv.visitFieldInsn(GETSTATIC, slashedname, fStaticFieldsName, lStaticStateManager);
			Label l2 = new Label();
			mv.visitJumpInsn(IFNONNULL, l2);
			mv.visitTypeInsn(NEW, tStaticStateManager);
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, tStaticStateManager, "<init>", "()V");
			mv.visitFieldInsn(PUTSTATIC, slashedname, fStaticFieldsName, lStaticStateManager);
			mv.visitLabel(l2);
			mv.visitFieldInsn(GETSTATIC, slashedname, fStaticFieldsName, lStaticStateManager);
			mv.visitFieldInsn(GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, tStaticStateManager, "setValue", "(" + lReloadableType
					+ "Ljava/lang/Object;Ljava/lang/String;)V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(4, 2);
			mv.visitEnd();
		}

		/**
		 * Create the instance field getter method. Looks a bit like this:
		 * <p>
		 * <code><pre>
		 * public Object r$get(Object instance, String name) {
		 *   if (this.instanceStateManager==null) {
		 *     this.instanceStateManager = new InstanceStateManager();
		 *   }
		 *   return this.instanceStateManager.getValue(reloadableType,instance,name);
		 * }
		 * </pre></code>
		 */
		private void createInstanceFieldGetterMethod() {
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, mInstanceFieldGetterName, mInstanceFieldGetterDescriptor, null, null);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, slashedname, fInstanceFieldsName, lInstanceStateManager);
			Label l1 = new Label();
			mv.visitJumpInsn(IFNONNULL, l1);
			// instance state manager will only get initialised here if the constructor did not do it
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(NEW, tInstanceStateManager);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
			mv.visitMethodInsn(INVOKESPECIAL, tInstanceStateManager, "<init>",
					"(Ljava/lang/Object;Lorg/springsource/loaded/ReloadableType;)V");
			mv.visitFieldInsn(PUTFIELD, slashedname, fInstanceFieldsName, lInstanceStateManager);
			mv.visitLabel(l1);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, slashedname, fInstanceFieldsName, lInstanceStateManager);
			mv.visitFieldInsn(GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKEVIRTUAL, tInstanceStateManager, "getValue", "(" + lReloadableType
					+ "Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
			mv.visitInsn(ARETURN);
			mv.visitMaxs(4, 3);
			mv.visitEnd();
		}

		private final static int lvThis = 0;

		// TODO [perf] shuffle ordering of value/instance passed in here to speed up things? (see also rewritePUTFIELD) can we reduce/remove swap
		/**
		 * Create a field setter for instance fields, signature of: public void r$set(Object,Object,String)
		 */
		private void createInstanceFieldSetterMethod() {
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, mInstanceFieldSetterName, mInstanceFieldSetterDescriptor, null, null);
			final int lvNewValue = 1;
			final int lvInstance = 2;
			final int lvName = 3;
			mv.visitVarInsn(ALOAD, lvThis);
			mv.visitFieldInsn(GETFIELD, slashedname, fInstanceFieldsName, lInstanceStateManager);
			// Will only get initialised here if the constructor did not do it
			Label l1 = new Label();
			mv.visitJumpInsn(IFNONNULL, l1);
			// Initialise the Instance State Manager
			mv.visitVarInsn(ALOAD, lvThis);
			mv.visitTypeInsn(NEW, tInstanceStateManager);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, lvThis);
			mv.visitFieldInsn(GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
			mv.visitMethodInsn(INVOKESPECIAL, tInstanceStateManager, "<init>",
					"(Ljava/lang/Object;Lorg/springsource/loaded/ReloadableType;)V");
			mv.visitFieldInsn(PUTFIELD, slashedname, fInstanceFieldsName, lInstanceStateManager);
			mv.visitLabel(l1);

			// get the instance state manager object
			mv.visitVarInsn(ALOAD, lvThis);
			mv.visitFieldInsn(GETFIELD, slashedname, fInstanceFieldsName, lInstanceStateManager);

			// get the reloadable type
			mv.visitFieldInsn(GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);

			// TODO [perf] do we need to pass reloadabletype? Shouldn't the ISMgr instance know it!
			// call setValue
			mv.visitVarInsn(ALOAD, lvInstance);
			mv.visitVarInsn(ALOAD, lvNewValue);
			mv.visitVarInsn(ALOAD, lvName);
			// setValue(ReloadableType rtype, Object instance, Object value, String name) 
			mv.visitMethodInsn(INVOKEVIRTUAL, tInstanceStateManager, "setValue", "(" + lReloadableType
					+ "Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(4, 4);
			mv.visitEnd();
		}

		private void createInstanceStateManagerInstance() {
			FieldVisitor f = cw.visitField(ACC_PUBLIC | ACC_TRANSIENT, fInstanceFieldsName, lInstanceStateManager, null, null);
			f.visitEnd();
		}

		private void createStaticStateManagerInstance() {
			FieldVisitor f = cw.visitField(ACC_PUBLIC_STATIC /*| ACC_TRANSIENT*/| ACC_FINAL, fStaticFieldsName,
					lStaticStateManager, null, null);
			f.visitEnd();
		}

		/**
		 * Create the reloadable type field, which can later answer questions about changes or be used to access the latest version
		 * of a type/method.
		 */
		private void createReloadableTypeField() {
			int acc = isInterface ? ACC_PUBLIC_STATIC_FINAL : ACC_PUBLIC_STATIC; //ACC_PRIVATE_STATIC;
			FieldVisitor fv = cw.visitField(acc, fReloadableTypeFieldName, lReloadableType, null, null);
			fv.visitEnd();
		}

		@Override
		public MethodVisitor visitMethod(int flags, String name, String descriptor, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(promoteIfNecessary(flags,name), name, descriptor, signature, exceptions);
			MethodVisitor newMethodVisitor = getMethodVisitor(name, descriptor, mv);
			return newMethodVisitor;
		}

		public MethodVisitor getMethodVisitor(String name, String descriptor, MethodVisitor mv) {
			MethodVisitor newMethodVisitor = null;
			if (name.charAt(0) == '<') {
				if (name.charAt(1) == 'c') { // <clinit>
					clinitDone = true;
					newMethodVisitor = new MethodPrepender(mv, new ClinitPrepender(mv));
				} else { // <init>
					newMethodVisitor = new AugmentingConstructorAdapter(mv, descriptor, slashedname, isTopmostReloadable());
					// want to create a copy of the constructor called ___init___ so it is reachable from the executor of the subtype.
					// All it really needs to do is call through the dispatcher to the executor for the relevant constructor.
					// this will force a reload of the supertype (to create the super executor) - that is something we can address later
				}
			} else {
				// what about just copying if isgroovy and name.startsWith("$get");
				// Can't do this for $getStaticMetaClass so let us use $get$$ for now, until we discover why that lets us down...
				//				if (isGroovy && name.startsWith("$get$$")) {
				//					newMethodVisitor = new MethodAdapter(mv);
				//				} else {
				newMethodVisitor = new AugmentingMethodAdapter(mv, name, descriptor);
				//				}
			}
			return newMethodVisitor;
		}

		// Default visibility elements need promotion to public so that they can be seen from the executor
		private int promoteIfNecessary(int flags,String name) {
			int newflags = Utils.promoteDefaultOrProtectedToPublic(flags, isEnum, name);
			return newflags;
		}

		@Override
		public FieldVisitor visitField(final int access, final String name, final String desc, final String signature,
				final Object value) {
			// Special casing here for serialVersionUID - not removing the final modifier.  This enables
			// the code in java.io.ObjectStreamClass to access it.
			int modAccess = 0;
			if ((access & ACC_FINAL) != 0) {
				if (name.equals("serialVersionUID")) {
					modAccess = (access & ~(ACC_PRIVATE | ACC_PROTECTED)) | ACC_PUBLIC;
				} else {
					// remove final
					modAccess = (access & ~(ACC_PRIVATE | ACC_PROTECTED)) | ACC_PUBLIC;
					modAccess = modAccess & ~ACC_FINAL;
				}
			} else {
				// remove final
				modAccess = (access & ~(ACC_PRIVATE | ACC_PROTECTED)) | ACC_PUBLIC;
				modAccess = modAccess & ~ACC_FINAL;
			}
			return cv.visitField(modAccess, name, desc, signature, value);
		}

		static class FieldHolder {
			final int access;
			final String name;
			final String desc;

			public FieldHolder(int access, String name, String desc) {
				this.access = access;
				this.name = name;
				this.desc = desc;
			}
		}

		@Override
		public void visitEnd() {
			if (!clinitDone) {
				// Need to add a static initializer to initialize the reloadable type field
				MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
				new ClinitPrepender(mv).prepend();
				mv.visitInsn(RETURN);
				mv.visitMaxs(3, 0);
				mv.visitEnd();
			}
			generateCatchers();
			generateDynamicDispatchHandler();
			cv.visitEnd();
		}

		/**
		 * Create a basic dynamic dispatch handler. To support changes to interfaces, a new method is added to all reloadable
		 * interfaces and this needs an implementation. This method generates the implementation which delegates to the reloadable
		 * interface for the type. As interfaces can't get static methods we only have to worry about instance methods here.
		 */
		private void generateDynamicDispatchHandler() {
			final int indexThis = 0;
			final int indexArgs = 1;
			final int indexTargetInstance = 2;
			final int indexNameAndDescriptor = 3;

			if (isInterface) {
				cw.visitMethod(ACC_PUBLIC_ABSTRACT, mDynamicDispatchName, mDynamicDispatchDescriptor, null, null);
			} else {
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, mDynamicDispatchName, mDynamicDispatchDescriptor, null, null);

				// Sometimes we come into the dynamic dispatcher because we are handling an INVOKEINTERFACE for a method
				// not defined on the original interface.  In these cases we will find that fetchLatest() returns null because
				// the interface was reloaded, but the implementation was not (necessarily).  Should we force it to reload
				// the implementation?  That would generate the correct dispatcher...

				// 1. Ask the reloadable type for the latest version of the interface
				//    __DynamicallyDispatchable dispatchable = r$type.determineDispatcher(this,nameAndDescriptor)
				mv.visitFieldInsn(Opcodes.GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
				mv.visitVarInsn(ALOAD, indexThis);
				mv.visitVarInsn(ALOAD, indexNameAndDescriptor);
				mv.visitMethodInsn(INVOKEVIRTUAL, tReloadableType, "determineDispatcher",
						"(Ljava/lang/Object;Ljava/lang/String;)Lorg/springsource/loaded/__DynamicallyDispatchable;");

				mv.visitInsn(DUP);
				Label l1 = new Label();
				mv.visitJumpInsn(IFNULL, l1);
				{
					// 2. package up the parameters
					// can I assert that 0==2 - ie. the instance being called upon is the same as the parameter passed in
					mv.visitVarInsn(ALOAD, indexArgs);
					mv.visitVarInsn(ALOAD, indexThis);
					mv.visitVarInsn(ALOAD, indexNameAndDescriptor);

					// 3. call it
					//    return dispatchable.__execute(parameters,this,nameAndDescriptor)
					mv.visitMethodInsn(INVOKEINTERFACE, "org/springsource/loaded/__DynamicallyDispatchable", mDynamicDispatchName,
							mDynamicDispatchDescriptor);
					mv.visitInsn(ARETURN);
				}
				mv.visitLabel(l1);
				mv.visitInsn(POP); // POPNULL

				// delegate to the superclass dynamic dispatch method
				//				mv.visitVarInsn(ALOAD, 0);
				//				mv.visitVarInsn(ALOAD, 1);
				//				mv.visitVarInsn(ALOAD, 2);
				//				mv.visitVarInsn(ALOAD, 3);
				//				mv.visitMethodInsn(INVOKESPECIAL, supertypeName, mDynamicDispatchName, mDynamicDispatchDescriptor);
				//				mv.visitInsn(ARETURN);

				// 4. throw NoSuchMethodError
				genThrowNoSuchMethodError(mv, indexNameAndDescriptor);
				mv.visitMaxs(5, 4);
				mv.visitEnd();
			}
		}

		/**
		 * @param mv where to append the instructions
		 * @param index the index of the String message to load and use in the exception
		 */
		private final void genThrowNoSuchMethodError(MethodVisitor mv, int index) {
			mv.visitTypeInsn(NEW, "java/lang/NoSuchMethodError");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, index);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoSuchMethodError", "<init>", "(Ljava/lang/String;)V");
			mv.visitInsn(ATHROW);
		}

		/**
		 * Catcher methods are 'empty' methods added to subtypes to 'catch' any virtual dispatch calls that would otherwise be
		 * missed. Catchers then check to see if the type on which they are defined now provides an implementation of the method in
		 * question - if it does then it is called, otherwise the catcher simply calls the supertype.
		 * <p>
		 * Catchers typically have the same visibility as the methods for which they exist, unless those methods are
		 * protected/default, in which case the catcher is made public. This enables them to be seen from the executor.
		 */
		private void generateCatchers() {
			if (!GlobalConfiguration.catchersOn || isInterface) {
				return;
			}
			FieldMember[] fms = typeDescriptor.getFieldsRequiringAccessors();

			for (FieldMember field : fms) {
				createProtectedFieldGetterSetter(field);
			}
			
			MethodMember[] methods = typeDescriptor.getMethods();

			for (MethodMember method: methods) {
				if (!MethodMember.isSuperDispatcher(method)) {
					continue;
				}
				// TODO topmost test?
				String name = method.getName();
				String descriptor = method.getDescriptor();
				if (GlobalConfiguration.verboseMode && log.isLoggable(Level.FINEST)) {
					log.finest("Creating super dispatcher for method "+name+descriptor+" in type "+slashedname);
				}
				// Create a superdispatcher for this method
				MethodVisitor mv = cw.visitMethod(Modifier.PUBLIC, method.getName(), method.getDescriptor(), null, method.getExceptions());
				int ps = Utils.getParameterCount(method.getDescriptor());
				ReturnType methodReturnType = Utils.getReturnTypeDescriptor(method.getDescriptor());
				int lvarIndex = 0;
				mv.visitVarInsn(ALOAD, lvarIndex++); // load this
				Utils.createLoadsBasedOnDescriptor(mv, descriptor, lvarIndex);
				String targetMethod = method.getName().substring(0,method.getName().lastIndexOf("_$"));
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL,typeDescriptor.getSupertypeName(),targetMethod,method.getDescriptor());
				Utils.addCorrectReturnInstruction(mv, methodReturnType, false);
				int maxs = ps + 1;
				if (methodReturnType.isDoubleSlot()) {
					maxs++;
				}
				mv.visitMaxs(maxs, maxs);
				mv.visitEnd();
			}
			
			for (MethodMember method : methods) {
				if (!MethodMember.isCatcher(method)) {
					continue;
				}
				String name = method.getName();
				String descriptor = method.getDescriptor();
				ReturnType returnType = Utils.getReturnTypeDescriptor(descriptor);

				// 1. Create the method signature
				int flags = method.getModifiers();
				if (Modifier.isProtected(flags)) {
					flags = flags - Modifier.PROTECTED + Modifier.PUBLIC;
				}
				MethodVisitor mv = cw.visitMethod(flags, method.getName(), method.getDescriptor(), null, method.getExceptions());

				// 2. Ask the type if anything has changed from first load
				mv.visitFieldInsn(Opcodes.GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
				mv.visitLdcInsn(method.getId());
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tReloadableType, "fetchLatestIfExists", "(I)Ljava/lang/Object;");

				// If the return value is null, there is no implementation
				mv.visitInsn(DUP);

				// 3. create the if statement
				Label l1 = new Label();
				mv.visitJumpInsn(Opcodes.IFNULL, l1);

				// 4. if changed then call the interface to run whatever version has been added
				mv.visitTypeInsn(CHECKCAST, Utils.getInterfaceName(slashedname));

				int lvarIndex = 0;
				mv.visitVarInsn(ALOAD, lvarIndex++); // load this
				Utils.createLoadsBasedOnDescriptor(mv, descriptor, lvarIndex);
				String desc = new StringBuffer("(L").append(slashedname).append(";").append(descriptor.substring(1)).toString();
				mv.visitMethodInsn(INVOKEINTERFACE, Utils.getInterfaceName(slashedname), name, desc);
				Utils.addCorrectReturnInstruction(mv, returnType, true);

				// 5. if unchanged just run the supertype version (could be another catcher...)
				mv.visitLabel(l1);
				mv.visitInsn(POP);
				int ps = Utils.getParameterCount(method.getDescriptor());
				ReturnType methodReturnType = Utils.getReturnTypeDescriptor(method.getDescriptor());

				// A catcher for an interface method is inserted into abstract classes.  These should never be reached unless
				// they now provide an implementation (on a reload) and the subtype has deleted the implementation it had.
				// This means there is never a need to call 'super' in the logic below and getting here when there isn't
				// something to run in the executor is a bug (so we throw AbstractMethodError)
				if (MethodMember.isCatcherForInterfaceMethod(method)) {
					mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
					mv.visitInsn(DUP);
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/AbstractMethodError", "<init>", "()V");
					mv.visitInsn(ATHROW);
				} else {
					mv.visitVarInsn(ALOAD, 0); // load this
					Utils.createLoadsBasedOnDescriptor(mv, method.getDescriptor(), 1);
					mv.visitMethodInsn(INVOKESPECIAL, supertypeName, method.getName(), method.getDescriptor());
					Utils.addCorrectReturnInstruction(mv, methodReturnType, false);
				}

				int maxs = ps + 1;
				if (methodReturnType.isDoubleSlot()) {
					maxs++;
				}
				mv.visitMaxs(maxs, maxs);
				mv.visitEnd();
			}

		}

		private void insertCorrectLoad(MethodVisitor mv, ReturnType rt, int slot) {
			if (rt.isPrimitive()) {
				switch (rt.descriptor.charAt(0)) {
				case 'Z':
				case 'S':
				case 'I':
				case 'B':
				case 'C':
					mv.visitVarInsn(ILOAD, slot);
					break;
				case 'F':
					mv.visitVarInsn(FLOAD, slot);
					break;
				case 'J':
					mv.visitVarInsn(LLOAD, slot);
					break;
				case 'D':
					mv.visitVarInsn(DLOAD, slot);
					break;
				default:
					throw new IllegalStateException(rt.descriptor);
				}
			} else {
				mv.visitVarInsn(ALOAD, slot);
			}
		}

		/**
		 * For the fields that need it (protected fields from a non-reloadable supertype), create the getters and setters so that
		 * the executor can read/write them.
		 * 
		 */
		private void createProtectedFieldGetterSetter(FieldMember field) {
			String descriptor = field.descriptor;
			String name = field.name;
			ReturnType rt = ReturnType.getReturnType(descriptor);
			if (field.isStatic()) {
				MethodVisitor mv = cw.visitMethod(Modifier.PUBLIC | Modifier.STATIC, Utils.getProtectedFieldGetterName(name), "()"
						+ descriptor, null, null);
				mv.visitFieldInsn(GETSTATIC, slashedname, name, descriptor);
				Utils.addCorrectReturnInstruction(mv, rt, false);
				mv.visitMaxs(rt.isDoubleSlot() ? 2 : 1, 0);
				mv.visitEnd();

				mv = cw.visitMethod(Modifier.PUBLIC | Modifier.STATIC, Utils.getProtectedFieldSetterName(name), "(" + descriptor
						+ ")V", null, null);
				insertCorrectLoad(mv, rt, 0);
				mv.visitFieldInsn(PUTSTATIC, slashedname, name, descriptor);
				mv.visitInsn(RETURN);
				mv.visitMaxs(rt.isDoubleSlot() ? 2 : 1, 1);
				mv.visitEnd();
			} else {
				MethodVisitor mv = cw.visitMethod(Modifier.PUBLIC, Utils.getProtectedFieldGetterName(name), "()" + descriptor,
						null, null);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, slashedname, name, descriptor);
				Utils.addCorrectReturnInstruction(mv, rt, false);
				mv.visitMaxs(rt.isDoubleSlot() ? 2 : 1, 1);
				mv.visitEnd();

				mv = cw.visitMethod(Modifier.PUBLIC, Utils.getProtectedFieldSetterName(name), "(" + descriptor + ")V", null, null);
				mv.visitVarInsn(ALOAD, 0);
				insertCorrectLoad(mv, rt, 1);
				mv.visitFieldInsn(PUTFIELD, slashedname, name, descriptor);
				mv.visitInsn(RETURN);
				mv.visitMaxs(rt.isDoubleSlot() ? 3 : 2, 2);
				mv.visitEnd();
			}
		}

		/**
		 * The ClinitPrepender adds the code to initialize the reloadable type field at class load
		 */
		class ClinitPrepender implements Prepender, Constants {

			MethodVisitor mv;

			ClinitPrepender(MethodVisitor mv) {
				this.mv = mv;
			}

			public void prepend() {
				// Discover the ReloadableType object and store it here
				mv.visitCode();
				// TODO optimization: could collapse ints into one but this snippet isn't put in many places
				mv.visitLdcInsn(rtype.getTypeRegistryId());
				mv.visitLdcInsn(rtype.getId());
				mv.visitMethodInsn(INVOKESTATIC, tRegistryType, "getReloadableType", "(II)" + lReloadableType);
				mv.visitFieldInsn(PUTSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
				//				mv.visitFieldInsn(GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
				//				mv.visitLdcInsn(Type.getObjectType(rtype.getSlashedSupertypeName()));//Type("L" + rtype.getSlashedSupertypeName() + ";")); // faster way?
				//				mv.visitMethodInsn(INVOKEVIRTUAL, tReloadableType, "setSuperclass", "(Ljava/lang/Class;)V");

				// only in the top most type - what about interfaces??
				if (GlobalConfiguration.fieldRewriting) {
					mv.visitFieldInsn(GETSTATIC, slashedname, fStaticFieldsName, lStaticStateManager);
					Label l1 = new Label();
					mv.visitJumpInsn(IFNONNULL, l1);
					mv.visitTypeInsn(NEW, tStaticStateManager);
					mv.visitInsn(DUP);
					mv.visitMethodInsn(INVOKESPECIAL, tStaticStateManager, "<init>", "()V");
					mv.visitFieldInsn(PUTSTATIC, slashedname, fStaticFieldsName, lStaticStateManager);
					mv.visitLabel(l1);
				}
				// If the static initializer has changed, call the new version through the ___clinit___ method

				mv.visitFieldInsn(Opcodes.GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tReloadableType, "clinitchanged", "()I");
				// 2. Create the if statement
				Label wasZero = new Label();
				mv.visitJumpInsn(Opcodes.IFEQ, wasZero); // if == 0, jump to where we can do the original thing

				// 3. grab the latest dispatcher and call it through the interface
				mv.visitFieldInsn(Opcodes.GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
				mv.visitMethodInsn(INVOKEVIRTUAL, tReloadableType, "fetchLatest", "()Ljava/lang/Object;");
				mv.visitTypeInsn(CHECKCAST, Utils.getInterfaceName(slashedname));
				mv.visitMethodInsn(INVOKEINTERFACE, Utils.getInterfaceName(slashedname), mStaticInitializerName, "()V");
				mv.visitInsn(RETURN);
				// 4. do what you were going to do anyway
				mv.visitLabel(wasZero);
			}
		}

		/**
		 * Rewrites a method to include the extra checks to verify it is the most up to date version.
		 */
		class AugmentingMethodAdapter extends MethodVisitor implements Opcodes {

			int methodId;
			String name;
			String descriptor;
			MethodMember method;
			ReturnType returnType;

			public AugmentingMethodAdapter(MethodVisitor mv, String name, String descriptor) {
				super(ASM5,mv);
				this.name = name;
				this.method = rtype.getMethod(name, descriptor);
				this.methodId = method.getId();
				this.descriptor = descriptor;
				this.returnType = Utils.getReturnTypeDescriptor(descriptor);
			}

			@Override
			public void visitCode() {
				super.visitCode();
				boolean isStaticMethod = method.isStatic();
				// 1. ask the reloadable type if anything has changed since initial load by
				//    calling 'int changed(int)' passing in the method ID
				//     0 if the method cannot have changed
				//     1 if the method has changed
				//     2 if the method has been deleted in a new version
				mv.visitFieldInsn(Opcodes.GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
				mv.visitLdcInsn(methodId);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tReloadableType, "changed", "(I)I");
				// 2. Create the if statement
				Label wasZero = new Label();
				if (!isStaticMethod) {
					mv.visitInsn(DUP);
				}
				mv.visitJumpInsn(Opcodes.IFEQ, wasZero); // if == 0, jump to where we can do the original thing

				// TODO if it isStatic then the invoke side should be pointing at the right version - do we need to cope with it not?
				if (!isStaticMethod) {
					Label wasOne = new Label();
					mv.visitInsn(ICONST_1);
					mv.visitJumpInsn(IF_ICMPEQ, wasOne); // if == 1, method has changed
					// If here, == 2, so method has been deleted
					// either try an invokespecial on a super or throw a NoSuchmethodError

					// Only worth including super.xxx() call if the supertype does define it or the supertype is reloadable
					// Otherwise we will generate invokespecial 'Object.somethingThatCantBeThere' in some cases
					TypeDescriptor superDescriptor = rtype.getTypeRegistry().getDescriptorFor(supertypeName);
					if (!superDescriptor.definesNonPrivate(name + descriptor)) {
						insertThrowNoSuchMethodError();
					} else {
						insertInvokeSpecialToCallSuperMethod();
					}
					mv.visitLabel(wasOne);
				}
				// 3. grab the latest dispatcher and call it through the interface
				mv.visitFieldInsn(Opcodes.GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
				mv.visitMethodInsn(INVOKEVIRTUAL, tReloadableType, "fetchLatest", "()Ljava/lang/Object;");
				mv.visitTypeInsn(CHECKCAST, Utils.getInterfaceName(slashedname));
				int lvarIndex = 0;
				if (!isStaticMethod) {
					mv.visitVarInsn(ALOAD, lvarIndex++);
				} else {
					mv.visitInsn(ACONST_NULL);
				}
				Utils.createLoadsBasedOnDescriptor(mv, descriptor, lvarIndex);
				String desc = new StringBuilder("(L").append(slashedname).append(";").append(descriptor.substring(1)).toString();
				if (method.isStatic() && MethodMember.isClash(method)) {
					name = "__" + name;
				}
				mv.visitMethodInsn(INVOKEINTERFACE, Utils.getInterfaceName(slashedname), name, desc);
				Utils.addCorrectReturnInstruction(mv, returnType, true);
				// 4. do what you were going to do anyway
				mv.visitLabel(wasZero);
				if (!isStaticMethod) {
					mv.visitInsn(POP);
				}
			}

			private void insertInvokeSpecialToCallSuperMethod() {
				int lvarIndex = 0;
				if (!Modifier.isStatic(method.getModifiers())) {
					mv.visitVarInsn(ALOAD, lvarIndex++); // load this
				}
				Utils.createLoadsBasedOnDescriptor(mv, descriptor, lvarIndex);
				mv.visitMethodInsn(INVOKESPECIAL, supertypeName, name, descriptor);
				Utils.addCorrectReturnInstruction(mv, Utils.getReturnTypeDescriptor(descriptor), false);
			}

			private void insertThrowNoSuchMethodError() {
				// exception text should look like: a.b.c.B.foo()V
				String text = rtype.getName() + "." + name.replace('/', '.') + descriptor;
				mv.visitTypeInsn(NEW, "java/lang/NoSuchMethodError");
				mv.visitInsn(DUP);
				mv.visitLdcInsn(text);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoSuchMethodError", "<init>", "(Ljava/lang/String;)V");
				mv.visitInsn(ATHROW);
			}

		}

		class AugmentingConstructorAdapter extends MethodVisitor implements Opcodes {

			int ctorId;
			String name;
			String descriptor;
			MethodMember method;
			String type;
			boolean isTopMost;

			public AugmentingConstructorAdapter(MethodVisitor mv, String descriptor, String type, boolean isTopMost) {
				super(ASM5,mv);
				this.descriptor = descriptor;
				this.type = type;
				this.isTopMost = isTopMost;
			}

			@Override
			public void visitCode() {
				super.visitCode();

				// 1. Quick check if anything has changed
				mv.visitFieldInsn(Opcodes.GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
				mv.visitLdcInsn(ctorId);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, tReloadableType, "cchanged", "(I)Ljava/lang/Object;");
				// return value of that call is the dispatcher to call, or null if we shouldn't do anything different

				// TODO throw exception if no longer defined? (Shouldn't really be called if not defined though...)

				// 2. if nothing has changed jump to the end and run the original code
				Label wasNull = new Label();
				mv.visitInsn(DUP);
				mv.visitJumpInsn(Opcodes.IFNULL, wasNull); // if == null

				mv.visitTypeInsn(CHECKCAST, Utils.getInterfaceName(slashedname));
				//				mv.visitInsn(SWAP);
				mv.visitVarInsn(ALOAD, 0);
				//				mv.visitInsn(DUP);
				// Now we have the dispatcher on the stack for our type, we can't pass 'this' (aload_0) on a call yet because it is not initialized
				// have to initialize it now before we can pass it on
				//				mv.visitMethodInsn(INVOKESPECIAL, slashedname, "<init>", "(Lorg/springsource/loaded/ReloadableType;)V");
				//				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
				//				mv.visitMethodInsn(INVOKESPECIAL, owner, name, desc)
				if (isEnum) {
					mv.visitVarInsn(ALOAD, 1);
					mv.visitVarInsn(ILOAD, 2);
					mv.visitInsn(ACONST_NULL);
					mv.visitMethodInsn(INVOKESPECIAL, slashedname, "<init>", "(Ljava/lang/String;ILorg/springsource/loaded/C;)V");
				} else {
					mv.visitInsn(ACONST_NULL);
					mv.visitMethodInsn(INVOKESPECIAL, slashedname, "<init>", "(Lorg/springsource/loaded/C;)V");
				}

				// initialize the field instance manager
				// not conditional on isTopMost because entering object construction through this route won't ever call the initialization logic in
				// the super ctor, because all that is bypassed.  We could put this initialization logic into the topmost 'special' constructor
				// that we create, that is an alternative
				if (GlobalConfiguration.fieldRewriting) {// && isTopMost) {
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, slashedname, fInstanceFieldsName, lInstanceStateManager);
					Label l1 = new Label();
					mv.visitJumpInsn(IFNONNULL, l1);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitTypeInsn(NEW, tInstanceStateManager);
					mv.visitInsn(DUP);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(Opcodes.GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
					mv.visitMethodInsn(INVOKESPECIAL, tInstanceStateManager, "<init>",
							"(Ljava/lang/Object;Lorg/springsource/loaded/ReloadableType;)V");
					mv.visitFieldInsn(PUTFIELD, slashedname, fInstanceFieldsName, lInstanceStateManager);
					mv.visitLabel(l1);
				}

				mv.visitVarInsn(ALOAD, 0);
				Utils.createLoadsBasedOnDescriptor(mv, descriptor, 1);
				// from "(Ljava/lang/String;J)V" to "(Ljava/lang/String;J)Lcom/foo/A;"
				//				String desc = new StringBuilder().append(descriptor, 0, descriptor.length() - 1).append("L").append(slashedname)
				//						.append(";").toString();

				//				mv.visitMethodInsn(INVOKEVIRTUAL,"")

				String desc = new StringBuilder("(L").append(slashedname).append(";").append(descriptor.substring(1)).toString();

				//				mv.visitVarInsn(ALOAD, 0);
				//				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

				mv.visitMethodInsn(INVOKEINTERFACE, Utils.getInterfaceName(slashedname), "___init___", desc);
				mv.visitInsn(RETURN);
				//				mv.visitTypeInsn(CHECKCAST, type);

				// 4. do what you were going to do anyway
				mv.visitLabel(wasNull);
				mv.visitInsn(POP);
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
			public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
				super.visitMethodInsn(opcode, owner, name, desc);
				if (opcode == INVOKESPECIAL) {
					unitializedObjectsCount--;
				}
				if (unitializedObjectsCount == -1 && isTopMost) {
					// Need to insert this after the relevant invokespecial
					if (GlobalConfiguration.fieldRewriting) {
						mv.visitVarInsn(ALOAD, 0);
						mv.visitFieldInsn(GETFIELD, slashedname, fInstanceFieldsName, lInstanceStateManager);
						Label l1 = new Label();
						mv.visitJumpInsn(IFNONNULL, l1);
						mv.visitVarInsn(ALOAD, 0);
						mv.visitTypeInsn(NEW, tInstanceStateManager);
						mv.visitInsn(DUP);
						mv.visitVarInsn(ALOAD, 0);
						mv.visitFieldInsn(Opcodes.GETSTATIC, slashedname, fReloadableTypeFieldName, lReloadableType);
						mv.visitMethodInsn(INVOKESPECIAL, tInstanceStateManager, "<init>",
								"(Ljava/lang/Object;Lorg/springsource/loaded/ReloadableType;)V");
						//						mv.visitMethodInsn(INVOKESPECIAL, tInstanceStateManager, "<init>", "(Ljava/lang/Object;)V");
						mv.visitFieldInsn(PUTFIELD, slashedname, fInstanceFieldsName, lInstanceStateManager);
						mv.visitLabel(l1);
					}
				}
			}

		}

		interface Prepender {
			void prepend();
		}

		class MethodPrepender extends MethodVisitor implements Opcodes {

			Prepender appender;

			public MethodPrepender(MethodVisitor mv, Prepender appender) {
				super(ASM5,mv);
				this.appender = appender;
			}

			@Override
			public void visitCode() {
				super.visitCode();
				appender.prepend();
			}

		}

	}

}