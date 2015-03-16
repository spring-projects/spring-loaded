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

/**
 * Describes a field, created during TypeDescriptor construction.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class FieldMember extends AbstractMember {

	final static FieldMember[] NONE = new FieldMember[0];

	String typename;

	protected FieldMember(String typename, int modifiers, String name, String descriptor, String signature) {
		super(modifiers, name, descriptor, signature);
		this.typename = typename;
	}

	public String getDeclaringTypeName() {
		return typename;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("0x").append(Integer.toHexString(modifiers));
		sb.append(" ").append(descriptor).append(" ").append(name);
		if (signature != null) {
			sb.append(" [").append(signature).append("]");
		}
		return sb.toString().trim();
	}

	public boolean equals(Object other) {
		if (!(other instanceof FieldMember)) {
			return false;
		}
		FieldMember o = (FieldMember) other;
		if (!name.equals(o.name)) {
			return false;
		}
		if (modifiers != o.modifiers) {
			return false;
		}
		if (!descriptor.equals(o.descriptor)) {
			return false;
		}
		if (signature == null && o.signature != null) {
			return false;
		}
		if (signature != null && o.signature == null) {
			return false;
		}
		if (signature != null) {
			if (!signature.equals(o.signature)) {
				return false;
			}
		}
		return true;
	}

	public int hashCode() {
		int result = modifiers;
		result = result * 37 + name.hashCode();
		result = result * 37 + descriptor.hashCode();
		if (signature != null) {
			result = result * 37 + signature.hashCode();
		}
		return result;
	}

}
