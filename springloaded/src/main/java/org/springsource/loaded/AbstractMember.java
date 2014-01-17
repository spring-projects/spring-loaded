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

/**
 * Simple implementation of Member which could represent a method, field or constructor.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public abstract class AbstractMember implements Constants {

	protected final int modifiers;
	protected final String name;
	protected final String descriptor; // this is the erased descriptor.  There is no generic descriptor.
	// Members have a well known id within their type - ids are unique per kind of member (methods/fields/constructors)
	protected int id = -1;
	// For generic methods, contains generic signature
	protected final String signature;
	private final boolean isPrivate; // gets asked a lot so made into a flag

	protected AbstractMember(int modifiers, String name, String descriptor, String signature) {
		this.modifiers = modifiers;
		this.name = name;
		this.descriptor = descriptor;
		this.signature = signature;
		this.isPrivate = Modifier.isPrivate(modifiers);
	}

	/**
	 * @return the name of the member
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @return the member descriptor. methods/constructors: "()Ljava/lang/String;" fields: "Ljava/lang/String;"
	 */
	public final String getDescriptor() {
		return descriptor;
	}

	/**
	 * @return the generics related signature. May be null if this method is non-generic.
	 */
	public String getGenericSignature() {
		return signature;
	}

	/**
	 * @return the modifiers of the member
	 */
	public final int getModifiers() {
		return modifiers;
	}

	/**
	 * @return the allocated ID for this member
	 */
	public final int getId() {
		if (id == -1) {
			throw new IllegalStateException("id not yet allocated");
		}
		return id;
	}

	/**
	 * @param id the id number to assign to this member for later quick reference.
	 */
	public final void setId(int id) {
		this.id = id;
	}

	// helpers

	public final boolean isStatic() {
		return Modifier.isStatic(getModifiers());
	}

	public final boolean isFinal() {
		return Modifier.isFinal(getModifiers());
	}

	public final boolean isPrivate() {
		return isPrivate;
	}

	public final boolean isProtected() {
		return Modifier.isProtected(getModifiers());
	}

	public final boolean isPublic() {
		return Modifier.isPublic(getModifiers());
	}

	public boolean isPrivateStaticFinal() {
		return (modifiers & ACC_PRIVATE_STATIC_FINAL) != 0;
	}

}
