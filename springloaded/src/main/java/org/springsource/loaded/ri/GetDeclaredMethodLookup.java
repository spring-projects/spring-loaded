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

import java.util.List;

/**
 * Provides an implementation for method lookup as suitable for 'Class.getDeclaredMethod'
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class GetDeclaredMethodLookup {

	private String name;
	private String paramsDescriptor;

	/*
	 * Create an object capable of performing the lookup in some MethodProvider
	 */
	public GetDeclaredMethodLookup(String name, String paramsDescriptor) {
		this.name = name;
		this.paramsDescriptor = paramsDescriptor;
	}

	public Invoker lookup(MethodProvider methodProvider) {
		List<Invoker> methods = methodProvider.getDeclaredMethods();
		Invoker found = null;
		for (Invoker invoker : methods) {
			if (matches(invoker)) {
				if (found == null || isMoreSpecificReturnTypeThan(invoker, found)) {
					found = invoker;
				}
			}
		}
		return found;
	}

	/*
	 * @return true if m2 has a more specific return type than m1
	 */
	private boolean isMoreSpecificReturnTypeThan(Invoker m1, Invoker m2) {
		//This uses 'Class.isAssigableFrom'. This is ok, assuming that inheritance hierarchy is not something that we are allowed
		// to change on reloads.
		Class<?> cls1 = m1.getReturnType();
		Class<?> cls2 = m2.getReturnType();
		return cls2.isAssignableFrom(cls1);
	}

	protected boolean matches(Invoker invoker) {
		return name.equals(invoker.getName()) && paramsDescriptor.equals(invoker.getParamsDescriptor());
	}

	@Override
	public String toString() {
		return "GetDeclaredMethod( " + name + "." + paramsDescriptor + " )";
	}
}
