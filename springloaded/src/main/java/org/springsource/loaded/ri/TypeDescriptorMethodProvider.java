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

import java.util.ArrayList;
import java.util.List;

import org.springsource.loaded.MethodMember;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypeRegistry;


/**
 * Abstract base class for implementation of MethodProvider that are capable of producing a {@link TypeDescriptor}
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public abstract class TypeDescriptorMethodProvider extends MethodProvider {

	protected abstract TypeDescriptor getTypeDescriptor();

	protected abstract TypeRegistry getTypeRegistry();

	protected abstract Invoker invokerFor(MethodMember methodMember);

	@Override
	public List<Invoker> getDeclaredMethods() {
		TypeDescriptor typeDescriptor = getTypeDescriptor();
		MethodMember[] methods = typeDescriptor.getMethods();
		List<Invoker> invokers = new ArrayList<Invoker>();
		for (MethodMember method : methods) {
			// TODO [perf] create constant for this check?
			if (((MethodMember.BIT_CATCHER | MethodMember.BIT_SUPERDISPATCHER | MethodMember.WAS_DELETED) & method.bits) == 0) {
				invokers.add(invokerFor(method));
			}
		}
		return invokers;
	}

	@Override
	public MethodProvider getSuper() {
		TypeRegistry registry = getTypeRegistry();
		TypeDescriptor typeDesc = getTypeDescriptor();
		String superName = typeDesc.getSupertypeName();
		if (superName == null) {
			//This happens only for type Object... Code unreachable unless Object is reloadable
			return null;
		}
		else {
			ReloadableType rsuper = registry.getReloadableType(superName);
			if (rsuper != null) {
				return MethodProvider.create(rsuper);
			}
			else {
				TypeDescriptor dsuper = registry.getDescriptorFor(superName);
				return MethodProvider.create(registry, dsuper);
			}
		}
	}

	@Override
	public String getSlashedName() {
		return getTypeDescriptor().getName();
	}

	@Override
	public MethodProvider[] getInterfaces() {
		TypeRegistry registry = getTypeRegistry();
		String[] itfNames = getTypeDescriptor().getSuperinterfacesName();
		MethodProvider[] itfs = new MethodProvider[itfNames.length];
		for (int i = 0; i < itfNames.length; i++) {
			itfs[i] = MethodProvider.create(registry, registry.getDescriptorFor(itfNames[i]));
		}
		return itfs;
	}

	@Override
	public boolean isInterface() {
		return getTypeDescriptor().isInterface();
	}
}
