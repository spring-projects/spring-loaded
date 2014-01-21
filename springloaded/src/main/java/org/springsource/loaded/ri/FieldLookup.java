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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;
import org.springsource.loaded.CurrentLiveVersion;
import org.springsource.loaded.FieldMember;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils;
import org.springsource.loaded.jvm.JVM;


/**
 * This class contains code that is used as support infrastructure to implement Field lookup algorithms.
 * 
 * Mainly, it provides an abstraction to allows Java classes and reloadable types to be treated as instances of a common abstraction
 * "FieldProvider" and then implement algorithms to find fields in those providers independent of how the fields are being provided.
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class FieldLookup {

	private static class JavaFieldRef extends FieldRef {

		private Field f;

		public JavaFieldRef(Field f) {
			this.f = f;
		}

		@Override
		public Field getField() {
			return f;
		}

		@Override
		public String getName() {
			return f.getName();
		}

		@Override
		public boolean isPublic() {
			return Modifier.isPublic(f.getModifiers());
		}

	}

	private static class JavaClassFieldProvider extends FieldProvider {

		private Class<?> clazz;

		public JavaClassFieldProvider(Class<?> clazz) {
			this.clazz = clazz;
		}

		@Override
		List<FieldRef> getFields() {
			Field[] fields = clazz.getDeclaredFields();
			List<FieldRef> refs = new ArrayList<FieldLookup.FieldRef>();
			for (Field f : fields) {
				refs.add(new JavaFieldRef(f));
			}
			return refs;
		}

		@Override
		public boolean isInterface() {
			return clazz.isInterface();
		}

		@Override
		public FieldProvider[] getInterfaces() {
			Class<?>[] itfs = clazz.getInterfaces();
			FieldProvider[] provs = new FieldProvider[itfs.length];
			for (int i = 0; i < itfs.length; i++) {
				provs[i] = FieldProvider.create(itfs[i]);
			}
			return provs;
		}

		@Override
		public FieldProvider getSuper() {
			Class<?> supr = clazz.getSuperclass();
			if (supr != null) {
				FieldProvider.create(supr);
			}
			return null;
		}

	}

	static abstract class FieldRef {

		public abstract Field getField();

		public abstract String getName();

		public abstract boolean isPublic();

	}

	public static class ReloadedTypeFieldRef extends FieldRef {

		private ReloadableType rtype;
		private FieldMember f;

		public ReloadedTypeFieldRef(ReloadableType rtype, FieldMember f) {
			if (GlobalConfiguration.assertsOn) {
				Utils.assertTrue(rtype.hasBeenReloaded(), "Not yet reloaded: " + rtype.getName());
			}
			this.rtype = rtype;
			this.f = f;
		}

		@Override
		public Field getField() {
			Class<?> declaring = Utils.toClass(rtype);
			Class<?> type;
			try {
				type = Utils.toClass(Type.getType(f.getDescriptor()), rtype.typeRegistry.getClassLoader());
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException(e);
			}
			return JVM.newField(declaring, type, f.getModifiers(), f.getName(), f.getGenericSignature());
		}

		@Override
		public String getName() {
			return f.getName();
		}

		@Override
		public boolean isPublic() {
			return f.isPublic();
		}

	}

	protected static abstract class FieldProvider {

		abstract List<FieldRef> getFields();

		public abstract boolean isInterface();

		public abstract FieldProvider[] getInterfaces();

		public abstract FieldProvider getSuper();

		public static FieldProvider create(ReloadableType rtype) {
			return new ReloadableTypeFieldProvider(rtype);
		}

		public static FieldProvider create(TypeRegistry typeRegistry, String slashyName) {
			if (typeRegistry.isReloadableTypeName(slashyName)) {
				return create(typeRegistry.getReloadableType(slashyName));
			} else {
				try {
					return create(Utils.toClass(Type.getObjectType(slashyName), typeRegistry.getClassLoader()));
				} catch (ClassNotFoundException e) {
					throw new IllegalStateException(e);
				}
			}
		}

		public static FieldProvider create(Class<?> clazz) {
			return new JavaClassFieldProvider(clazz);
		}
	}

	public static class ReloadableTypeFieldProvider extends FieldProvider {
		private ReloadableType rtype;

		public ReloadableTypeFieldProvider(ReloadableType rtype) {
			this.rtype = rtype;
		}

		@Override
		List<FieldRef> getFields() {
			FieldMember[] fields = rtype.getLatestTypeDescriptor().getFields();
			List<FieldRef> refs = new ArrayList<FieldRef>(fields.length);
			for (FieldMember f : fields) {
				refs.add(fieldRef(rtype, f));
			}
			return refs;
		}

		private FieldRef fieldRef(ReloadableType rtype, FieldMember f) {
			CurrentLiveVersion clv = rtype.getLiveVersion();
			if (clv == null) {
				//Not yet reloaded... use original field (with fixed mods)
				try {
					Field jf = rtype.getClazz().getDeclaredField(f.getName());
					ReflectiveInterceptor.fixModifier(rtype.getLatestTypeDescriptor(), jf);
					return new JavaFieldRef(jf);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			} else {
				//Already reloaded
				return new ReloadedTypeFieldRef(rtype, f);
			}
		}

		@Override
		public boolean isInterface() {
			return rtype.getLatestTypeDescriptor().isInterface();
		}

		@Override
		public FieldProvider[] getInterfaces() {
			String[] superItfs = rtype.getLatestTypeDescriptor().getSuperinterfacesName();
			FieldProvider[] superProvs = new FieldProvider[superItfs.length];
			for (int i = 0; i < superItfs.length; i++) {
				superProvs[i] = FieldProvider.create(rtype.typeRegistry, superItfs[i]);
			}
			return superProvs;
		}

		@Override
		public FieldProvider getSuper() {
			String supr = rtype.getLatestTypeDescriptor().getSupertypeName();
			if (supr != null) {
				return FieldProvider.create(rtype.typeRegistry, supr);
			}
			return null;
		}
	}

}
