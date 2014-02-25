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
package org.springsource.loaded.jvm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springsource.loaded.ri.ReflectiveInterceptor;


/**
 * Utility class containing operations that are "JVM" specific and may need porting when changing JVMs.
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class JVM {

	public static Logger log = Logger.getLogger(JVM.class.getName());

	@SuppressWarnings("unchecked")
	private static Constructor<Method> jlrMethodCtor = (Constructor<Method>) Method.class.getDeclaredConstructors()[0];

	private static Method jlrMethodCopy;
	static {
		try {
			jlrMethodCopy = Method.class.getDeclaredMethod("copy");
			jlrMethodCopy.setAccessible(true);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problems getting 'Method.copy()' method. Incompatible JVM?", e);
		}
	}

	private static Method jlrFieldCopy;
	static {
		try {
			jlrFieldCopy = Field.class.getDeclaredMethod("copy");
			jlrFieldCopy.setAccessible(true);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problems getting 'Field.copy()' method. Incompatible JVM?", e);
		}
	}

	private static Method jlrConstructorCopy;
	static {
		try {
			jlrConstructorCopy = Constructor.class.getDeclaredMethod("copy");
			jlrConstructorCopy.setAccessible(true);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problems getting 'Constructor.copy()' method. Incompatible JVM?", e);
		}
	}

	private static Field jlrMethodModifiers;
	static {
		try {
			jlrMethodModifiers = Method.class.getDeclaredField("modifiers");
			jlrMethodModifiers.setAccessible(true);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problems getting Field 'Method.modifiers' method. Incompatible JVM?", e);
		}
	}

	private static Field jlrConstructorModifiers;
	static {
		try {
			jlrConstructorModifiers = Constructor.class.getDeclaredField("modifiers");
			jlrConstructorModifiers.setAccessible(true);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problems getting Field 'Constructor.modifiers' method. Incompatible JVM?", e);
		}
	}

	private static Field jlrFieldModifiers;
	static {
		try {
			jlrFieldModifiers = Field.class.getDeclaredField("modifiers");
			jlrFieldModifiers.setAccessible(true);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problems getting Field 'Field.modifiers' method. Incompatible JVM?", e);
		}
	}

	@SuppressWarnings("restriction")
	public static void ensureMemberAccess(Class<?> callerClass, Class<?> declaringClass, Object target, int mods)
			throws IllegalAccessException {
		sun.reflect.Reflection.ensureMemberAccess(callerClass, declaringClass, target, mods);
	}

	/*
	 * Create a new Method object from scratch. This Method object is 'fake' and will not be "invokable". ReflectionInterceptor will
	 * be responsible to make sure user code calling 'invoke' on this object will be intercepted and handled appropriately.
	 */
	public static Method newMethod(Class<?> clazz, String name, Class<?>[] params, Class<?> returnType, Class<?>[] exceptions,
			int modifiers, String signature) {
		// This is what the constructor looks like:
		// Method(Class declaringClass, String name, Class[] parameterTypes, Class returnType,
		// Class[] checkedExceptions, int modifiers, int slot, String signature,
		// byte[] annotations, byte[] parameterAnnotations, byte[] annotationDefault)
		Method returnMethod;
		try {
			jlrMethodCtor.setAccessible(true);
			returnMethod = jlrMethodCtor.newInstance(clazz, name, params, returnType, exceptions, modifiers, 0, signature, null,
					null, null);
		} catch (Exception e) {
			//This shouldn't happen...
			ReflectiveInterceptor.log.log(Level.SEVERE, "Internal Error", e);
			throw new Error(e);
		}
		return returnMethod;
	}

	/*
	 * Creates a copy of a method object that is equivalent to the original.
	 */
	public static Method copyMethod(Method method) {
		try {
			return (Method) jlrMethodCopy.invoke(method);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problems copying method. Incompatible JVM?", e);
			return method; // return original as the best we can do
		}
	}

	public static Field copyField(Field field) {
		try {
			return (Field) jlrFieldCopy.invoke(field);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problems copying field. Incompatible JVM?", e);
			return field; // return original as the best we can do
		}
	}

	public static Constructor<?> copyConstructor(Constructor<?> c) {
		try {
			return (Constructor<?>) jlrConstructorCopy.invoke(c);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problems copying constructor. Incompatible JVM?", e);
			return c; // return original as the best we can do
		}
	}

	public static void setMethodModifiers(Method method, int modifiers) {
		try {
			jlrMethodModifiers.setInt(method, modifiers);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Couldn't set correct modifiers on reflected method: " + method, e);
		}
	}

	public static void setConstructorModifiers(Constructor<?> c, int modifiers) {
		try {
			jlrConstructorModifiers.setInt(c, modifiers);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Couldn't set correct modifiers on reflected constructor: " + c, e);
		}
	}

	public static void setFieldModifiers(Field field, int mods) {
		try {
			jlrFieldModifiers.setInt(field, mods);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Couldn't set correct modifiers on reflected field: " + field, e);
		}
	}

	@SuppressWarnings("unchecked")
	private static final Constructor<Field> jlFieldCtor = (Constructor<Field>) Field.class.getDeclaredConstructors()[0];

	public static Field newField(Class<?> declaring, Class<?> type, int mods, String name, String sig) {
		jlFieldCtor.setAccessible(true);
		// This is what the constructor looks like:
		// Field(Class declaringClass,String name,Class type,int modifiers, int slot, String signature,
		// byte[] annotations)
		try {
			return jlFieldCtor.newInstance(declaring, name, type, mods, 0, sig, null);
		} catch (Exception e) {
			throw new IllegalStateException("Problem creating reloadable Field: " + declaring.getName() + "." + name, e);
		}
	}

	@SuppressWarnings("unchecked")
	private static final Constructor<Constructor<?>> jlConstructorCtor = (Constructor<Constructor<?>>) Constructor.class
			.getDeclaredConstructors()[0];

	public static Constructor<?> newConstructor(Class<?> clazz, Class<?>[] params, Class<?>[] exceptions, int modifiers,
			String signature) {
		jlConstructorCtor.setAccessible(true);
		// This is what the constructor looks like:
		//	    Constructor(Class<T> declaringClass,
		//                Class[] parameterTypes,
		//                Class[] checkedExceptions,
		//                int modifiers,
		//                int slot,
		//                String signature,
		//                byte[] annotations,
		//                byte[] parameterAnnotations)
		try {
			return jlConstructorCtor.newInstance(clazz, params, exceptions, modifiers, 0, signature, null, null);
		} catch (Exception e) {
			StringBuffer msg = new StringBuffer("Problem creating reloadable Constructor: ");
			msg.append(clazz.getName());
			msg.append("(");
			for (int i = 0; i < params.length; i++) {
				if (i > 0) {
					msg.append(", ");
				}
				msg.append(params[i].getName());
			}
			msg.append(")");
			throw new IllegalStateException(msg.toString(), e);
		}
	}

}
