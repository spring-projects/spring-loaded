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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link MethodProvider} that provides methods by using the Java reflection API.
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class JavaClassMethodProvider extends MethodProvider {

	private Class<?> clazz;

	public JavaClassMethodProvider(Class<?> clazz) {
		this.clazz = clazz;
	}

	@Override
	public List<Invoker> getDeclaredMethods() {
		Method[] jMethods = clazz.getDeclaredMethods();
		List<Invoker> invokers = new ArrayList<Invoker>(jMethods.length);
		for (Method jMethod : jMethods) {
			invokers.add(new JavaMethodInvoker(this, jMethod));
		}
		return invokers;
	}

	@Override
	public MethodProvider getSuper() {
		Class<?> supr = clazz.getSuperclass();
		if (supr == null) {
			return null;
		}
		return MethodProvider.create(supr);
	}

	@Override
	public MethodProvider[] getInterfaces() {
		Class<?>[] jItfs = clazz.getInterfaces();
		MethodProvider[] itfs = new MethodProvider[jItfs.length];
		for (int i = 0; i < itfs.length; i++) {
			itfs[i] = MethodProvider.create(jItfs[i]);
		}
		return itfs;
	}

	@Override
	public String getSlashedName() {
		return getDottedName().replace('.', '/');
	}

	@Override
	public String getDottedName() {
		return clazz.getName();
	}

	@Override
	public boolean isInterface() {
		return clazz.isInterface();
	}

}
