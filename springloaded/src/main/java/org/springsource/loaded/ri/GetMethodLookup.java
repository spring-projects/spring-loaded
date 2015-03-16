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

import org.springsource.loaded.Utils;


/**
 * Implements a 'lookup' strategy that finds methods in the fashion required by java.lang.Class.getMethod
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class GetMethodLookup {

	private String name;

	private String paramsDescriptor;

	/*
	 * Create an object capable of performing the lookup
	 */
	public GetMethodLookup(String name, String paramsDescriptor) {
		this.name = name;
		this.paramsDescriptor = paramsDescriptor;
	}

	public GetMethodLookup(String name, Class<?>[] params) {
		this(name, Utils.toParamDescriptor(params));
	}

	public Invoker lookup(MethodProvider methodProvider) {
		Invoker method = methodProvider.getDeclaredMethod(name, paramsDescriptor);
		if (method != null && Modifier.isPublic(method.getModifiers())) {
			return method;
		}

		// Try the superclass context (but not for interfaces, we aren't supposed to include Object's methods
		// in them!
		if (!methodProvider.isInterface()) {
			MethodProvider parent = methodProvider.getSuper();
			if (parent != null) {
				method = lookup(parent);
				if (method != null) {
					return method;
				}
			}
		}

		// Try the interfaces
		MethodProvider[] itfs = methodProvider.getInterfaces();
		for (MethodProvider itf : itfs) {
			Invoker itfMethod = lookup(itf);
			if (itfMethod != null) {
				return itfMethod;
			}
		}

		return null;
	}

}
