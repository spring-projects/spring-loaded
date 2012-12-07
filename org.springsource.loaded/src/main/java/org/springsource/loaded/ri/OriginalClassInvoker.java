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

import org.springsource.loaded.MethodMember;
import org.springsource.loaded.jvm.JVM;


/**
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class OriginalClassInvoker extends Invoker {

	private Class<?> clazz;
	private MethodMember method;
	private JavaMethodCache methodCache;

	public OriginalClassInvoker(Class<?> clazz, MethodMember methodMember, JavaMethodCache methodCache) {
		this.clazz = clazz;
		this.method = methodMember;
		this.methodCache = methodCache;
	}

	@Override
	public Object invoke(Object target, Object... params) throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		Method method = getJavaMethod();
		method.setAccessible(true); //Disable access checks, we do our own!
		return method.invoke(target, params);
	}

	@Override
	public Method createJavaMethod() {
		Method retval = method.cachedMethod;
		if (retval == null) {
			if (!methodCache.isInitialized()) {
				JavaMethodCache.Initializer init = new JavaMethodCache.Initializer();
				Method[] methods = clazz.getDeclaredMethods();
				for (Method m : methods) {
					init.put(m);
				}
				methodCache.initialize(init);
			}
			retval = methodCache.get(method);
			method.cachedMethod = retval;
			if (retval.getModifiers() != method.getModifiers()) {
				JVM.setMethodModifiers(retval, method.getModifiers());
			}
		}
		return JVM.copyMethod(retval); // Since we got m from a cache we must copy to give it a fresh 'isAccessible' flag.
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
		return method.getDescriptor();
	}

	@Override
	public String getClassName() {
		return clazz.getName();
	}
}