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
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A type descriptor describes the type, methods, fields, etc - two type descriptors are comparable to discover what has changed
 * between versions.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class TypeDescriptorExtractor {

	private TypeRegistry registry;

	public TypeDescriptorExtractor(TypeRegistry registry) {
		this.registry = registry;
	}

	public TypeDescriptor extract(byte[] bytes, boolean isReloadableType) {
		ClassReader fileReader = new ClassReader(bytes);
		ExtractionVisitor extractionVisitor = new ExtractionVisitor(isReloadableType);
		fileReader.accept(extractionVisitor, 0);
		return extractionVisitor.getTypeDescriptor();
	}

	/**
	 * Visit a class and accumulate enough information to build a TypeDescriptor.
	 */
	class ExtractionVisitor implements ClassVisitor, Opcodes {

		private boolean isReloadableType;
		private int flags;
		private String typename;
		private String superclassName;
		private String[] interfaceNames;
		private boolean isGroovy = false;
		private boolean hasClinit = false;
		private List<MethodMember> constructors = new ArrayList<MethodMember>();
		private List<MethodMember> methods = new ArrayList<MethodMember>();
		private List<FieldMember> fieldsRequiringAccessors = new ArrayList<FieldMember>();
		private List<FieldMember> fields = new ArrayList<FieldMember>();
		private List<String> finalInHierarchy = new ArrayList<String>();
		
		public ExtractionVisitor(boolean isReloadableType) {
			this.isReloadableType = isReloadableType;
		}
		

		public TypeDescriptor getTypeDescriptor() {
			if (isReloadableType) {
				computeCatchersAndSuperdispatchers();
			}
			computeFieldsRequiringAccessors();
			computeClashes();
			TypeDescriptor td = new TypeDescriptor(typename, superclassName, interfaceNames, flags, constructors, methods, fields,
					fieldsRequiringAccessors, isReloadableType, registry, hasClinit, finalInHierarchy);
			if (isGroovy) {
				td.setIsGroovyType(true);
			}
			return td;
		}

		/**
		 * Determine if there are clashes. A clash is where a static method takes the this reloadable type as its first parameter
		 * but in all other ways is the same as an existing instance method. For example this instance method A.foo(String) clashes
		 * with this static method A.foo(A, String). 'clashing' means the executor will have to do something to avoid a duplicate
		 * method problem and we'll have to differentiate between the two.
		 */
		private void computeClashes() {
			String clashDescriptorPrefix = "(L" + typename + ";";
			for (MethodMember member : methods) {
				if (member.isStatic()) {
					String desc = member.descriptor;
					if (desc.startsWith(clashDescriptorPrefix)) {
						// might be a clash, need to check the instance methods
						for (MethodMember member2 : methods) {
							if (member2.name.equals(member.name)) {
								// really might be a clash
								String instanceParams = member2.descriptor;
								instanceParams = instanceParams.substring(1, instanceParams.indexOf(')') + 1);
								String staticParams = desc.substring(clashDescriptorPrefix.length(), desc.indexOf(')') + 1);
								if (instanceParams.equals(staticParams)) {
									// CLASH
									member.bits |= MethodMember.BIT_CLASH;
								}
							}
						}
					}
				}
			}
		}

		private TypeDescriptor getTypeDescriptorFor(String slashedname) {
			return registry.getDescriptorFor(slashedname);
		}

		private TypeDescriptor findTypeDescriptor(TypeRegistry registry, String typename) {
			// follow the pattern for a classloader: recurse up trying to find it, then recurse down trying to load it
			TypeRegistry regToTry = registry;
			TypeDescriptor td = regToTry.getDescriptorForReloadableType(typename);
			while (td == null) {
				regToTry = regToTry.getParentRegistry();
				if (regToTry == null) {
					break;
				}
				td = regToTry.getDescriptorForReloadableType(typename);
			}
			if (td == null) {
				td = getTypeDescriptorFor(typename);
			}
			return td;
		}

		/**
		 * Create catcher methods for methods from our super-hierarchy that we don't yet override (but may after the initial define
		 * has happened).
		 */
		private void computeCatchersAndSuperdispatchers() {
			// When walking up the hierarchy we may hit a 'final' method which means we must not catch it.
			// The 'shouldNotCatch' list stores things we discover like this that should not be caught
			List<String> shouldNotCatch = new ArrayList<String>();

			String type = superclassName;
			// Don't need catchers in interfaces
			if (Modifier.isInterface(this.flags)) {
				return;
			}
			List<String> superDispatcherAddedFor = new ArrayList<String>();
			while (type != null) {
				TypeDescriptor supertypeDescriptor = findTypeDescriptor(registry, type);
				// TODO review the need to create catchers for methods where the supertype is reloadable.  In this situation we are already going to
				// be intercepting the call side of these methods so we don't need the catcher.  Could be a large performance increase and reduction in 
				// permgen, and simplification of stack traces
				//				if (!supertypeDescriptor.isReloadable()) {
				for (MethodMember method : supertypeDescriptor.getMethods()) {
					if (shouldCreateSuperDispatcherFor(method) && !superDispatcherAddedFor.contains(method.nameAndDescriptor)) {
						// need a public super dispatcher - so that we can reach that super method
						// from a reloaded instance of this type
						MethodMember superdispatcher = method.superDispatcherFor();
						methods.add(superdispatcher);
						superDispatcherAddedFor.add(method.nameAndDescriptor);
					}
					
					if (shouldCatchMethod(method) && !shouldNotCatch.contains(method.getNameAndDescriptor())) {
						// don't need the catcher if method is already defined since when the existing method is rewritten
						// it will be kind of morphed into a catcher
						// TODO what about a private method that is overridden by a static method (same name/descriptor but not
						// an overrides relationship)
						//						if (supertypeDescriptor.isGroovyType() && !isGroovy) {
						//							if (method.getName().startsWith("super$")) {
						//								continue;
						//							}
						//						}
						MethodMember found = null;
						for (MethodMember existingMethod : methods) {
							if (existingMethod.equalsApartFromModifiers(method)) {
								found = existingMethod;
								break;
							}
						}
						if (found != null) {
							continue;
						}
						MethodMember catcherCopy = method.catcherCopyOf();
						//						System.out.println("catcher is " + catcherCopy + "  is groovy type? " + this.isGroovy);
						methods.add(catcherCopy);
					} else {
						if (method.isFinal()) {
							shouldNotCatch.add(method.getNameAndDescriptor());
						}
					}
				}
				//				}
				type = supertypeDescriptor.supertypeName;
			}

			// ought to look in interfaces *if* we are an abstract class
			if (Modifier.isAbstract(this.flags)/* && !Modifier.isInterface(this.flags)*/) {
				// abstract class
				for (String interfaceName : interfaceNames) {
					addCatchersForNonImplementedMethodsFrom(interfaceName);
				}
			}

			finalInHierarchy.addAll(shouldNotCatch);
		}

		// TODO should clone and finalize be in here?
		private boolean shouldCreateSuperDispatcherFor(MethodMember method) {
			return method.isProtected() && !(
					(method.getName().equals("finalize") && method.getDescriptor().equals("()V")) || 
					(method.getName().equals("clone") && method.getDescriptor().equals("()Ljava/lang/Object;")));
		}


		private void addCatchersForNonImplementedMethodsFrom(String interfacename) {
			TypeDescriptor interfaceDescriptor = findTypeDescriptor(registry, interfacename);
			for (MethodMember method : interfaceDescriptor.getMethods()) {
				// If this class doesn't implement this interface method, add it
				boolean found = false;
				for (MethodMember existingMethod : methods) {
					if (existingMethod.equalsApartFromModifiers(method)) {
						found = true;
						break;
					}
				}
				if (!found) {
					methods.add(method.catcherCopyOfWithAbstractRemoved());
				}
			}
			for (String interfaceName : interfaceDescriptor.superinterfaceNames) {
				addCatchersForNonImplementedMethodsFrom(interfaceName);
			}
		}

		/**
		 * Field
		 */
		private void computeFieldsRequiringAccessors() {
			String type = superclassName;
			while (type != null) {
				TypeDescriptor supertypeDescriptor = findTypeDescriptor(registry, type);
				if (!supertypeDescriptor.isReloadable()) {
					for (FieldMember field : supertypeDescriptor.getFields()) {
						if (field.isProtected()) {
							boolean found = false;
							for (FieldMember existingField : fields) {
								if (existingField.getName().equals(field.getName())) {
									// no need for accessor... this type defines a field that overrides it
									found = true;
									break;
								}
							}
							if (!found) {
								fieldsRequiringAccessors.add(field);
							}
						}
					}
				}
				type = supertypeDescriptor.supertypeName;
			}
		}

		/**
		 * Determine if a method gets a catcher. Deliberately not catching final methods, static methods, private methods or
		 * finalize()V.
		 * 
		 * @return true if it should be caught
		 */
		private boolean shouldCatchMethod(MethodMember method) {
			return !(method.isPrivateStaticFinal() || method.getName().endsWith(Constants.methodSuffixSuperDispatcher) || (method.getName().equals("finalize") && method.getDescriptor().equals("()V")));
		}

		public void visit(int version, int flags, String name, String signature, String superclassName, String[] interfaceNames) {
			this.flags = flags;
			this.superclassName = superclassName;
			this.interfaceNames = interfaceNames;
			this.typename = name;
		}

		public AnnotationVisitor visitAnnotation(String classDesc, boolean isRuntime) {
			return null;
		}

		public void visitAttribute(Attribute attribute) {
		}
		
		public void visitInnerClass(String name, String outername, String innerName, int access) {
			if (name.equals(typename)) {
				this.flags = access;
			}
		}

		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			fields.add(new FieldMember(typename, access, name, desc, signature));
			if (name.equals("$callSiteArray")) {
				isGroovy = true;
			}
			return null;
		}
		// For each method, copy it into the new class making appropriate adjustments
		/**
		 * Visit a method in the class and build an appropriate representation for it to include in the extracted output.
		 */
		public MethodVisitor visitMethod(int flags, String name, String descriptor, String genericSignature, String[] exceptions) {
			if (name.charAt(0) != '<') {
				methods.add(new MethodMember(flags, name, descriptor, genericSignature, exceptions));
			} else {
				if (name.equals("<init>")) {
					//Even though constructors are not reloadable at present, we need to add them to type descriptors to know
					//about their original modifiers (these are promoted to public to allow executors access to them).
					constructors.add(new MethodMember(flags, name, descriptor, genericSignature, exceptions));
				} else if (name.equals("<clinit>")) {
					hasClinit = true;
				}
			}
			return null;
		}

		public void visitOuterClass(String owner, String name, String desc) {
		}

		public void visitSource(String source, String debug) {
		}

		public void visitEnd() {
		}

	}

}
