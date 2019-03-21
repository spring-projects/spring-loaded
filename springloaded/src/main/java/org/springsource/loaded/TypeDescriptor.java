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

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the information about a type relevant to reloading. The TypeDescriptor for a type is sometimes extracted
 * whilst performing some other operation (eg. {@link InterfaceExtractor}) but can also be retrieved directly using
 * {@link TypeDescriptorExtractor}.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class TypeDescriptor implements Constants {

	private final int modifiers;

	final String typename; // slashed

	final String supertypeName; // slashed

	final String[] superinterfaceNames; // slashed // empty array if there are none

	private final MethodMember[] constructors; // empty array if there are none (but this doesn't ever happen!)

	private final MethodMember[] methods; // empty array if there are none

	private final MethodMember[] nonprivateMethods; // empty array if there are none

	private final FieldMember[] fields; // empty array if there are none

	private final FieldMember[] fieldsRequiringAccessors; // empty array if there are none

	private List<String> finalInHierarchy; // nameAndDescriptor strings for methods final in the hierarchy (e.g. ordinal()I for an enum)

	private final TypeRegistry registry;

	private final boolean isReloadable;

	private final boolean hasClinit;

	private final static int IS_GROOVY_TYPE = 0x0001;

	private int bits = 0x0000;

	private ReloadableType reloadableType;

	private int nextId = 0;

	public TypeDescriptor(String slashedTypeName, String supertypeName, String[] superinterfaceNames, int modifiers,
			List<? extends MethodMember> constructors, List<MethodMember> methods, List<? extends FieldMember> fields,
			List<? extends FieldMember> fieldsRequiringAccessors, boolean isReloadable, TypeRegistry registry,
			boolean hasClinit,
			List<String> finalInHierarchy) {
		this.typename = slashedTypeName;
		this.supertypeName = supertypeName;
		this.superinterfaceNames = (superinterfaceNames == null ? NO_STRINGS : superinterfaceNames);
		this.finalInHierarchy = finalInHierarchy;
		this.modifiers = modifiers;
		this.fields = fields.size() == 0 ? FieldMember.NONE : fields.toArray(new FieldMember[fields.size()]);
		this.fieldsRequiringAccessors = fieldsRequiringAccessors.size() == 0 ? FieldMember.NONE
				: fieldsRequiringAccessors
						.toArray(new FieldMember[fieldsRequiringAccessors.size()]);
		this.constructors = constructors.size() == 0 ? MethodMember.NONE
				: constructors.toArray(new MethodMember[constructors
						.size()]);
		this.methods = methods.size() == 0 ? MethodMember.NONE : methods.toArray(new MethodMember[methods.size()]);
		this.nonprivateMethods = filterNonPrivateMethods(this.methods);
		this.isReloadable = isReloadable;
		this.registry = registry;
		this.hasClinit = hasClinit;
		allocateIds();
	}

	private static MethodMember[] filterNonPrivateMethods(MethodMember[] allMethods) {
		List<MethodMember> result = null;
		for (MethodMember method : allMethods) {
			if (!method.isPrivate()) {
				if (result == null) {
					result = new ArrayList<MethodMember>();
				}
				result.add(method);
			}
		}
		if (result == null) {
			return MethodMember.NONE;
		}
		else {
			return result.toArray(new MethodMember[result.size()]);
		}
	}

	private void allocateIds() {
		// Give the methods awareness of their index
		for (MethodMember method : methods) {
			method.setId(nextId++);
		}
	}

	public MethodMember[] getMethods() {
		return methods;
	}

	public MethodMember[] getConstructors() {
		return constructors;
	}

	public FieldMember[] getFields() {
		return fields;
	}

	public FieldMember[] getFieldsRequiringAccessors() {
		return fieldsRequiringAccessors;
	}

	public int getModifiers() {
		return modifiers;
	}

	/**
	 * @return the (slashed) type name
	 */
	public String getName() {
		return typename;
	}

	/**
	 * @return the (slashed) supertype name
	 */
	public String getSupertypeName() {
		return supertypeName;
	}

	/**
	 * @return array of (slashed) superinterface names (or an empty array if none)
	 */
	public String[] getSuperinterfacesName() {
		return superinterfaceNames;
	}

	/**
	 * Check if this descriptor defines the specified method. A strict check on all aspects of the method -
	 * names/exceptions/flags, etc.
	 * 
	 * @param method the method to check the existence of in this type descriptor
	 * @return true if this descriptor defines the specified method
	 */
	public boolean defines(MethodMember method) {
		for (MethodMember existingMethod : methods) {
			// make sure it *really* defines it (i.e. it is not a catcher)
			if (!MethodMember.isCatcher(existingMethod) && existingMethod.equals(method)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if this descriptor defines a method with the specified name and descriptor. Return the method if it is
	 * found. Modifiers, generic signature and exceptions are ignored in this search.
	 * 
	 * @param name the member name
	 * @param descriptor the member descriptor (e.g. (Ljava/lang/String;)I)
	 * @return the MethodMember if there is one
	 */
	public MethodMember getByDescriptor(String name, String descriptor) {
		for (MethodMember existingMethod : methods) {
			if (existingMethod.getName().equals(name) && existingMethod.getDescriptor().equals(descriptor)) {
				return existingMethod;
			}
		}
		return null;
	}

	public MethodMember getByNameAndDescriptor(String nameAndDescriptor) {
		for (MethodMember existingMethod : methods) {
			if (nameAndDescriptor.startsWith(existingMethod.getName())
					&& nameAndDescriptor.endsWith(existingMethod.getDescriptor())) {
				return existingMethod;
			}
		}
		return null;
	}

	/**
	 * @return true if this type descriptor has been created for a reloadable type
	 */
	public boolean isReloadable() {
		return isReloadable;
	}

	public MethodMember getMethod(int methodId) {
		// Should never be an AIOOBE if the woven code is behaving
		return methods[methodId];
	}

	public MethodMember getConstructor(int ctorId) {
		// Should never be an AIOOBE if the woven code is behaving
		return constructors[ctorId];
	}

	/**
	 * @return true if the type is an interface
	 */
	public boolean isInterface() {
		return (modifiers & ACC_INTERFACE) != 0;
	}

	/**
	 * @return true if the type is an annotation
	 */
	public boolean isAnnotation() {
		return (modifiers & ACC_ANNOTATION) != 0;
	}

	/**
	 * @return true if the type is an enum
	 */
	public boolean isEnum() {
		return (modifiers & ACC_ENUM) != 0;
	}

	public boolean definesNonPrivate(String nameAndDescriptor) {
		for (MethodMember existingMethod : nonprivateMethods) {
			if (existingMethod.nameAndDescriptor.equals(nameAndDescriptor)) {
				return true;
			}
		}
		return false;
	}

	public boolean isFinalInHierarchy(String nad) {
		return finalInHierarchy.contains(nad);
	}

	/**
	 * Search for a field on this type descriptor - do not try supertypes. This lookup does not differentiate between
	 * static/instance fields.
	 * 
	 * @param name the name of the field
	 * @return a FieldMember if the field is found, otherwise null
	 */
	public FieldMember getField(String name) {
		for (FieldMember field : fields) {
			if (field.getName().equals(name)) {
				return field;
			}
		}
		return null;
	}

	public ReloadableType getReloadableType() {
		if (!isReloadable) {
			return null;
		}
		if (reloadableType == null) {
			reloadableType = registry.getReloadableType(this.typename);
			if (reloadableType == null) {
				throw new IllegalStateException("There is no ReloadableType instance for " + typename);
			}
		}
		return reloadableType;
	}

	public TypeRegistry getTypeRegistry() {
		return registry;
	}

	// could be worth caching if used for more than error messages...
	public String getDottedName() {
		return getName().replace('/', '.');
	}

	public MethodMember getConstructor(String desc) {
		for (MethodMember ctor : constructors) {
			String d = ctor.getDescriptor();
			if (d.equals(desc)) {
				return ctor;
			}
		}
		return null;
	}

	public boolean isGroovyType() {
		return (bits & IS_GROOVY_TYPE) != 0;
	}

	public void setIsGroovyType(boolean b) {
		bits |= IS_GROOVY_TYPE;
	}

	public boolean hasClinit() {
		return hasClinit;
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("TypeDescriptor: name=" + typename + "  superclass=" + supertypeName + "  superinterfaces="
				+ interfacesToString());
		s.append(" flags=0x" + Integer.toHexString(modifiers).toUpperCase()).append("\n");
		s.append("Fields: #" + fields.length + "\n" + fieldsToString());
		s.append("Constructors:#" + constructors.length + "\n" + methodsToString(constructors));
		s.append("Methods:#" + methods.length + "\n" + methodsToString(methods));
		return s.toString();
	}

	private String fieldsToString() {
		StringBuilder s = new StringBuilder();
		int count = 0;
		for (FieldMember field : fields) {
			s.append(" field #" + Utils.toPaddedNumber((count++), 3)).append(' ').append(field.toString()).append('\n');
		}
		return s.toString();
	}

	private String interfacesToString() {
		if (superinterfaceNames == null) {
			return "";
		}
		else {
			StringBuilder s = new StringBuilder();
			for (String superinterfaceName : superinterfaceNames) {
				s.append(superinterfaceName);
				s.append(" ");
			}
			return s.toString().trim();
		}
	}

	public String methodsToString(MethodMember[] methods) {
		StringBuilder s = new StringBuilder();
		int count = 0;
		for (MethodMember method : methods) {
			s.append(" method #" + Utils.toPaddedNumber((count++), 3)).append(' ').append(method.toString()).append(
					"   ")
					.append(method.bitsToString()).append('\n');
		}
		return s.toString();
	}

}
