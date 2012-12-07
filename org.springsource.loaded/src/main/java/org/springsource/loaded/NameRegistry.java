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
 * Manages a mapping of names to numbers. The same number anywhere means the same name. This means that if some type a/b/C has been
 * loaded in two places (by different classloaders), it will have the same number in both. Only one of those a/b/C types will be
 * visible at the location in question, the tricky part could be working out which one (if classloaders are being naughty) but in
 * theory the first time we get confused (due to finding the name twice), we can work out which one is right and use that mapping
 * from then on.
 * 
 * @author Andy Clement
 * @since 0.8.1
 */
public class NameRegistry {

	private static int nextTypeId = 0;
	private static int size = 10;
	private static String[] allocatedIds = new String[size];

	private NameRegistry() {
	}

	/**
	 * Typically used by tests to ensure it looks like a fresh NameRegistry is being used.
	 */
	public static void reset() {
		nextTypeId = 0;
		size = 10;
		allocatedIds = new String[size];
	}

	/**
	 * Return the id for a particular type. This method will not allocate a new id if the type is unknown, it will return -1
	 * instead.
	 * 
	 * @param slashedClassName a type name like java/lang/String
	 * @return the allocated ID for that type or -1 if unknown
	 */
	public static int getIdFor(String slashedClassName) {
		assert Asserts.assertNotDotted(slashedClassName);
		for (int i = 0; i < nextTypeId; i++) {
			if (allocatedIds[i].equals(slashedClassName)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Return the id for a particular type. This method will not allocate a new id if the type is unknown, it will return -1
	 * instead.
	 * 
	 * @param slashedClassName a type name like java/lang/String
	 * @return the allocated ID for that type or -1 if unknown
	 */
	public static int getIdOrAllocateFor(String slashedClassName) {
		int id = getIdFor(slashedClassName);
		if (id == -1) {
			id = allocateId(slashedClassName);
		}
		return id;
	}

	private synchronized static int allocateId(String slashedClassName) {
		// Check again, in case two threads passed the -1 check in the getIdOrAllocateFor method
		int id = getIdFor(slashedClassName);
		if (id == -1) {
			id = nextTypeId++;
			if (id >= allocatedIds.length) {
				size = size + 10;
				// need to make more room
				String[] newAllocatedIds = new String[size];
				System.arraycopy(allocatedIds, 0, newAllocatedIds, 0, allocatedIds.length);
				allocatedIds = newAllocatedIds;
			}
			allocatedIds[id] = slashedClassName;
		}
		return id;
	}

	public static String getTypenameById(int typeId) {
		if (typeId > size) {
			return null;
		}
		return allocatedIds[typeId];
	}
}
