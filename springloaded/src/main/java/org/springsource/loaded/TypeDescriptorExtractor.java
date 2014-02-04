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

	private final static boolean DEBUG_TYPE_DESCRIPTOR_EXTRACTOR = false;
	
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
	 * Visit a class and accumulate sufficient information to build a TypeDescriptor.
	 */
	class ExtractionVisitor implements ClassVisitor, Opcodes {

		private boolean isReloadableType;
		private int flags;
		private String typename;
		private String superclassName;
		private String[] interfaceNames;
		private boolean isGroovy = false;
		private boolean isEnum = false;
		private boolean hasClinit = false;
		// TODO [perf - reduce garbage] make these collections lazily initialize
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
		 * Determine if there are clashes. A clash is where a static method takes the reloadable type as its first parameter
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

		// TODO [refactor] extract the type registry relationship code into a central helper class
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
		 * Algorithm: Go up the superclass hierarchy for a type and determine what should be caught in this type (see 
		 * 'catchers' in notes.md). Methods that are private, static or final do *not* get a catcher. This method
		 * also computes superdispatchers - see 'superdispatchers' in notes.md
		 * 
		 */
		private void walkHierarchyForCatchersAndSuperDispatchers(String superclass, List<String> superDispatchers, List<String> finalInHierarchy) {
			TypeDescriptor supertypeDescriptor = superclass==null?null:findTypeDescriptor(registry, superclass);
			if (DEBUG_TYPE_DESCRIPTOR_EXTRACTOR) {
				System.out.println("Computing catchers on "+this.typename+" from superclass "+superclass);
			}
			boolean isReloadable = supertypeDescriptor.isReloadable();
			for (MethodMember method: supertypeDescriptor.getMethods()) {
				if (shouldCreateSuperDispatcherFor(method) && !superDispatchers.contains(method.nameAndDescriptor)) {
					// need a public super dispatcher - so that we can reach that super method
					// from a reloaded instance of this type
					MethodMember superdispatcher = method.superDispatcherFor();
					methods.add(superdispatcher);
					superDispatchers.add(method.nameAndDescriptor);
				}
				if (shouldCatchMethod(method) && !finalInHierarchy.contains(method.getNameAndDescriptor())) {
					// don't need the catcher if method is already defined since when the existing method is rewritten
					// it will be kind of morphed into a catcher
					// TODO what about a private method that is overridden by a static method (same name/descriptor but not
					// an overrides relationship)
					if (!isReloadable && Modifier.isFinal(method.getModifiers())) {
						// Do not create a catcher, the supertype is not reloadable and so an implementation cannot be
						// added lower in the type hierarchy
						finalInHierarchy.add(method.getNameAndDescriptor());
						continue;
					}
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
					if (DEBUG_TYPE_DESCRIPTOR_EXTRACTOR) {
						System.out.println("Adding catcher for "+method.nameAndDescriptor);
					}
					methods.add(catcherCopy);
				} else {
					if (method.isFinal()) {
						finalInHierarchy.add(method.getNameAndDescriptor());
					}
				}
			}
			if (supertypeDescriptor.supertypeName != null) {
				walkHierarchyForCatchersAndSuperDispatchers(supertypeDescriptor.supertypeName, superDispatchers, finalInHierarchy);
			}
			if (Modifier.isAbstract(this.flags) && !this.isEnum/* && !Modifier.isInterface(this.flags)*/) {
				// abstract class may be missing methods that it can implement from the interfaces
				for (String interfaceName : supertypeDescriptor.superinterfaceNames) {
					addCatchersForNonImplementedMethodsFrom(interfaceName, finalInHierarchy);
				}
			}
		}
		
		/**
		 * Compute and add the catch methods and super dispatch methods that apply to this type.
		 */
		private void computeCatchersAndSuperdispatchers() {
			if (Modifier.isInterface(this.flags)) { // Don't need catchers in interfaces
				return;
			}
			
			// TODO [review design] review the need to create catchers for methods where the supertype is reloadable.
			// Can we just add them to the topmost reloadable type?
			List<String> doNotCatch = new ArrayList<String>();
			walkHierarchyForCatchersAndSuperDispatchers(superclassName, new ArrayList<String>(), doNotCatch);

			// ought to look in interfaces if we are an abstract class
			if (Modifier.isAbstract(this.flags) && !this.isEnum/* && !Modifier.isInterface(this.flags)*/) {
				// abstract class may be missing methods that it can implement from the interfaces
				for (String interfaceName : interfaceNames) {
					addCatchersForNonImplementedMethodsFrom(interfaceName, doNotCatch);
				}
			}
			if (DEBUG_TYPE_DESCRIPTOR_EXTRACTOR) {
				System.out.println("For "+this.typename+" setting finalsInHierarchy to "+doNotCatch);
			}
			finalInHierarchy.addAll(doNotCatch);
		}

		// TODO should clone and finalize be in here?
		private boolean shouldCreateSuperDispatcherFor(MethodMember method) {
			return method.isProtected() && !(
					(method.getName().equals("finalize") && method.getDescriptor().equals("()V")) || 
					(method.getName().equals("clone") && method.getDescriptor().equals("()Ljava/lang/Object;")));
		}

		private void addCatchersForNonImplementedMethodsFrom(String interfacename,List<String> finalInNonReloadableType) {
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
				if (!found && !finalInNonReloadableType.contains(method.getNameAndDescriptor())) {
					if (DEBUG_TYPE_DESCRIPTOR_EXTRACTOR) {
						Log.log("adding catcher for ["+method+"] from ["+interfacename+"] to ["+this.typename+"]");
					}
					methods.add(method.catcherCopyOfWithAbstractRemoved());
				}
			}
			for (String interfaceName : interfaceDescriptor.superinterfaceNames) {
				addCatchersForNonImplementedMethodsFrom(interfaceName,finalInNonReloadableType);
			}
		}

		/**
		 * Protected fields in reloadable parents of a class need an accessor adding to the reloadable
		 * type so that the fields can be reached from the executor.
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
			return !(method.isPrivateOrStaticOrFinal() || method.getName().endsWith(Constants.methodSuffixSuperDispatcher) || 
					 (method.getName().equals("finalize") && method.getDescriptor().equals("()V")));
		}

		public void visit(int version, int flags, String name, String signature, String superclassName, String[] interfaceNames) {
			this.flags = flags;
			this.superclassName = superclassName;
			this.interfaceNames = interfaceNames;
			if (superclassName!=null && superclassName.equals("java/lang/Enum")) {
				this.isEnum = true;
			}
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
