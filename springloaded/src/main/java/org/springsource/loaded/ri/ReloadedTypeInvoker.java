/*
 * Copyright 2010-2012 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.objectweb.asm.Type;
import org.springsource.loaded.CurrentLiveVersion;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.MethodMember;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.Utils;
import org.springsource.loaded.jvm.JVM;


/**
 * Common super type for Invoker for a method on a reloaded type.
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public abstract class ReloadedTypeInvoker extends Invoker {

	ReloadableType rtype;

	private MethodMember methodMember;

	private ReloadedTypeInvoker(ReloadableTypeMethodProvider declaringType, MethodMember methodMember) {
		this.methodMember = methodMember;
		rtype = declaringType.getRType();
		if (GlobalConfiguration.assertsMode) {
			Utils.assertTrue(rtype.hasBeenReloaded(),
					"This class is only equiped to provide invocation/method services for reloaded types");
		}
	}

	@Override
	public abstract Object invoke(Object target, Object... params) throws IllegalArgumentException,
			IllegalAccessException,
			InvocationTargetException;

	/**
	 * Create a 'mock' Java Method which is dependent on ReflectiveInterceptor to catch calls to invoke.
	 */
	@Override
	public Method createJavaMethod() {
		Class<?> clazz = rtype.getClazz();
		String name = methodMember.getName();
		String methodDescriptor = methodMember.getDescriptor();

		ClassLoader classLoader = rtype.getTypeRegistry().getClassLoader();
		try {
			Class<?>[] params = Utils.toParamClasses(methodDescriptor, classLoader);
			Class<?> returnType = Utils.toClass(Type.getReturnType(methodDescriptor), classLoader);
			Class<?>[] exceptions = Utils.slashedNamesToClasses(methodMember.getExceptions(), classLoader);
			return JVM.newMethod(clazz, name, params, returnType, exceptions, methodMember.getModifiers(),
					methodMember.getGenericSignature());
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("Couldn't create j.l.r.Method for " + clazz.getName() + "."
					+ methodDescriptor, e);
		}
	}

	@Override
	public int getModifiers() {
		return methodMember.getModifiers();
	}

	@Override
	public String getName() {
		return methodMember.getName();
	}

	@Override
	public String getMethodDescriptor() {
		return methodMember.getDescriptor();
	}

	@Override
	public String getClassName() {
		return rtype.getName();
	}

	public static Invoker create(ReloadableTypeMethodProvider declaringType, final MethodMember methodMember) {
		if (Modifier.isStatic(methodMember.getModifiers())) {
			// Since static methods don't change parameter lists, they just invoke the executor
			return new ReloadedTypeInvoker(declaringType, methodMember) {

				@Override
				public Object invoke(Object target, Object... params) throws IllegalArgumentException,
						IllegalAccessException,
						InvocationTargetException {
					CurrentLiveVersion clv = rtype.getLiveVersion();
					Method executor = clv.getExecutorMethod(methodMember);
					return executor.invoke(target, params);
				}
			};
		}
		else {
			// Non static method invokers need to add target as a first param
			return new ReloadedTypeInvoker(declaringType, methodMember) {

				@Override
				public Object invoke(Object target, Object... params) throws IllegalArgumentException,
						IllegalAccessException,
						InvocationTargetException {
					CurrentLiveVersion clv = rtype.getLiveVersion();
					Method executor = clv.getExecutorMethod(methodMember);
					if (params == null) {
						return executor.invoke(null, target);
					}
					else {
						Object[] ps = new Object[params.length + 1];
						System.arraycopy(params, 0, ps, 1, params.length);
						ps[0] = target;
						return executor.invoke(null, ps);
					}
				}
			};
		}
	}
}
