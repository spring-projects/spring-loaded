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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Representation of a Method. Some of the bitflags and state are only set for 'incremental' methods - those found in a
 * secondary type descriptor representing a type reload.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class MethodMember extends AbstractMember {

	final static MethodMember[] NONE = new MethodMember[0];

	protected final String[] exceptions;

	public int bits;

	// computed up front:
	public final static int BIT_CATCHER = 0x001;

	public final static int BIT_CLASH = 0x0002;

	// identifies a catcher method placed into an abstract class (where a method from a super interface hasn't been implemented)
	public final static int BIT_CATCHER_INTERFACE = 0x004;

	public final static int BIT_SUPERDISPATCHER = 0x0008;

	// computed on incremental members to indicate what changed:
	public final static int MADE_STATIC = 0x0010;

	public final static int MADE_NON_STATIC = 0x0020;

	public final static int VISIBILITY_CHANGE = 0x0040;

	public final static int IS_NEW = 0x0080;

	public final static int WAS_DELETED = 0x0100;

	public final static int EXCEPTIONS_CHANGE = 0x0200;

	// For MethodMembers in an incremental type descriptor, this tracks the method in the original type descriptor (if there was one)
	public MethodMember original;

	public final String nameAndDescriptor;

	public Method cachedMethod;

	protected MethodMember(int modifiers, String name, String descriptor, String signature, String[] exceptions) {
		super(modifiers, name, descriptor, signature);
		this.exceptions = perhapsSortIfNecessary(exceptions);
		this.nameAndDescriptor = new StringBuilder(name).append(descriptor).toString();
	}

	private String[] perhapsSortIfNecessary(String[] exceptions) {
		if (exceptions == null) {
			return Constants.NO_STRINGS;
		}
		//		Arrays.sort(exceptions);
		return exceptions;
	}

	public String[] getExceptions() {
		return exceptions;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("0x").append(Integer.toHexString(modifiers));
		sb.append(" ").append(name).append(descriptor);
		if (exceptions.length != 0) {
			sb.append(" throws ");
			for (String ex : exceptions) {
				sb.append(ex).append(" ");
			}
		}
		return sb.toString().trim();
	}

	public String getParamDescriptor() {
		// more likely to be at the end, lets go back from there
		for (int pos = descriptor.length() - 1; pos > 0; pos--) {
			if (descriptor.charAt(pos) == ')') {
				return descriptor.substring(0, pos + 1);
			}
		}
		throw new IllegalStateException("Method has invalid descriptor: " + descriptor);
	}

	public boolean hasReturnValue() {
		return descriptor.charAt(descriptor.length() - 1) != 'V';
	}

	public boolean equals(Object other) {
		if (!(other instanceof MethodMember)) {
			return false;
		}
		MethodMember o = (MethodMember) other;
		if (!name.equals(o.name)) {
			return false;
		}
		if (modifiers != o.modifiers) {
			return false;
		}
		if (!descriptor.equals(o.descriptor)) {
			return false;
		}
		if (exceptions.length != o.exceptions.length) {
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
		for (int i = 0; i < exceptions.length; i++) {
			if (!exceptions[i].equals(o.exceptions[i])) {
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
		if (exceptions != null) {
			for (String ex : exceptions) {
				result = result * 37 + ex.hashCode();
			}
		}
		return result;
	}

	public MethodMember catcherCopyOf() {
		int newModifiers = modifiers & ~Modifier.NATIVE;
		if (name.equals("clone") && (modifiers & Modifier.NATIVE) != 0) {
			newModifiers = Modifier.PUBLIC;
		}
		else if ((modifiers & Modifier.PROTECTED) != 0) {
			// promote to public
			// The reason for this is that the executor may try and call these things and as it is not in the hierarchy
			// it cannot. The necessary knock on effect is that subtypes get their methods promoted to public too...
			newModifiers = Modifier.PUBLIC;
		}
		else if ((modifiers & Constants.ACC_PUBLIC_PRIVATE_PROTECTED) == 0) {
			// promote to public from default
			// The reason for this is that the executor may try and call these things and as it is not in the hierarchy
			// it cannot. The necessary knock on effect is that subtypes get their methods promoted to public too...
			newModifiers = Modifier.PUBLIC;
		}
		MethodMember copy = new MethodMember(newModifiers, name, descriptor, signature, exceptions);
		copy.bits |= MethodMember.BIT_CATCHER;
		return copy;
	}

	public MethodMember superDispatcherFor() {
		int newModifiers = modifiers & ~Modifier.NATIVE;
		if (name.equals("clone") && (modifiers & Modifier.NATIVE) != 0) {
			newModifiers = Modifier.PUBLIC;
		}
		else if ((modifiers & Modifier.PROTECTED) != 0) {
			// promote to public
			// The reason for this is that the executor may try and call these things and as it is not in the hierarchy
			// it cannot. The necessary knock on effect is that subtypes get their methods promoted to public too...
			newModifiers = Modifier.PUBLIC;
		}
		else if ((modifiers & Constants.ACC_PUBLIC_PRIVATE_PROTECTED) == 0) {
			// promote to public from default
			// The reason for this is that the executor may try and call these things and as it is not in the hierarchy
			// it cannot. The necessary knock on effect is that subtypes get their methods promoted to public too...
			newModifiers = Modifier.PUBLIC;
		}
		MethodMember copy = new MethodMember(newModifiers, name + "_$superdispatcher$", descriptor, signature,
				exceptions);
		copy.bits |= MethodMember.BIT_SUPERDISPATCHER;
		return copy;
	}

	public MethodMember catcherCopyOfWithAbstractRemoved() {
		int newModifiers = modifiers & ~(Modifier.NATIVE | Modifier.ABSTRACT);
		if (name.equals("clone") && (modifiers & Modifier.NATIVE) != 0) {
			newModifiers = Modifier.PUBLIC;
		}
		else if ((modifiers & Modifier.PROTECTED) != 0) {
			// promote to public
			// The reason for this is that the executor may try and call these things and as it is not in the hierarchy
			// it cannot. The necessary knock on effect is that subtypes get their methods promoted to public too...
			newModifiers = Modifier.PUBLIC;
		}
		else if ((modifiers & Constants.ACC_PUBLIC_PRIVATE_PROTECTED) == 0) {
			// promote to public from default
			// The reason for this is that the executor may try and call these things and as it is not in the hierarchy
			// it cannot. The necessary knock on effect is that subtypes get their methods promoted to public too...
			newModifiers = Modifier.PUBLIC;
		}
		MethodMember copy = new MethodMember(newModifiers, name, descriptor, signature, exceptions);
		copy.bits |= MethodMember.BIT_CATCHER;
		copy.bits |= MethodMember.BIT_CATCHER_INTERFACE;
		return copy;
	}

	public boolean equalsApartFromModifiers(MethodMember other) {
		if (!(other instanceof MethodMember)) {
			return false;
		}
		MethodMember o = other;
		if (!name.equals(o.name)) {
			return false;
		}
		if (!descriptor.equals(o.descriptor)) {
			return false;
		}
		//		if (exceptions.length != o.exceptions.length) {
		//			return false;
		//		}
		//		for (int i = 0; i < exceptions.length; i++) {
		//			if (!exceptions[i].equals(o.exceptions[i])) {
		//				return false;
		//			}
		//		}
		return true;
	}

	public String getNameAndDescriptor() {
		return nameAndDescriptor;
	}

	public static boolean isClash(MethodMember method) {
		return (method.bits & MethodMember.BIT_CLASH) != 0;
	}

	public static boolean isSuperDispatcher(MethodMember method) {
		return (method.bits & BIT_SUPERDISPATCHER) != 0;
	}

	public static boolean isCatcher(MethodMember method) {
		return (method.bits & BIT_CATCHER) != 0;
	}

	public static boolean isCatcherForInterfaceMethod(MethodMember method) {
		return (method.bits & BIT_CATCHER_INTERFACE) != 0;
	}

	public static boolean isDeleted(MethodMember method) {
		return (method.bits & WAS_DELETED) != 0;
	}

	public Object bitsToString() {
		StringBuilder s = new StringBuilder();
		if ((bits & BIT_CATCHER) != 0) {
			s.append("catcher ");
		}
		if ((bits & BIT_CLASH) != 0) {
			s.append("clash ");
		}
		if ((bits & BIT_SUPERDISPATCHER) != 0) {
			s.append("superdispatcher ");
		}
		if ((bits & MADE_STATIC) != 0) {
			s.append("made_static ");
		}
		if ((bits & MADE_NON_STATIC) != 0) {
			s.append("made_non_static ");
		}
		if ((bits & VISIBILITY_CHANGE) != 0) {
			s.append("vis_change ");
		}
		if ((bits & IS_NEW) != 0) {
			s.append("is_new ");
		}
		if ((bits & WAS_DELETED) != 0) {
			s.append("is_new ");
		}
		return "[" + s.toString().trim() + "]";
	}

	/*
	 * Determine whether this method should replace the other method on reload. In accordance to how JVM works at class load time,
	 * this will be the case if this and other have the same Class, name, parameter types and return type. I.e. formally, in JVM
	 * bytecode (unlike source code) a method doesn't override a method with a different return type. When such a situation occurs
	 * in source code, the compiler will introduce a bridge method in bytecode.
	 */
	public boolean shouldReplace(MethodMember other) {
		if (!name.equals(other.name)) {
			return false;
		}
		if (!descriptor.equals(other.descriptor)) {
			return false;
		}
		return true;
	}

	public boolean isConstructor() {
		return name.equals("<init>");
	}

}
