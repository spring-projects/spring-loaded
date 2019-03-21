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

import java.util.Collection;
import java.util.List;

import org.objectweb.asm.Type;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils;


/**
 * To manage the complexity of the different cases created by a variety of different types of contexts where we can do
 * 'method lookup' we need an abstraction to represent them all.
 * <p>
 * This class provides that abstraction.
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public abstract class MethodProvider {

	public static MethodProvider create(ReloadableType rtype) {
		return new ReloadableTypeMethodProvider(rtype);
	}

	public static MethodProvider create(TypeRegistry registry, TypeDescriptor typeDescriptor) {
		if (typeDescriptor.isReloadable()) {
			ReloadableType rtype = registry.getReloadableType(typeDescriptor.getName(), false);
			if (rtype == null) {
				TypeRegistry tr = registry;
				while (rtype == null) {
					ClassLoader pcl = tr.getClassLoader().getParent();
					if (pcl == null) {
						break;
					}
					else {
						tr = TypeRegistry.getTypeRegistryFor(pcl);
						if (tr == null) {
							break;
						}
						rtype = tr.getReloadableType(typeDescriptor.getName(), false);
					}
				}
			}
			if (rtype != null) {
				return new ReloadableTypeMethodProvider(rtype);
			}

			//			ReloadableType rtype = registry.getReloadableType(typeDescriptor.getName(), true);
			//			// TODO rtype can be null if this type hasn't been loaded yet for the first time, is that true?
			//			// e.g. CGLIB generated proxy for a service type in grails
			//			if (rtype != null) {
			//				return new ReloadableTypeMethodProvider(rtype);
			//			}
		}
		try {
			try {
				Type objectType = Type.getObjectType(typeDescriptor.getName());

				// TODO doing things this way would mean we aren't 'guessing' the delegation strategy, we
				// are instead allowing it to do its thing then looking for the right registry.
				// Above we are guessing regular parent delegation.
				Class<?> class1 = Utils.toClass(objectType, registry.getClassLoader());
				if (typeDescriptor.isReloadable()) {
					ClassLoader cl = class1.getClassLoader();
					TypeRegistry tr = TypeRegistry.getTypeRegistryFor(cl);
					ReloadableType rtype = tr.getReloadableType(typeDescriptor.getName(), true);
					if (rtype != null) {
						return new ReloadableTypeMethodProvider(rtype);
					}
				}
				return create(class1);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalStateException("We have a type descriptor for '" + typeDescriptor.getName()
						+ " but no corresponding Java class", e);
			}
		}
		catch (RuntimeException re) {
			re.printStackTrace();
			throw re;
		}
	}

	public static MethodProvider create(Class<?> clazz) {
		return new JavaClassMethodProvider(clazz);
	}

	public abstract List<Invoker> getDeclaredMethods();

	public abstract MethodProvider getSuper();

	public abstract MethodProvider[] getInterfaces();

	public abstract boolean isInterface();

	public abstract String getSlashedName();

	/**
	 * @return Full qualified name with "."
	 */
	public String getDottedName() {
		return getSlashedName().replace('/', '.');
	}

	public Invoker dynamicLookup(int mods, String name, String methodDescriptor) {
		return new DynamicLookup(name, methodDescriptor).lookup(this);
	}

	public Invoker staticLookup(int mods, String name, String methodDescriptor) {
		return new StaticLookup(name, methodDescriptor).lookup(this);
	}

	public Invoker getMethod(String name, Class<?>[] params) {
		return new GetMethodLookup(name, params).lookup(this);
	}

	public Invoker getDeclaredMethod(String name, String paramsDescriptor) {
		return new GetDeclaredMethodLookup(name, paramsDescriptor).lookup(this);
	}

	public Invoker getDeclaredMethod(String name, Class<?>[] params) {
		return getDeclaredMethod(name, Utils.toParamDescriptor(params));
	}

	public Collection<Invoker> getMethods() {
		return new GetMethodsLookup().lookup(this);
	}

	@Override
	public String toString() {
		return "MethodProvider(" + getDottedName() + ")";
	}

}
