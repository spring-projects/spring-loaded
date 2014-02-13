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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Extract an interface for a type. The interface embodies the shape of the type as originally loaded. The key difference with
 * methods in the interface is that they contain an extra (leading) parameter that is the type of the original loaded class.<br>
 * For example:<br>
 * 
 * <tt> <pre>
 * class Foo {
 * public String foo(int i) {}
 * }
 * </pre></tt>
 * 
 * will cause creation of an interface method:
 * 
 * <tt> <pre>
 * String foo(Foo instance, int i) {}
 * </pre></tt>
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class InterfaceExtractor {

	@SuppressWarnings("unused")
	private TypeRegistry registry;

	public InterfaceExtractor(TypeRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Extract the fixed interface for a class and a type descriptor with more details on the methods
	 */
	public static byte[] extract(byte[] classbytes, TypeRegistry registry, TypeDescriptor typeDescriptor) {
		return new InterfaceExtractor(registry).extract(classbytes, typeDescriptor);
	}

	public byte[] extract(byte[] classbytes, TypeDescriptor typeDescriptor) {
		ClassReader fileReader = new ClassReader(classbytes);
		ExtractorVisitor extractorVisitor = new ExtractorVisitor(typeDescriptor);
		fileReader.accept(extractorVisitor, 0);
		return extractorVisitor.getBytes();
	}

	class ExtractorVisitor extends ClassVisitor implements Constants {

		private TypeDescriptor typeDescriptor;
		private ClassWriter interfaceWriter = new ClassWriter(0);
		private String slashedtypename;

		public ExtractorVisitor(TypeDescriptor typeDescriptor) {
			super(ASM5);
			this.typeDescriptor = typeDescriptor;
		}

		public byte[] getBytes() {
			return interfaceWriter.toByteArray();
		}

		public void visit(int version, int flags, String name, String signature, String superclassName, String[] interfaceNames) {
			// Create interface "public interface [typename]__I {"
			interfaceWriter.visit(version, ACC_PUBLIC_INTERFACE, Utils.getInterfaceName(name), null, "java/lang/Object", null);
			this.slashedtypename = name;
		}

		public MethodVisitor visitMethod(int flags, String name, String descriptor, String signature, String[] exceptions) {
			// TODO should we special case statics (and not have them require an extra leading param)?
			if (isClinitOrInit(name)) {
				if (name.charAt(1) != 'c') { // avoid <clinit>
					// It is a constructor
					String newDescriptor = createDescriptorWithPrefixedParameter(descriptor);
					// Need a modified name
					name = "___init___";
					interfaceWriter.visitMethod(ACC_PUBLIC_ABSTRACT, name, newDescriptor, signature, exceptions);
				}
			} else {
				String newDescriptor = createDescriptorWithPrefixedParameter(descriptor);
				// generic signature is erased
				MethodMember method = typeDescriptor.getByDescriptor(name, descriptor);
				if (MethodMember.isClash(method)) {
					name = "__" + name;
				}
				interfaceWriter.visitMethod(ACC_PUBLIC_ABSTRACT, name, newDescriptor, null, exceptions);
			}
			return null;
		}

		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			return null;
		}

		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			return null;
		}

		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			// nothing to do
		}

		public void visitOuterClass(String owner, String name, String desc) {
			// nothing to do
		}

		public void visitSource(String source, String debug) {
			// nothing to do
		}

		public void visitAttribute(Attribute attr) {
			// nothing to do
		}

		public void visitEnd() {
			// Must add a method on the interface for the dynamic invocation method
			String descriptor = mDynamicDispatchDescriptor;
			interfaceWriter.visitMethod(ACC_PUBLIC_ABSTRACT, mDynamicDispatchName, descriptor, null, null);
			interfaceWriter.visitMethod(ACC_PUBLIC_ABSTRACT, mStaticInitializerName, "()V", null, null);
			// Go through catchers on the type descriptor and add the methods to the interface
			for (MethodMember method : typeDescriptor.getMethods()) {
				if (!MethodMember.isCatcher(method)) {
					continue;
				}
				descriptor = createDescriptorWithPrefixedParameter(method.getDescriptor());
				interfaceWriter.visitMethod(ACC_PUBLIC_ABSTRACT, method.getName(), descriptor, null, method.getExceptions());
			}
		}

		/**
		 * Modify the descriptor to include a leading parameter of the type of the class being visited. For example: if visiting
		 * type "com.Bar" and hit method "(Ljava/lang/String;)V" then this method will return "(Lcom/Bar;Ljava/lang/String;)V"
		 * 
		 * @return new descriptor with extra leading parameter
		 */
		private String createDescriptorWithPrefixedParameter(String descriptor) {
			StringBuilder newDescriptor = new StringBuilder();
			newDescriptor.append("(L").append(slashedtypename).append(";");
			newDescriptor.append(descriptor, 1, descriptor.length());
			return newDescriptor.toString();
		}

		private boolean isClinitOrInit(String name) {
			return name.charAt(0) == '<';
		}
	}

}
