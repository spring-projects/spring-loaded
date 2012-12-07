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

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kris De Volder
 * @since 0.5.0
 * 
 */
public class GetMethodsLookup {

	public Collection<Invoker> lookup(MethodProvider methodProvider) {
		Map<String, Invoker> found = new HashMap<String, Invoker>();
		collectAll(methodProvider, found);
		return found.values();
	}

	/**
	 * Collect all public methods from methodProvider and its supertypes into the 'found' hasmap, indexed by "name+descriptor".
	 */
	private void collectAll(MethodProvider methodProvider, Map<String, Invoker> found) {
		//We do this in inverse order as in 'GetMethodLookup'. This is because GetMethodLookup
		//is lazy and wants to stop when a method is found, but here we instead collect
		//verything bottom up and 'overwrite' earlier results so the last one found is the
		//one kept.

		//First interfaces in inverse order...
		MethodProvider[] itfs = methodProvider.getInterfaces();
		for (int i = itfs.length - 1; i >= 0; i--) { // inverse order
			collectAll(itfs[i], found);
		}

		//Then the superclass(es), but only if we're not an interface (interfaces do not report
		// the methods of Object!
		MethodProvider supr = methodProvider.getSuper();
		if (supr != null && !methodProvider.isInterface()) {
			collectAll(supr, found);
		}

		//Finally all our own public methods
		for (Invoker method : methodProvider.getDeclaredMethods()) {
			if (Modifier.isPublic(method.getModifiers())) {
				found.put(method.getName() + method.getMethodDescriptor(), method);
			}
		}
	}

}
