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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.MethodMember;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils;


/**
 * Concrete implementation of MethodProvider that provides methods for a Reloadable Type, taking into account any changes made to
 * the type by reloading.
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class ReloadableTypeMethodProvider extends TypeDescriptorMethodProvider {

	ReloadableType rtype;

	public ReloadableTypeMethodProvider(ReloadableType rtype) {
		if (GlobalConfiguration.assertsMode) {
			Utils.assertTrue(rtype != null, "ReloadableTypeMethodProvider rtype should not be null");
		}
		this.rtype = rtype;
	}

	protected Invoker invokerFor(final MethodMember methodMember) {
		if (rtype.getLiveVersion() == null) {
			//Should be possible to call the original method
			return new OriginalClassInvoker(rtype.getClazz(), methodMember, rtype.getJavaMethodCache());
		} else {
			//Should be calling executor method
			return ReloadedTypeInvoker.create(this, methodMember);
		}
	}

	public TypeDescriptor getTypeDescriptor() {
		return rtype.getLatestTypeDescriptor();
	}

	@Override
	protected TypeRegistry getTypeRegistry() {
		return rtype.getTypeRegistry();
	}

	public ReloadableType getRType() {
		return rtype;
	}

	@Override
	public List<Invoker> getDeclaredMethods() {
		if (TypeRegistry.nothingReloaded && rtype.invokersCache_getDeclaredMethods != null) {
			// use the cached version, it will not change if a reload hasn't occurred
			return rtype.invokersCache_getDeclaredMethods;
		}
		List<Invoker> invokers = super.getDeclaredMethods();
		rtype.invokersCache_getDeclaredMethods = invokers;
		return invokers;
	}

	@Override
	public Collection<Invoker> getMethods() {
		if (TypeRegistry.nothingReloaded && rtype.invokersCache_getMethods != null) {
			// use the cached version, it will not change if a reload hasn't occurred
			return rtype.invokersCache_getMethods;
		}
		Collection<Invoker> invokers = super.getMethods();
		rtype.invokersCache_getMethods = invokers;
		return invokers;
	}

	@Override
	public Invoker getMethod(String name, Class<?>[] params) {
		String paramsDescriptor = Utils.toParamDescriptor(params);
		if (TypeRegistry.nothingReloaded) {
			// use the cache
			// TODO manage memory for this cache
			Map<String, Map<String, Invoker>> m = rtype.invokerCache_getMethod;
			Map<String, Invoker> psToInvoker = m.get(name);
			if (psToInvoker != null) {
				if (psToInvoker.containsKey(paramsDescriptor)) {
					return psToInvoker.get(paramsDescriptor);
				}
			}
		}
		Invoker invoker = super.getMethod(name, params);
		if (TypeRegistry.nothingReloaded) {
			Map<String, Map<String, Invoker>> m = rtype.invokerCache_getMethod;
			Map<String, Invoker> psToInvoker = m.get(name);
			if (psToInvoker == null) {
				psToInvoker = new HashMap<String, Invoker>();
				m.put(name, psToInvoker);
			}
			psToInvoker.put(paramsDescriptor, invoker);
		}
		return invoker;
	}

	@Override
	public Invoker getDeclaredMethod(String name, String paramsDescriptor) {
		if (TypeRegistry.nothingReloaded) {
			// use the cache
			// TODO manage memory for this cache
			Map<String, Map<String, Invoker>> m = rtype.invokerCache_getDeclaredMethod;
			Map<String, Invoker> psToInvoker = m.get(name);
			if (psToInvoker != null) {
				if (psToInvoker.containsKey(paramsDescriptor)) {
					return psToInvoker.get(paramsDescriptor);
				}
			}
		}
		Invoker invoker = super.getDeclaredMethod(name, paramsDescriptor);
		if (TypeRegistry.nothingReloaded) {
			Map<String, Map<String, Invoker>> m = rtype.invokerCache_getDeclaredMethod;
			Map<String, Invoker> psToInvoker = m.get(name);
			if (psToInvoker == null) {
				psToInvoker = new HashMap<String, Invoker>();
				m.put(name, psToInvoker);
			}
			psToInvoker.put(paramsDescriptor, invoker);
		}
		return invoker;
	}
}
