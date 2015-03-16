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

import org.objectweb.asm.Type;
import org.springsource.loaded.jvm.JVM;


/**
 * Implementation of Invoker that wraps a {@link Method} object. It is assumed that this Method object is from a
 * non-reloadable Class so it shouldn't need any kind of special handling.
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class JavaMethodInvoker extends Invoker {

	private Method method;

	public JavaMethodInvoker(/*@SuppressWarnings("unused")*/JavaClassMethodProvider provider, Method method) {
		this.method = method;
	}

	@Override
	public Object invoke(Object target, Object... params) throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		return method.invoke(target, params);
	}

	@Override
	public Method createJavaMethod() {
		return JVM.copyMethod(method);
	}

	@Override
	public int getModifiers() {
		return method.getModifiers();
	}

	@Override
	public String getName() {
		return method.getName();
	}

	@Override
	public String getMethodDescriptor() {
		return Type.getMethodDescriptor(method);
	}

	@Override
	public String getClassName() {
		return method.getDeclaringClass().getName();
	}

}
