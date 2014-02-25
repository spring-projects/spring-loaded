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
package org.springsource.loaded.ri;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;
import org.springsource.loaded.MethodMember;


/**
 * Creating Java Method objects for a given MethodMember is rather expensive because it typically involves getting. The declared
 * methods of a Class and searching for one that matches the method signature. This is most problematic when we are trying to get a
 * Method for an array of MethodMembers, because in this case we will end up repeating the process multiple times. A JavaMethodCache
 * instance can cache Method objects from the first time we iterate the declared methods of a class so subsequently we can just get
 * the other methods from the cache.
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class JavaMethodCache {

	//TODO: [...] This cache uses string+descriptor for key. It may be possible to cache method objects inside MethodMembers
	// themselves, which would make for much quicker 'lookup'.

	/*
	 * This class is used to initialise the cache in a thread safe manner. I.e. a fully filled map should be passed into the cache's
	 * initialize method, so that the 'isInitialized' method will not return true unless initialisation is complete and all entries
	 * are present.
	 */
	public static class Initializer {

		//To build up initial map entries with 'put'
		private Map<String, Method> cache = new HashMap<String, Method>();

		protected void put(Method method) {
			cache.put(method.getName() + Type.getMethodDescriptor(method), method);
		}

	}

	/*
	 * Map indexed by name+descriptor.
	 */
	private Map<String, Method> cache = null;

	public boolean isInitialized() {
		return cache != null;
	}

	/*
	 * This method should be called to put all entries into the map.
	 */
	public void initialize(Initializer init) {
		this.cache = init.cache;
		init.cache = null; // Not strictly necessary, but prevents reuse of the initializer.
	}

	public Method get(MethodMember methodMember) {
		return cache.get(methodMember.getNameAndDescriptor());
	}

}
