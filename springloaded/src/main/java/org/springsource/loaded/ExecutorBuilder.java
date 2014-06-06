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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The executor embodies the new implementation of the type after it has been reloaded.
 * <p>
 * The executor is the class full of static methods that looks very like the original class.
 * <p>
 * <b>Methods</b>. For each method in the original type we have a method in the executor, it has the same SourceFile attribute and
 * the same local variable and line number details for debugging to work. Note the first variable will have been renamed from 'this'
 * to 'thiz' to prevent the eclipse debugger crashing. All annotations from the new version will be copied to the methods on an
 * executor.
 * <p>
 * <b>Fields</b>. Fields are copied into the executor but only so that there is a place to hang the annotations off (so that they
 * can be accessed through reflection).
 * <p>
 * <b>Constructors</b>. Constructors are added to the executor as ___init___ methods, with the invokespecials within them
 * transformed, either removed if they are calls to Object.&lt;init&gt; or mutated into ___init___ calls on the supertype instance.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class ExecutorBuilder {

	private TypeRegistry typeRegistry;

	ExecutorBuilder(TypeRegistry typeRegistry) {
		this.typeRegistry = typeRegistry;
	}

	public byte[] createFor(ReloadableType reloadableType, String versionstamp, TypeDescriptor typeDescriptor, byte[] newVersionData) {
		if (typeDescriptor == null) {
			// must be reloadable or we would not be here - so can pass 'true'
			typeDescriptor = typeRegistry.getExtractor().extract(newVersionData, true);
		}
		ClassReader fileReader = new ClassReader(newVersionData);
		ExecutorBuilderVisitor executorVisitor = new ExecutorBuilderVisitor(reloadableType.getSlashedName(), versionstamp,
				typeDescriptor);
		fileReader.accept(executorVisitor, 0);
		return executorVisitor.getBytes();
	}

	/**
	 * ClassVisitor that constructs the executor by visiting the original class. The basic goal is to visit the original class and
	 * 'copy' the methods into the executor, making adjustments as we go.
	 */
	static class ExecutorBuilderVisitor extends ClassVisitor implements Constants {

		private ClassWriter cw = new ClassWriter(0);

		private String classname;
		private String suffix;
		protected TypeDescriptor typeDescriptor;

		public ExecutorBuilderVisitor(String classname, String suffix, TypeDescriptor typeDescriptor) {
			super(ASM5);
			this.classname = classname;
			this.suffix = suffix;
			this.typeDescriptor = typeDescriptor;
		}

		public byte[] getBytes() {
			return cw.toByteArray();
		}

		public void visit(int version, int flags, String name, String signature, String superclassName, String[] interfaceNames) {
			cw.visit(version, Opcodes.ACC_PUBLIC, Utils.getExecutorName(classname, suffix), null, "java/lang/Object", null);
		}

		// For type level annotation copying
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = cw.visitAnnotation(desc, visible);
			return new CopyingAnnotationVisitor(av);
		}

		// Fields are copied solely to provide a place to hang annotations
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			return cw.visitField(access, name, desc, signature, value);
		}

		// For each method, copy it into the new class making appropriate adjustments
		public MethodVisitor visitMethod(int flags, String name, String descriptor, String signature, String[] exceptions) {
			if (!Utils.isInitializer(name)) {
				// method
				if (!Modifier.isStatic(flags)) {
					// For non static methods add the extra initial parameter which is 'this'
					descriptor = Utils.insertExtraParameter(classname, descriptor);
					MethodVisitor mv = cw.visitMethod(ACC_PUBLIC_STATIC, name, descriptor, signature, exceptions);
					return new MethodCopier(mv, typeDescriptor.isInterface(), descriptor, typeDescriptor, classname, suffix);
				} else {
					// If this static method would 'clash' with an instance method that has the extra parameter added then
					// we have a couple of options to make them different:
					// 1. tweak the name
					// 2. tweak the parameters
					MethodMember method = typeDescriptor.getByDescriptor(name, descriptor);
					if (MethodMember.isClash(method)) {
						name = "__" + name;
					}
					MethodVisitor mv = cw.visitMethod(ACC_PUBLIC_STATIC, name, descriptor, signature, exceptions);
					return new MethodCopier(mv, typeDescriptor.isInterface(), descriptor, typeDescriptor, classname, suffix);
				}
			} else {
				// constructor
				if (name.charAt(1) != 'c') {
					// regular constructor
					// want to create the ___init___ handler for this constructor
					
					// With the JDT compiler the inner class constructor gets an extra first parameter that is the type of
					// containing class. But with javac the inner class constructor gets an extra first parameter that is of
					// a special anonymous type (inner class of the containing class)
					// For example:  class Foo { class Bar {}}
					// JDT: ctor in Bar is <init>(Foo) {}
					// JAVAC: ctor in Bar is <init>(Foo$1) {}
										
					descriptor = Utils.insertExtraParameter(classname, descriptor);
					
					MethodVisitor mv = cw.visitMethod(ACC_PUBLIC_STATIC, mInitializerName, descriptor, signature, exceptions);

					ConstructorCopier cc = new ConstructorCopier(mv, typeDescriptor, suffix, classname);
					return cc;
				} else {
					// static initializer
					MethodVisitor mv = cw.visitMethod(ACC_PUBLIC_STATIC, mStaticInitializerName, descriptor, signature, exceptions);
					return new MethodCopier(mv, typeDescriptor.isInterface(), descriptor, typeDescriptor, classname, suffix);
				}
			}
		}

		public void visitSource(String sourcefile, String debug) {
			cw.visitSource(sourcefile, debug);//getSMAP(sourcefile));
		}
		
		/**
		 * Create the SMAP according to the JSR45 spec, *Note* this method is a work in progress not currently used.
		 * 
		 * @param sourcefile
		 * @return debug extension attribute encoded into a string
		 */
		public String getSMAP(String sourcefile) {
			System.out.println("Building smap for "+sourcefile);
			StringBuilder s = new StringBuilder();
			
			// Header
			s.append("SMAP\n");
			s.append(sourcefile+"\n"); // name of the generated file
			s.append("SpringLoaded\n"); // Default stratum (Java)	

			// StratumSection
			s.append("*S SpringLoaded\n");
			
			// FileSection
			s.append("*F\n");
			s.append("+ 1 "+sourcefile+"\n");
			s.append("jaapplication1/"+sourcefile+"\n");
//			s.append("1 javaapplication1/"+sourcefile+"\n");
			
			// LineSection
			s.append("*L\n");
			s.append("1#1,1000:1,1\n");
			
			// EndSection
			s.append("*E\n");
			
			System.out.println(s.toString());
			return s.toString();
		}

		private static class CopyingAnnotationVisitor extends AnnotationVisitor {

			private AnnotationVisitor av;

			public CopyingAnnotationVisitor(AnnotationVisitor av) {
				super(ASM5);
				this.av = av;
			}

			public void visit(String name, Object value) {
				av.visit(name, value);
			}

			public AnnotationVisitor visitAnnotation(String name, String desc) {
				AnnotationVisitor localav = av.visitAnnotation(name, desc);
				return new CopyingAnnotationVisitor(localav);
			}

			public AnnotationVisitor visitArray(String name) {
				AnnotationVisitor localav = av.visitArray(name);
				return new CopyingAnnotationVisitor(localav);
			}

			public void visitEnd() {

				av.visitEnd();
			}

			public void visitEnum(String name, String desc, String value) {
				av.visitEnum(name, desc, value);
			}

		}

		public void visitOuterClass(String arg0, String arg1, String arg2) {
			// nothing to do
		}

		public void visitAttribute(Attribute attr) {
			// nothing to do
		}

		public void visitEnd() {
			// nothing to do
		}

		public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
			// nothing to do
		}
	}

}
