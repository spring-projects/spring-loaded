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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class computes and then encapsulates what has changed between the original form of a type and a newly loaded
 * version.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class IncrementalTypeDescriptor implements Constants {

	private TypeDescriptor initialTypeDescriptor;

	private TypeDescriptor latestTypeDescriptor;

	private int bits;

	private final static int BIT_COMPUTED_DIFF = 0x0001;

	private Map<String, MethodMember> latestMethods; // Map from nameAndDescriptor to the MethodMember

	private List<MethodMember> newOrChangedMethods;

	private List<MethodMember> newOrChangedConstructors; // TODO required?

	private List<MethodMember> deletedMethods;

	public IncrementalTypeDescriptor(TypeDescriptor initialTypeDescriptor) {
		reinitialize();
		this.initialTypeDescriptor = initialTypeDescriptor;
	}

	public TypeDescriptor getLatestTypeDescriptor() {
		return latestTypeDescriptor;
	}

	public void setLatestTypeDescriptor(TypeDescriptor typeDescriptor) {
		reinitialize();
		this.latestTypeDescriptor = typeDescriptor;
	}

	/**
	 * When first setup or a new latest descriptor passed to us, forget what we know.
	 */
	private void reinitialize() {
		// trigger recomputation
		bits = 0x0000;
	}

	/**
	 * Return the list of 'new or changed' methods. New or changed is characterised as: *
	 * <ul>
	 * <li>methods that never used to exist but do now
	 * <li>methods where name and descriptor are the same but something else has changed (see MethodMember.equals())
	 * <li>method was represented as a catcher in the original, but is now 'real'
	 * </ul>
	 * It does not include catchers.
	 * 
	 * @return list of new or changed methods in this type
	 */
	public List<MethodMember> getNewOrChangedMethods() {
		compute();
		return newOrChangedMethods;
	}

	/**
	 * Return the list of 'new or changed' constructors. New or changed is characterized as:
	 * <ul>
	 * <li>constructors that did not exist in the original class as loaded, but do now
	 * <li>constructors that did exist in the original class but have changed in some way (visibility)
	 * </ul>
	 * 
	 * @return list of new or changed constructors in this type
	 */
	public List<MethodMember> getNewOrChangedConstructors() {
		compute();
		return newOrChangedConstructors;
	}

	public List<MethodMember> getDeletedMethods() {
		compute();
		return deletedMethods;
	}

	private void compute() {
		if ((bits & BIT_COMPUTED_DIFF) != 0) {
			return;
		}
		latestMethods = new HashMap<String, MethodMember>();
		newOrChangedMethods = new ArrayList<MethodMember>();
		deletedMethods = new ArrayList<MethodMember>();
		// Process the methods in the latest copy, compared to the original
		for (MethodMember latest : latestTypeDescriptor.getMethods()) {

			// Did this method exist in the original? Ask by name and descriptor
			MethodMember original = initialTypeDescriptor.getByDescriptor(latest.getName(), latest.getDescriptor());

			// If it did not exist, tag it
			if (original == null) {
				latest.bits |= MethodMember.IS_NEW;
				newOrChangedMethods.add(latest);
			}
			else {
				if (!original.equals(latest)) { // check more than just name/descriptor
					newOrChangedMethods.add(latest);
				}
				// If originally it was a catcher and now it is no longer a catcher (an impl has been provided), record it
				if (MethodMember.isCatcher(original) && !MethodMember.isCatcher(latest)) {
					latest.bits |= MethodMember.IS_NEW;
					newOrChangedMethods.add(latest);
				}
				// TODO [perf] not convinced this can occur? Think it through
				if (MethodMember.isSuperDispatcher(original) && !MethodMember.isSuperDispatcher(latest)) {
					latest.bits |= MethodMember.IS_NEW;
					newOrChangedMethods.add(latest);
				}

				// If it now is a catcher where it didn't used to be, it has been deleted
				if (MethodMember.isCatcher(latest) && !MethodMember.isCatcher(original)) {
					latest.bits |= MethodMember.WAS_DELETED;
				}
				latest.original = original;
				// Keep track of important changes:
				if (original.modifiers != latest.modifiers) {
					// Determine if a change was made from static to non-static or vice versa
					boolean wasStatic = original.isStatic();
					boolean isStatic = latest.isStatic();
					if (wasStatic != isStatic) {
						if (wasStatic) {
							// has been made non-static
							latest.bits |= MethodMember.MADE_NON_STATIC;
						}
						else {
							// has been made static
							latest.bits |= MethodMember.MADE_STATIC;
						}
					}
					// Determine if a change was made with regards visibility
					int oldVisibility = original.modifiers & ACC_PUBLIC_PRIVATE_PROTECTED;
					int newVisibility = latest.modifiers & ACC_PUBLIC_PRIVATE_PROTECTED;
					if (oldVisibility != newVisibility) {
						latest.bits |= MethodMember.VISIBILITY_CHANGE;
					}
				}
				// TODO do we care about exceptions changing?  It doesn't make it a new method.
				// TODO if we do, upgrade this check to remember the precise changes?
				//				int oExceptionsLength = original.exceptions == null ? 0 : original.exceptions.length;
				//				int nExceptionsLength = latest.exceptions == null ? 0 : latest.exceptions.length;
				//				if (oExceptionsLength != nExceptionsLength) {
				//					latest.bits |= MethodMember.EXCEPTIONS_CHANGE;
				//				} else {
				//					for (int i = 0; i < oExceptionsLength; i++) {
				//						if (!original.exceptions[i].equals(latest.exceptions[i])) {
				//							latest.bits |= MethodMember.EXCEPTIONS_CHANGE;
				//						}
				//					}
				//				}
			}
			String nadKey = new StringBuilder(latest.getName()).append(latest.getDescriptor()).toString();
			latestMethods.put(nadKey, latest);
		}
		for (MethodMember initialMethod : initialTypeDescriptor.getMethods()) {
			if (MethodMember.isCatcher(initialMethod)) {
				continue;
			}
			if (!latestTypeDescriptor.defines(initialMethod)) {
				deletedMethods.add(initialMethod);
			}
		}
		bits |= BIT_COMPUTED_DIFF;
	}

	public boolean mustUseExecutorForThisMethod(int methodId) {
		// Rule1: if it is a new method, we must use the executor

		compute();
		// If it is a catcher method that now has an implementation, we must use the executor
		MethodMember method = initialTypeDescriptor.getMethod(methodId);
		if (MethodMember.isCatcher(method)) {
			// Has it now been provided??  If it has not we can just return immediately
			boolean found = false;
			for (MethodMember method2 : newOrChangedMethods) {
				if (method2.shouldReplace(method)) { // modifiers? what of static/nonstatic et al
					//We should not consider modifiers or exceptions in this test!
					// otherwise we will end up not finding a method that really should replace / override
					// the catcher. 
					found = true;
					if (MethodMember.isCatcher(method2)) {
						return false;
					}
				}
			}
			if (!found) {
				// not provided!  New type descriptor doesn't include catchers
				return false;
			}
		}
		return true;
	}

	public boolean hasBeenDeleted(int methodId) {
		compute();
		MethodMember method = initialTypeDescriptor.getMethod(methodId);

		boolean a = false;
		for (MethodMember m : deletedMethods) {
			if (m.equals(method)) {
				a = true;
				break;
			}
		}

		// alternative mechanism
		//		boolean b = true;
		//		for (MethodMember m : this.latestTypeDescriptor.getMethods()) {
		//			if (m.equals(method)) {
		//				b = wasDeleted(m);
		//				break;
		//			}
		//		}

		return a;
	}

	public MethodMember getFromLatestByDescriptor(String nameAndDescriptor) {
		compute();
		return latestMethods.get(nameAndDescriptor);
	}

	// For checking the bitflags:

	/**
	 * @param mm the MethodMember to check if brand new
	 * @return true if the method is brand new after a reload (i.e. was never defined in the original type)
	 */
	public static boolean isBrandNewMethod(MethodMember mm) {
		return (mm.bits & MethodMember.IS_NEW) != 0;
	}

	public static boolean hasChanged(MethodMember mm) {
		return (mm.bits & 0x7fffffff) != 0;
	}

	public static boolean isCatcher(MethodMember method) {
		return (method.bits & MethodMember.BIT_CATCHER) != 0;
	}

	public static boolean isNowNonStatic(MethodMember method) {
		return (method.bits & MethodMember.MADE_NON_STATIC) != 0;
	}

	public static boolean isNowStatic(MethodMember method) {
		return (method.bits & MethodMember.MADE_STATIC) != 0;
	}

	public static boolean hasVisibilityChanged(MethodMember method) {
		return (method.bits & MethodMember.VISIBILITY_CHANGE) != 0;
	}

	public static boolean wasDeleted(MethodMember method) {
		return (method.bits & MethodMember.WAS_DELETED) != 0;
	}

	public TypeDescriptor getOriginal() {
		return this.initialTypeDescriptor;
	}

	public String toString() {
		return toString(false);
	}

	public String toString(boolean compute) {
		StringBuilder s = new StringBuilder();
		s.append("Original:\n").append(this.initialTypeDescriptor).append("\nCurrent:\n").append(
				this.latestTypeDescriptor);
		s.append('\n');
		if (compute) {
			compute();
			s.append("Deleted methods: ").append(deletedMethods).append("\n");
			s.append("New or changed methods: ").append(newOrChangedMethods).append("\n");
		}
		return s.toString();
	}

}
