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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * An invoker represents the result of a method lookup operation in the type hierarchy.
 * <p>
 * It encapsulates a reference to a resolved method implementation in a reloadable or non-reloadable type and provides an 'invoke'
 * method suitable for invoking that method implementation, and a 'createJavaMethod' to create a Java {@link Method} instance that
 * can be used to represent the method in the Java reflection API.
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public abstract class Invoker {

	private Method cachedMethod; //Cached for cases where we get call getJavaMethod multiple times.

	public abstract Object invoke(Object target, Object... params) throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException;

	public abstract int getModifiers();

	public abstract String getName();

	public abstract String getMethodDescriptor();

	public String toString() {
		return "Invoker(" + Modifier.toString(getModifiers()) + " " + getClassName() + "." + getName() + getMethodDescriptor()
				+ ")";
	}

	public abstract String getClassName();

	protected abstract Method createJavaMethod();

	public String getParamsDescriptor() {
		String methodDescriptor = getMethodDescriptor();
		return methodDescriptor.substring(0, methodDescriptor.lastIndexOf(')') + 1);
	}

	public Class<?> getReturnType() {
		return getJavaMethod().getReturnType();
	}

	public final Method getJavaMethod() {
		if (cachedMethod == null) {
			cachedMethod = createJavaMethod();
		}
		return cachedMethod;
	}

}
