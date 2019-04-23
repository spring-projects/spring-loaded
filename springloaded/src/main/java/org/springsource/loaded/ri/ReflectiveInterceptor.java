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

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.springsource.loaded.C;
import org.springsource.loaded.Constants;
import org.springsource.loaded.CurrentLiveVersion;
import org.springsource.loaded.FieldMember;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.MethodMember;
import org.springsource.loaded.ReloadException;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils;
import org.springsource.loaded.infra.UsedByGeneratedCode;
import org.springsource.loaded.jvm.JVM;
import org.springsource.loaded.support.ConcurrentWeakIdentityHashMap;

/**
 * The reflective interceptor is called to rewrite any reflective calls that are found in the bytecode. Intercepting the
 * calls means we can delegate to the SpringLoaded infrastructure.
 *
 * @author Andy Clement
 * @author Kris De Volder
 * @since 0.5.0
 */
public class ReflectiveInterceptor {

	public static Logger log = Logger.getLogger(ReflectiveInterceptor.class.getName());

	private static Map<Class<?>, WeakReference<ReloadableType>> classToRType = null;

	static {
		boolean synchronize = false;
		try {
			String prop = System.getProperty("springloaded.synchronize", "false");
			if (prop.equalsIgnoreCase("true")) {
				synchronize = true;
			}
		}
		catch (Throwable t) {
			// likely security manager
		}
		if (synchronize) {
			classToRType = Collections.synchronizedMap(new WeakHashMap<Class<?>, WeakReference<ReloadableType>>());
		}
		else {
			classToRType = new ConcurrentWeakIdentityHashMap<Class<?>, WeakReference<ReloadableType>>();
			// classToRType = new WeakHashMap<Class<?>, WeakReference<ReloadableType>>();
		}
	}

	@UsedByGeneratedCode
	public static boolean jlosHasStaticInitializer(Class<?> clazz) {
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Exception tells the caller to use the 'old way' to determine if there is a static initializer
			throw new IllegalStateException();
		}
		return rtype.hasStaticInitializer();
	}

	/*
	 * Implementation of java.lang.class.getDeclaredMethod(String name, Class... params).
	 */
	@UsedByGeneratedCode
	public static Method jlClassGetDeclaredMethod(Class<?> clazz, String name, Class<?>... params)
			throws SecurityException,
			NoSuchMethodException {
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable...
			return clazz.getDeclaredMethod(name, params);
		}
		else {
			// Reloadable
			MethodProvider methods = MethodProvider.create(rtype);
			Invoker method = methods.getDeclaredMethod(name, params);
			if (method == null) {
				throw Exceptions.noSuchMethodException(clazz, name, params);
			}
			else {
				return method.createJavaMethod();
			}
		}
	}

	/*
	 * Implementation of java.lang.class.getMethod(String name, Class... params).
	 */
	@UsedByGeneratedCode
	public static Method jlClassGetMethod(Class<?> clazz, String name, Class<?>... params) throws SecurityException,
			NoSuchMethodException {
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable...
			return clazz.getMethod(name, params);
		}
		else {
			MethodProvider methods = MethodProvider.create(rtype);
			Invoker method = methods.getMethod(name, params);
			if (method == null) {
				throw Exceptions.noSuchMethodException(clazz, name, params);
			}
			else {
				return method.createJavaMethod();
			}
		}
	}

	public static Method[] jlClassGetDeclaredMethods(Class<?> clazz) {
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable...
			return clazz.getDeclaredMethods();
		}
		else {
			MethodProvider methods = MethodProvider.create(rtype);
			List<Invoker> invokers = methods.getDeclaredMethods();
			Method[] javaMethods = new Method[invokers.size()];
			for (int i = 0; i < javaMethods.length; i++) {
				javaMethods[i] = invokers.get(i).createJavaMethod();
			}
			return javaMethods;
		}
	}

	public static Method[] jlClassGetMethods(Class<?> clazz) {
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable...
			return clazz.getMethods();
		}
		else {
			MethodProvider methods = MethodProvider.create(rtype);
			Collection<Invoker> invokers = methods.getMethods();
			Method[] javaMethods = new Method[invokers.size()];
			int i = 0;
			for (Invoker invoker : invokers) {
				javaMethods[i++] = invoker.createJavaMethod();
			}
			return javaMethods;
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static String toParamString(Class<?>[] params) {
		if (params == null || params.length == 0) {
			return "()";
		}
		StringBuilder s = new StringBuilder();
		s.append('(');
		for (int i = 0, max = params.length; i < max; i++) {
			if (i > 0) {
				s.append(", ");
			}
			if (params[i] == null) {
				s.append("null");
			}
			else {
				s.append(params[i].getName());
			}
		}
		s.append(')');
		return s.toString();
	}

	private static int depth = 4;

	/*
	 * Get the Class that declares the method calling interceptor method that called this method.
	 */
	@SuppressWarnings("deprecation")
	public static Class<?> getCallerClass() {
		//0 = sun.reflect.Reflection.getCallerClass
		//1 = this method's frame
		//2 = caller of 'getCallerClass' = asAccesibleMethod
		//3 = caller of 'asAccesibleMethod' = jlrInvoke
		//4 = caller we are interested in...

		// In jdk17u25 there is an extra frame inserted:
		// "This also fixes a regression introduced in 7u25 in which
		// getCallerClass(int) is now a Java method that adds an additional frame
		// that wasn't taken into account." in https://permalink.gmane.org/gmane.comp.java.openjdk.jdk7u.devel/6573
		Class<?> caller = sun.reflect.Reflection.getCallerClass(depth);
		if (caller == ReflectiveInterceptor.class) {
			// If this is true we have that extra frame on the stack
			depth = 5;
			caller = sun.reflect.Reflection.getCallerClass(depth);
		}

		String callerClassName = caller.getName();

		Matcher matcher = Constants.executorClassNamePattern.matcher(callerClassName);
		if (matcher.find()) {
			// Complication... the caller may in fact be an executor method...
			// in this case the caller will be an executor class.

			ClassLoader loader = caller.getClassLoader();
			try {
				return Class.forName(callerClassName.substring(0, matcher.start()), false, loader);
			}
			catch (ClassNotFoundException e) {
				//Supposedly it wasn't an executor class after all...
				log.log(Level.INFO, "Potential trouble determining caller of reflective method", e);
			}
		}
		return caller;
	}

	/**
	 * Called to satisfy an invocation of java.lang.Class.getDeclaredAnnotations().
	 *
	 * @param clazz the class upon which the original call was being invoked
	 * @return array of annotations on the class
	 */
	public static Annotation[] jlClassGetDeclaredAnnotations(Class<?> clazz) {
		if (TypeRegistry.nothingReloaded) {
			return clazz.getDeclaredAnnotations();
		}
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(clazz);
		if (rtype == null) {
			return clazz.getDeclaredAnnotations();
		}
		CurrentLiveVersion clv = rtype.getLiveVersion();
		return clv.getExecutorClass().getDeclaredAnnotations();
	}

	/*
	 * Called to satisfy an invocation of java.lang.Class.getDeclaredAnnotations().
	 *
	 * @param clazz the class upon which the original call was being invoked
	 */
	public static Annotation[] jlClassGetAnnotations(Class<?> clazz) {
		if (TypeRegistry.nothingReloaded) {
			return clazz.getAnnotations();
		}
		ReloadableType rtype = getRType(clazz);
		//Note: even if class has not been reloaded, it's superclass may have been and this may affect
		//  the inherited annotations, so we must *not* use 'getReloadableTypeIfHasBeenReloaded' above!
		if (rtype == null) {
			return clazz.getAnnotations();
		}

		Class<?> superClass = clazz.getSuperclass();
		if (superClass == null) {
			return jlClassGetDeclaredAnnotations(clazz); //Nothing to inherit so it's ok to call this
		}
		Map<Class<? extends Annotation>, Annotation> combinedAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();

		Annotation[] annotationsToAdd = jlClassGetAnnotations(superClass);
		for (Annotation annotation : annotationsToAdd) {
			if (isInheritable(annotation)) {
				combinedAnnotations.put(annotation.annotationType(), annotation);
			}
		}

		annotationsToAdd = jlClassGetDeclaredAnnotations(clazz);
		for (Annotation annotation : annotationsToAdd) {
			combinedAnnotations.put(annotation.annotationType(), annotation);
		}

		return combinedAnnotations.values().toArray(new Annotation[combinedAnnotations.size()]);
	}

	public static Annotation jlClassGetAnnotation(Class<?> clazz, Class<? extends Annotation> annoType) {
		ReloadableType rtype = getRType(clazz);
		//Note: even if class has not been reloaded, it's superclass may have been and this may affect
		//  the inherited annotations, so we must *not* use 'getReloadableTypeIfHasBeenReloaded' above!

		if (rtype == null) {
			return clazz.getAnnotation(annoType);
		}

		if (annoType == null) {
			throw new NullPointerException();
		}

		for (Annotation localAnnot : jlClassGetDeclaredAnnotations(clazz)) {
			if (localAnnot.annotationType() == annoType) {
				return localAnnot;
			}
		}

		if (annoType.isAnnotationPresent(Inherited.class)) {
			Class<?> superClass = clazz.getSuperclass();
			if (superClass != null) {
				return jlClassGetAnnotation(superClass, annoType);
			}
		}
		return null;
	}

	public static boolean jlClassIsAnnotationPresent(Class<?> clazz, Class<? extends Annotation> annoType) {
		ReloadableType rtype = getRType(clazz);
		//Note: even if class has not been reloaded, it's superclass may have been and this may affect
		//  the inherited annotations, so we must *not* use 'getReloadableTypeIfHasBeenReloaded' above!

		if (rtype == null) {
			return clazz.isAnnotationPresent(annoType);
		}
		return jlClassGetAnnotation(clazz, annoType) != null;
	}

	public static Constructor<?>[] jlClassGetDeclaredConstructors(Class<?> clazz) {
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Non reloadable type
			Constructor<?>[] cs = clazz.getDeclaredConstructors();
			return cs;
		}
		else if (!rtype.hasBeenReloaded()) {
			// Reloadable but not yet reloaded
			Constructor<?>[] cs = clazz.getDeclaredConstructors();
			int i = 0;
			for (Constructor<?> c : cs) {
				if (isMetaConstructor(clazz, c)) {
					// We must remove the 'special' constructor added by SpringLoaded
					continue;
				}
				// SpringLoaded changes modifiers, so must fix them
				fixModifier(rtype, c);
				cs[i++] = c;
			}
			return Utils.arrayCopyOf(cs, i);
		}
		else {
			CurrentLiveVersion liveVersion = rtype.getLiveVersion();
			// Reloaded type
			Constructor<?>[] clazzCs = null;
			TypeDescriptor desc = rtype.getLatestTypeDescriptor();
			MethodMember[] members = desc.getConstructors();
			Constructor<?>[] cs = new Constructor<?>[members.length];
			for (int i = 0; i < cs.length; i++) {
				MethodMember m = members[i];
				if (!liveVersion.hasConstructorChanged(m)) {
					if (clazzCs == null) {
						clazzCs = clazz.getDeclaredConstructors();
					}
					cs[i] = findConstructor(clazzCs, m);
					//					 SpringLoaded changes modifiers, so must fix them
					fixModifier(rtype, cs[i]);
				}
				else {
					cs[i] = newConstructor(rtype, m);
				}
			}
			return cs;
		}
	}

	private static Constructor<?> findConstructor(Constructor<?>[] constructors, MethodMember searchFor) {
		String paramDescriptor = searchFor.getDescriptor();
		for (int i = 0, max = constructors.length; i < max; i++) {
			String candidateDescriptor = Utils.toConstructorDescriptor(constructors[i].getParameterTypes());
			if (candidateDescriptor.equals(paramDescriptor)) {
				return constructors[i];
			}
		}
		return null;
	}

	private static boolean isMetaConstructor(Class<?> clazz, Constructor<?> c) {
		Class<?>[] params = c.getParameterTypes();
		if (clazz.isEnum()) {
			return params.length > 2 && params[2].getName().equals(Constants.magicDescriptorForGeneratedCtors);
		}
		else if (clazz.getSuperclass() != null && clazz.getSuperclass().getName().equals("groovy.lang.Closure")) {
			return params.length > 2 && params[2].getName().equals(Constants.magicDescriptorForGeneratedCtors);
		}
		else {
			return params.length > 0 && params[0].getName().equals(Constants.magicDescriptorForGeneratedCtors);
		}
	}

	private static Constructor<?> newConstructor(ReloadableType rtype, MethodMember m) {
		ClassLoader classLoader = rtype.getTypeRegistry().getClassLoader();
		try {
			return JVM.newConstructor(Utils.toClass(rtype), //declaring
					Utils.toParamClasses(m.getDescriptor(), classLoader), // params
					Utils.slashedNamesToClasses(m.getExceptions(), classLoader), //exceptions
					m.getModifiers(), //modifiers
					m.getGenericSignature() //signature
			);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("Couldn't create j.l.Constructor for " + m, e);
		}
	}

	private static void fixModifiers(ReloadableType rtype, Field[] fields) {
		TypeDescriptor typeDesc = rtype.getLatestTypeDescriptor();
		for (Field field : fields) {
			fixModifier(typeDesc, field);
		}
	}

	static void fixModifier(TypeDescriptor typeDesc, Field field) {
		int mods = typeDesc.getField(field.getName()).getModifiers();
		if (mods != field.getModifiers()) {
			JVM.setFieldModifiers(field, mods);
		}
	}

	protected static void fixModifier(ReloadableType rtype, Constructor<?> constructor) {
		String desc = Type.getConstructorDescriptor(constructor);
		MethodMember rCons = rtype.getCurrentConstructor(desc);
		if (constructor.getModifiers() != rCons.getModifiers()) {
			JVM.setConstructorModifiers(constructor, rCons.getModifiers());
		}
	}

	public static Constructor<?>[] jlClassGetConstructors(Class<?> clazz) {
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			return clazz.getConstructors();
		}
		else {
			Constructor<?>[] candidates = jlClassGetDeclaredConstructors(clazz);
			//We need to throw away any non-public constructors.
			List<Constructor<?>> keep = new ArrayList<Constructor<?>>(candidates.length);
			for (Constructor<?> candidate : candidates) {
				if (Modifier.isPublic(candidate.getModifiers())) {
					keep.add(candidate);
				}
			}
			return keep.toArray(new Constructor<?>[keep.size()]);
		}
	}

	public static Constructor<?> jlClassGetDeclaredConstructor(Class<?> clazz, Class<?>... params)
			throws SecurityException,
			NoSuchMethodException {
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Non reloadable type
			Constructor<?> c = clazz.getDeclaredConstructor(params);
			return c;
		}
		else if (!rtype.hasBeenReloaded()) {
			// Reloadable but not yet reloaded
			Constructor<?> c = clazz.getDeclaredConstructor(params);
			if (isMetaConstructor(clazz, c)) {
				// not a real constructor !
				throw Exceptions.noSuchConstructorException(clazz, params);
			}
			// SpringLoaded changes modifiers, so must fix them
			fixModifier(rtype, c);
			return c;
		}
		else {

			// This would be the right thing to do but makes getDeclaredConstructors() very messy
			CurrentLiveVersion clv = rtype.getLiveVersion();
			boolean b = clv.hasConstructorChanged(Utils.toConstructorDescriptor(params));
			if (!b) {
				Constructor<?> c = clazz.getDeclaredConstructor(params);
				if (isMetaConstructor(clazz, c)) {
					// not a real constructor !
					throw Exceptions.noSuchConstructorException(clazz, params);
				}
				// SpringLoaded changes modifiers, so must fix them
				fixModifier(rtype, c);
				return c;
			}
			else {
				// Reloaded type
				TypeDescriptor desc = rtype.getLatestTypeDescriptor();
				MethodMember[] members = desc.getConstructors();
				String searchFor = Utils.toConstructorDescriptor(params);
				for (MethodMember m : members) {
					if (m.getDescriptor().equals(searchFor)) {
						return newConstructor(rtype, m);
					}
				}
				throw Exceptions.noSuchConstructorException(clazz, params);
			}
		}
	}

	public static Constructor<?> jlClassGetConstructor(Class<?> clazz, Class<?>... params) throws SecurityException,
			NoSuchMethodException {
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			return clazz.getConstructor(params);
		}
		else {
			Constructor<?> c = jlClassGetDeclaredConstructor(clazz, params);
			if (Modifier.isPublic(c.getModifiers())) {
				return c;
			}
			else {
				throw Exceptions.noSuchMethodException(clazz, "<init>", params);
			}
		}
	}

	private static boolean isInheritable(Annotation annotation) {
		return annotation.annotationType().isAnnotationPresent(Inherited.class);
	}

	/**
	 * Performs access checks and returns a (potential) copy of the method with accessibility flag set if this necessary
	 * for the invoke to succeed.
	 * <p>
	 * Also checks for deleted methods.
	 * <p>
	 * If any checks fail, an appropriate exception is raised.
	 */
	private static Method asAccessibleMethod(ReloadableType methodDeclaringTypeReloadableType, Method method,
			Object target,
			boolean makeAccessibleCopy) throws IllegalAccessException {
		if (methodDeclaringTypeReloadableType != null && isDeleted(methodDeclaringTypeReloadableType, method)) {
			throw Exceptions.noSuchMethodError(method);
		}

		if (method.isAccessible()) {
			//More expensive check not required / copy not required
		}
		else {
			Class<?> clazz = method.getDeclaringClass();
			int mods = method.getModifiers();
			int classmods;

			//		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(clazz);
			if (methodDeclaringTypeReloadableType == null || !methodDeclaringTypeReloadableType.hasBeenReloaded()) {
				classmods = clazz.getModifiers();
			}
			else {
				//Note: the "super bit" may be set in class modifiers but we should block it out, it
				//shouldn't be shown to users of the reflection API.
				classmods = methodDeclaringTypeReloadableType.getLatestTypeDescriptor().getModifiers()
						& ~Opcodes.ACC_SUPER;
			}
			if (Modifier.isPublic(mods & classmods/*jlClassGetModifiers(clazz)*/)) {
				//More expensive check not required / copy not required
			}
			else {
				//More expensive check required
				Class<?> callerClass = getCallerClass();
				JVM.ensureMemberAccess(callerClass, clazz, target, mods);
				if (makeAccessibleCopy) {
					method = JVM.copyMethod(method); // copy: we must not change accessible flag on original method!
					method.setAccessible(true);
				}
			}
		}
		return makeAccessibleCopy ? method : null;
	}

	private static Constructor<?> asAccessibleConstructor(Constructor<?> c, boolean makeAccessibleCopy)
			throws NoSuchMethodException, IllegalAccessException {
		if (isDeleted(c)) {
			throw Exceptions.noSuchConstructorError(c);
		}
		Class<?> clazz = c.getDeclaringClass();
		int mods = c.getModifiers();
		if (c.isAccessible() || Modifier.isPublic(mods & jlClassGetModifiers(clazz))) {
			//More expensive check not required / copy not required
		}
		else {
			//More expensive check required
			Class<?> callerClass = getCallerClass();
			JVM.ensureMemberAccess(callerClass, clazz, null, mods);
			if (makeAccessibleCopy) {
				c = JVM.copyConstructor(c); // copy: we must not change accessible flag on original method!
				c.setAccessible(true);
			}
		}
		return makeAccessibleCopy ? c : null;
	}

	/**
	 * Performs access checks and returns a (potential) copy of the field with accessibility flag set if this necessary
	 * for the acces operation to succeed.
	 * <p>
	 * If any checks fail, an appropriate exception is raised.
	 *
	 * Warning this method is sensitive to stack depth! Should expects to be called DIRECTLY from a jlr redicriction
	 * method only!
	 */
	private static Field asAccessibleField(Field field, Object target, boolean makeAccessibleCopy)
			throws IllegalAccessException {
		if (isDeleted(field)) {
			throw Exceptions.noSuchFieldError(field);
		}
		Class<?> clazz = field.getDeclaringClass();
		int mods = field.getModifiers();
		if (field.isAccessible() || Modifier.isPublic(mods & jlClassGetModifiers(clazz))) {
			//More expensive check not required / copy not required
		}
		else {
			//More expensive check required
			Class<?> callerClass = getCallerClass();
			JVM.ensureMemberAccess(callerClass, clazz, target, mods);
			if (makeAccessibleCopy) {
				//TODO: This code is not covered by a test. It needs a non-reloadable type with non-public
				//  field, being accessed reflectively from a context that is "priviliged" to access it without setting the access flag.

				field = JVM.copyField(field); // copy: we must not change accessible flag on original method!
				field.setAccessible(true);
			}
		}
		return makeAccessibleCopy ? field : null;
	}

	/**
	 * Performs all necessary checks that need to be done before a field set should be allowed.
	 *
	 * @throws IllegalAccessException
	 */
	private static Field asSetableField(Field field, Object target, Class<?> valueType, Object value,
			boolean makeAccessibleCopy)
			throws IllegalAccessException {
		// Must do the checks exactly in the same order as JVM if we want identical error messages.

		// JVM doesn't do this, since it cannot happen without reloading, we do it first of all.
		if (isDeleted(field)) {
			throw Exceptions.noSuchFieldError(field);
		}

		Class<?> clazz = field.getDeclaringClass();
		int mods = field.getModifiers();
		if (field.isAccessible() || Modifier.isPublic(mods & jlClassGetModifiers(clazz))) {
			//More expensive check not required / copy not required
		}
		else {
			//More expensive check required
			Class<?> callerClass = getCallerClass();
			JVM.ensureMemberAccess(callerClass, clazz, target, mods);
			if (makeAccessibleCopy) {
				//TODO: This code is not covered by a test. It needs a non-reloadable type with non-public
				//  field, being accessed reflectively from a context that is "priviliged" to access it without setting the access flag.

				field = JVM.copyField(field); // copy: we must not change accessible flag on original field!
				field.setAccessible(true);
			}
		}
		if (isPrimitive(valueType)) {
			//It seems for primitive types, the order of the checks (in Sun JVM) is different!
			typeCheckFieldSet(field, valueType, value);
			if (!field.isAccessible() && Modifier.isFinal(mods)) {
				throw Exceptions.illegalSetFinalFieldException(field, field.getType(), coerce(value, field.getType()));
			}
		}
		else {
			if (!field.isAccessible() && Modifier.isFinal(mods)) {
				throw Exceptions.illegalSetFinalFieldException(field, valueType, value);
			}
			typeCheckFieldSet(field, valueType, value);
		}
		return makeAccessibleCopy ? field : null;
	}

	private static Object coerce(Object value, Class<?> toType) {
		//Warning: this method's implementation is not for general use, it's only intended use is to
		//  ensure correctness of error messages, so it doesn't need to cover all 'coercable' cases,
		//  only those cases where the coerced value print out differently, and which are reachable
		//  from 'asSetableField'.
		Class<? extends Object> fromType = value.getClass();
		if (Integer.class.equals(fromType)) {
			if (float.class.equals(toType)) {
				return (float) (Integer) value;
			}
			else if (double.class.equals(toType)) {
				return (double) (Integer) value;
			}
		}
		else if (Byte.class.equals(fromType)) {
			if (float.class.equals(toType)) {
				return (float) (Byte) value;
			}
			else if (double.class.equals(toType)) {
				return (double) (Byte) value;
			}
		}
		else if (Character.class.equals(fromType)) {
			if (int.class.equals(toType)) {
				return (int) (Character) value;
			}
			else if (long.class.equals(toType)) {
				return (long) (Character) value;
			}
			else if (float.class.equals(toType)) {
				return (float) (Character) value;
			}
			else if (double.class.equals(toType)) {
				return (double) (Character) value;
			}
		}
		else if (Short.class.equals(fromType)) {
			if (float.class.equals(toType)) {
				return (float) (Short) value;
			}
			else if (double.class.equals(toType)) {
				return (double) (Short) value;
			}
		}
		else if (Long.class.equals(fromType)) {
			if (float.class.equals(toType)) {
				return (float) (Long) value;
			}
			else if (double.class.equals(toType)) {
				return (double) (Long) value;
			}
		}
		else if (Float.class.equals(fromType)) {
			if (double.class.equals(toType)) {
				return (double) (Float) value;
			}
		}
		return value;
	}

	/**
	 * Perform a dynamic type check needed when setting a field value onto a field. Raises the appropriate exception
	 * when the check fails and returns normally otherwise. This method should only be called for object types. For
	 * primitive types call the three parameter variant instead.
	 *
	 * @throws IllegalAccessException
	 */
	private static void typeCheckFieldSet(Field field, Object value) throws IllegalAccessException {
		Class<?> fieldType = field.getType();
		if (value == null) {
			if (fieldType.isPrimitive()) {
				throw Exceptions.illegalSetFieldTypeException(field, null, value);
			}
		}
		else {
			if (fieldType.isPrimitive()) {
				fieldType = boxTypeFor(fieldType);
			}
			Class<?> valueType = value.getClass();
			if (!Utils.isConvertableFrom(fieldType, valueType)) {
				throw Exceptions.illegalSetFieldTypeException(field, valueType, value);
			}
		}
	}

	/**
	 * Perform a dynamic type check needed when setting a field value onto a field. Raises the appropriate exception
	 * when the check fails and returns normally otherwise.
	 *
	 * @throws IllegalAccessException
	 */
	private static void typeCheckFieldSet(Field field, Class<?> valueType, Object value) throws IllegalAccessException {
		if (!isPrimitive(valueType)) {
			//Call the version of this method that considers autoboxing
			typeCheckFieldSet(field, value);
		}
		else {
			//Value type is primitive.
			//  Note: In this case value was a primitive value that became boxed, so it can't be null.
			Class<?> fieldType = field.getType();
			if (!Utils.isConvertableFrom(fieldType, valueType)) {
				throw Exceptions.illegalSetFieldTypeException(field, valueType, value);
			}
		}
	}

	/**
	 * Checks whether given 'valueType' is a primitive type, considering that we use 'null' as the type for 'null' (to
	 * distinguish it from the type 'Object' which is not the same!)
	 */
	private static boolean isPrimitive(Class<?> valueType) {
		return valueType != null && valueType.isPrimitive();
	}

	/**
	 * Determine a "valueType" from a given value object. Note that this should really only be used for values that are
	 * non-primitive, otherwise it will be impossible to distinguish between a primitive value and its boxed
	 * representation.
	 * <p>
	 * In a context where you have a primitive value that gets boxed up, its valueType should be passed in explicitly as
	 * a class like, for example, int.class.
	 */
	private static Class<?> valueType(Object value) {
		if (value == null) {
			return null;
		}
		else {
			return value.getClass();
		}
	}

	/**
	 * Retrieve modifiers for a Java class, which might or might not be reloadable or reloaded.
	 *
	 * @param clazz the class for which to discover modifiers
	 * @return the modifiers
	 */
	public static int jlClassGetModifiers(Class<?> clazz) {
		//		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(clazz);
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			return clazz.getModifiers();
		}
		else {
			//Note: the "super bit" may be set in class modifiers but we should block it out, it
			//shouldn't be shown to users of the reflection API.
			return rtype.getLatestTypeDescriptor().getModifiers() & ~Opcodes.ACC_SUPER;
		}
	}

	private static boolean isDeleted(ReloadableType rtype, Method method) {
		//		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(method.getDeclaringClass());
		if (rtype == null || !rtype.hasBeenReloaded()) {
			return false;
		}
		else {
			MethodMember currentMethod = rtype.getCurrentMethod(method.getName(), Type.getMethodDescriptor(method));
			if (currentMethod == null) {
				return true; // Method not there, consider it deleted
			}
			else {
				return MethodMember.isDeleted(currentMethod); // Deleted bit is set consider deleted
			}
		}
	}

	private static boolean isDeleted(Constructor<?> c) {
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(c.getDeclaringClass());
		if (rtype == null) {
			return false;
		}
		else {
			TypeDescriptor desc = rtype.getLatestTypeDescriptor();
			MethodMember currentConstructor = desc.getConstructor(Type.getConstructorDescriptor(c));
			if (currentConstructor == null) {
				//TODO: test case with a deleted constructor
				return true; // Method not there, consider it deleted
			}
			else {
				return false;
			}
		}
	}

	private static boolean isDeleted(Field field) {
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(field.getDeclaringClass());
		if (rtype == null) {
			return false;
		}
		else {
			TypeDescriptor desc = rtype.getLatestTypeDescriptor();
			FieldMember currentField = desc.getField(field.getName());
			if (currentField == null) {
				return true; // Method not there, consider it deleted
			}
			else {
				return false;
			}
			// Fields don't have deleted bits now, but maybe they get them in the future?
			//			} else {
			//				return FieldMember.isDeleted(currentField); // Deleted bit is set consider deleted
			//			}
		}
	}

	/**
	 * If clazz is reloadable <b>and</b> has been reloaded at least once then return the ReloadableType instance for it,
	 * otherwise return null.
	 *
	 * @param clazz the type which may or may not be reloadable
	 * @return the reloadable type or null
	 */
	private static ReloadableType getReloadableTypeIfHasBeenReloaded(Class<?> clazz) {
		if (TypeRegistry.nothingReloaded) {
			return null;
		}
		ReloadableType rtype = getRType(clazz);
		if (rtype != null && rtype.hasBeenReloaded()) {
			return rtype;
		}
		else {
			return null;
		}
	}

	private final static boolean theOldWay = false;

	/**
	 * Access and return the ReloadableType field on a specified class.
	 *
	 * @param clazz the class for which to discover the reloadable type
	 * @return the reloadable type for the class, or null if not reloadable
	 */
	public static ReloadableType getRType(Class<?> clazz) {
		//		ReloadableType rtype = null;
		WeakReference<ReloadableType> ref = classToRType.get(clazz);
		ReloadableType rtype = null;
		if (ref != null) {
			rtype = ref.get();
		}
		if (rtype == null) {

			if (!theOldWay) {
				// 'theOldWay' attempts to grab the field from the type via reflection.  This usually works except
				// in cases where the class is not resolved yet since it can cause the class to resolve and its
				// static initializer to run.  This was happening on a grails compile where the compiler is
				// loading dependencies (but not initializing them).  Instead we can use this route of
				// discovering the type registry and locating the reloadable type.  This does some map lookups
				// which may be a problem, but once discovered, it is cached in the weak ref so that shouldn't
				// be an ongoing perf problem.

				// TODO testcases for something that is reloaded without having been resolved
				ClassLoader cl = clazz.getClassLoader();
				TypeRegistry tr = TypeRegistry.getTypeRegistryFor(cl);
				if (tr == null) {
					classToRType.put(clazz, ReloadableType.NOT_RELOADABLE_TYPE_REF);
				}
				else {
					rtype = tr.getReloadableType(clazz.getName().replace('.', '/'));
					if (rtype == null) {
						classToRType.put(clazz, ReloadableType.NOT_RELOADABLE_TYPE_REF);
					}
					else {
						classToRType.put(clazz, new WeakReference<ReloadableType>(rtype));
					}
				}
			}
			else {
				// need to work it out
				Field rtypeField;
				try {
					//				System.out.println("discovering field for " + clazz.getName());
					// TODO cache somewhere - will need a clazz>Field cache
					rtypeField = clazz.getDeclaredField(Constants.fReloadableTypeFieldName);
				}
				catch (NoSuchFieldException nsfe) {
					classToRType.put(clazz, ReloadableType.NOT_RELOADABLE_TYPE_REF);
					// expensive if constantly discovering this
					return null;
				}
				try {
					rtypeField.setAccessible(true);
					rtype = (ReloadableType) rtypeField.get(null);
					if (rtype == null) {
						classToRType.put(clazz, ReloadableType.NOT_RELOADABLE_TYPE_REF);
						throw new ReloadException("ReloadableType field '" + Constants.fReloadableTypeFieldName
								+ "' is 'null' on type " + clazz.getName());
					}
					else {
						classToRType.put(clazz, new WeakReference<ReloadableType>(rtype));
					}
				}
				catch (Exception e) {
					throw new ReloadException("Unable to access ReloadableType field '"
							+ Constants.fReloadableTypeFieldName
							+ "' on type " + clazz.getName(), e);
				}
			}
		}
		else if (rtype == ReloadableType.NOT_RELOADABLE_TYPE) {
			return null;
		}
		return rtype;
	}

	public static Annotation[] jlrMethodGetDeclaredAnnotations(Method method) {
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(method.getDeclaringClass());
		if (rtype == null) {
			//Nothing special to be done
			return method.getDeclaredAnnotations();
		}
		else {
			// Method could have changed...
			CurrentLiveVersion clv = rtype.getLiveVersion();
			MethodMember methodMember = rtype.getCurrentMethod(method.getName(), Type.getMethodDescriptor(method));
			if (MethodMember.isCatcher(methodMember)) {
				if (clv.getExecutorMethod(methodMember) != null) {
					throw new IllegalStateException();
				}
				return method.getDeclaredAnnotations();
			}
			Method executor = clv.getExecutorMethod(methodMember);
			return executor.getAnnotations();
		}
	}

	public static Annotation[][] jlrMethodGetParameterAnnotations(Method method) {
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(method.getDeclaringClass());
		if (rtype == null) {
			//Nothing special to be done
			return method.getParameterAnnotations();
		}
		else {
			// Method could have changed...
			CurrentLiveVersion clv = rtype.getLiveVersion();
			MethodMember currentMethod = rtype.getCurrentMethod(method.getName(), Type.getMethodDescriptor(method));
			Method executor = clv.getExecutorMethod(currentMethod);
			Annotation[][] result = executor.getParameterAnnotations();
			if (!currentMethod.isStatic()) {
				//Non=static methods have an extra param.
				//Though extra param is added to front...
				//Annotations aren't being moved so we have to actually drop the *last* array element
				result = Utils.arrayCopyOf(result, result.length - 1);
			}
			return result;
		}
	}

	public static Object jlClassNewInstance(Class<?> clazz) throws SecurityException, NoSuchMethodException,
			IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		// Note: no special case for non-reloadable types here, because access checks:
		//    access checks depend on stack depth and springloaded rewriting changes that even for non-reloadable types!

		// TODO: This implementation doesn't check access modifiers on the class. So may allow
		//   instantiations that wouldn't be allowed by the JVM (e.g if constructor is public, but class is private)

		// TODO: what about trying to instantiate an abstract class? should produce an error, does it?

		Constructor<?> c;
		try {
			c = jlClassGetDeclaredConstructor(clazz);
		}
		catch (NoSuchMethodException e) {
			//			e.printStackTrace();
			throw Exceptions.instantiation(clazz);
		}
		c = asAccessibleConstructor(c, true);
		return jlrConstructorNewInstance(c);
	}

	public static Object jlrConstructorNewInstance(Constructor<?> c, Object... params) throws InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException,
			NoSuchMethodException {
		//Note: unlike for methods we don't need to handle the reloadable but not reloaded case specially, that is because there
		// is no inheritance on constructors, so reloaded superclasses can affect method lookup in the same way.

		Class<?> clazz = c.getDeclaringClass();
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(clazz);
		if (rtype == null) {
			c = asAccessibleConstructor(c, true);
			//Nothing special to be done
			return c.newInstance(params);
		}
		else {
			// Constructor may have changed...
			// this is the right thing to do but makes a mess of getDeclaredConstructors (and affects getDeclaredConstructor)
			//			// TODO  should check about constructor changing
			//			rtype.getTypeDescriptor().getConstructor("").
			boolean ctorChanged = rtype.getLiveVersion().hasConstructorChanged(
					Utils.toConstructorDescriptor(c.getParameterTypes()));
			if (!ctorChanged) {
				// if we let the getDeclaredConstructor(s) code run as is, it may create invalid ctors, if we want to run the real one we should discover it here and use it.
				// would it be cheaper to fix up getDeclaredConstructor to always return valid ones if we are going to use them, or should we intercept here? probably the former...

				c = asAccessibleConstructor(c, true);
				return c.newInstance(params);
			}
			asAccessibleConstructor(c, false);
			CurrentLiveVersion clv = rtype.getLiveVersion();
			Method executor = clv.getExecutorMethod(rtype.getCurrentConstructor(Type.getConstructorDescriptor(c)));
			Constructor<?> magicConstructor = clazz.getConstructor(C.class);
			Object instance = magicConstructor.newInstance((Object) null);

			Object[] instanceAndParams;
			if (params == null || params.length == 0) {
				instanceAndParams = new Object[] { instance };
			}
			else {
				//Must add instance as first param: executor is a static method.
				instanceAndParams = new Object[params.length + 1];
				instanceAndParams[0] = instance;
				System.arraycopy(params, 0, instanceAndParams, 1, params.length);
			}
			executor.invoke(null, instanceAndParams);
			return instance;
		}
	}

	//	private static String toString(Object... params) {
	//		if (params == null) {
	//			return "null";
	//		}
	//		StringBuilder s = new StringBuilder();
	//		for (Object param : params) {
	//			s.append(param).append(" ");
	//		}
	//		return "[" + s.toString().trim() + "]";
	//	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object jlrMethodInvoke(Method method, Object target, Object... params)
			throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		//		System.out.println("> jlrMethodInvoke:method=" + method + " target=" + target + " params=" + toString(params));
		Class declaringClass = method.getDeclaringClass();
		if (declaringClass == Class.class) {
			String mname = method.getName();
			try {
				if (mname.equals("getFields")) {
					return jlClassGetFields((Class) target);
				}
				else if (mname.equals("getDeclaredFields")) {
					return jlClassGetDeclaredFields((Class) target);
				}
				else if (mname.equals("getDeclaredField")) {
					return jlClassGetDeclaredField((Class) target, (String) params[0]);
				}
				else if (mname.equals("getField")) {
					return jlClassGetField((Class) target, (String) params[0]);
				}
				else if (mname.equals("getConstructors")) {
					return jlClassGetConstructors((Class) target);
				}
				else if (mname.equals("getDeclaredConstructors")) {
					return jlClassGetDeclaredConstructors((Class) target);
				}
				else if (mname.equals("getDeclaredMethod")) {
					return jlClassGetDeclaredMethod((Class) target, (String) params[0], (Class[]) params[1]);
				}
				else if (mname.equals("getDeclaredMethods")) {
					return jlClassGetDeclaredMethods((Class) target);
				}
				else if (mname.equals("getMethod")) {
					return jlClassGetMethod((Class) target, (String) params[0], (Class[]) params[1]);
				}
				else if (mname.equals("getMethods")) {
					return jlClassGetMethods((Class) target);
				}
				else if (mname.equals("getConstructor")) {
					return jlClassGetConstructor((Class) target, (Class[]) params[0]);
				}
				else if (mname.equals("getDeclaredConstructor")) {
					return jlClassGetDeclaredConstructor((Class) target, (Class[]) params[0]);
				}
				else if (mname.equals("getModifiers")) {
					return jlClassGetModifiers((Class) target);
				}
				else if (mname.equals("isAnnotationPresent")) {
					return jlClassIsAnnotationPresent((Class) target, (Class<? extends Annotation>) params[0]);
				}
				else if (mname.equals("newInstance")) {
					return jlClassNewInstance((Class) target);
				}
				else if (mname.equals("getDeclaredAnnotations")) {
					return jlClassGetDeclaredAnnotations((Class) target);
				}
				else if (mname.equals("getAnnotation")) {
					return jlClassGetAnnotation((Class) target, (Class) params[0]);
				}
				else if (mname.equals("getAnnotations")) {
					return jlClassGetAnnotations((Class) target);
				}
			}
			catch (NoSuchMethodException nsme) {
				throw new InvocationTargetException(nsme);
			}
			catch (NoSuchFieldException nsfe) {
				throw new InvocationTargetException(nsfe);
			}
			catch (InstantiationException ie) {
				throw new InvocationTargetException(ie);
			}
		}
		else if (declaringClass == Method.class) {
			String mname = method.getName();
			if (mname.equals("invoke")) {
				return jlrMethodInvoke((Method) target, params[0], (Object[]) params[1]);
			}
			else if (mname.equals("getAnnotation")) {
				return jlrMethodGetAnnotation((Method) target, (Class) params[0]);
			}
			else if (mname.equals("getAnnotations")) {
				return jlrMethodGetAnnotations((Method) target);
			}
			else if (mname.equals("getDeclaredAnnotations")) {
				return jlrMethodGetDeclaredAnnotations((Method) target);
			}
			else if (mname.equals("getParameterAnnotations")) {
				return jlrMethodGetParameterAnnotations((Method) target);
			}
			else if (mname.equals("isAnnotationPresent")) {
				return jlrMethodIsAnnotationPresent((Method) target, (Class) params[0]);
			}
		}
		else if (declaringClass == Constructor.class) {
			String mname = method.getName();
			try {
				if (mname.equals("getAnnotation")) {
					return jlrConstructorGetAnnotation((Constructor) target, (Class) params[0]);
				}
				else if (mname.equals("newInstance")) {
					return jlrConstructorNewInstance((Constructor) target, (Object[]) params[0]);
				}
				else if (mname.equals("getAnnotations")) {
					return jlrConstructorGetAnnotations((Constructor) target);
				}
				else if (mname.equals("getDeclaredAnnotations")) {
					return jlrConstructorGetDeclaredAnnotations((Constructor) target);
				}
				else if (mname.equals("isAnnotationPresent")) {
					return jlrConstructorIsAnnotationPresent((Constructor) target, (Class) params[0]);
				}
				else if (mname.equals("getParameterAnnotations")) {
					return jlrConstructorGetParameterAnnotations((Constructor) target);
				}
			}
			catch (InstantiationException ie) {
				throw new InvocationTargetException(ie);
			}
			catch (NoSuchMethodException nsme) {
				throw new InvocationTargetException(nsme);
			}
		}
		else if (declaringClass == Field.class) {
			String mname = method.getName();
			if (mname.equals("set")) {
				jlrFieldSet((Field) target, params[0], params[1]);
				return null;
			}
			else if (mname.equals("setBoolean")) {
				jlrFieldSetBoolean((Field) target, params[0], (Boolean) params[1]);
				return null;
			}
			else if (mname.equals("setByte")) {
				jlrFieldSetByte((Field) target, params[0], (Byte) params[1]);
				return null;
			}
			else if (mname.equals("setChar")) {
				jlrFieldSetChar((Field) target, params[0], (Character) params[1]);
				return null;
			}
			else if (mname.equals("setFloat")) {
				jlrFieldSetFloat((Field) target, params[0], (Float) params[1]);
				return null;
			}
			else if (mname.equals("setShort")) {
				jlrFieldSetShort((Field) target, params[0], (Short) params[1]);
				return null;
			}
			else if (mname.equals("setLong")) {
				jlrFieldSetLong((Field) target, params[0], (Long) params[1]);
				return null;
			}
			else if (mname.equals("setDouble")) {
				jlrFieldSetDouble((Field) target, params[0], (Double) params[1]);
				return null;
			}
			else if (mname.equals("setInt")) {
				jlrFieldSetInt((Field) target, params[0], (Integer) params[1]);
				return null;
			}
			else if (mname.equals("get")) {
				return jlrFieldGet((Field) target, params[0]);
			}
			else if (mname.equals("getByte")) {
				return jlrFieldGetByte((Field) target, params[0]);
			}
			else if (mname.equals("getChar")) {
				return jlrFieldGetChar((Field) target, params[0]);
			}
			else if (mname.equals("getDouble")) {
				return jlrFieldGetDouble((Field) target, params[0]);
			}
			else if (mname.equals("getBoolean")) {
				return jlrFieldGetBoolean((Field) target, params[0]);
			}
			else if (mname.equals("getLong")) {
				return jlrFieldGetLong((Field) target, params[0]);
			}
			else if (mname.equals("getFloat")) {
				return jlrFieldGetFloat((Field) target, params[0]);
			}
			else if (mname.equals("getInt")) {
				return jlrFieldGetInt((Field) target, params[0]);
			}
			else if (mname.equals("getShort")) {
				return jlrFieldGetShort((Field) target, params[0]);
			}
			else if (mname.equals("getAnnotations")) {
				return jlrFieldGetAnnotations((Field) target);
			}
			else if (mname.equals("getDeclaredAnnotations")) {
				return jlrFieldGetDeclaredAnnotations((Field) target);
			}
			else if (mname.equals("isAnnotationPresent")) {
				return jlrFieldIsAnnotationPresent((Field) target, (Class) params[0]);
			}
			else if (mname.equals("getAnnotation")) {
				return jlrFieldGetAnnotation((Field) target, (Class) params[0]);
			}
		}
		else if (declaringClass == AccessibleObject.class) {
			String mname = method.getName();
			if (mname.equals("isAnnotationPresent")) {
				if (target instanceof Constructor) {
					// TODO what about null target - how should things go bang?
					return jlrConstructorIsAnnotationPresent((Constructor) target, (Class) params[0]);
				}
				else if (target instanceof Method) {
					return jlrMethodIsAnnotationPresent((Method) target, (Class) params[0]);
				}
				else if (target instanceof Field) {
					return jlrFieldIsAnnotationPresent((Field) target, (Class) params[0]);
				}
			}
			else if (mname.equals("getAnnotations")) {
				if (target instanceof Constructor) {
					return jlrConstructorGetAnnotations((Constructor) target);
				}
				else if (target instanceof Method) {
					return jlrMethodGetAnnotations((Method) target);
				}
				else if (target instanceof Field) {
					return jlrFieldGetAnnotations((Field) target);
				}
			}
			else if (mname.equals("getDeclaredAnnotations")) {
				if (target instanceof Constructor) {
					return jlrConstructorGetDeclaredAnnotations((Constructor) target);
				}
				else if (target instanceof Method) {
					return jlrMethodGetDeclaredAnnotations((Method) target);
				}
				else if (target instanceof Field) {
					return jlrFieldGetDeclaredAnnotations((Field) target);
				}
			}
			else if (mname.equals("getAnnotation")) {
				if (target instanceof Constructor) {
					return jlrConstructorGetAnnotation((Constructor) target, (Class) params[0]);
				}
				else if (target instanceof Method) {
					return jlrMethodGetAnnotation((Method) target, (Class) params[0]);
				}
				else if (target instanceof Field) {
					return jlrFieldGetAnnotation((Field) target, (Class) params[0]);
				}
			}
		}
		else if (declaringClass == AnnotatedElement.class) {
			String mname = method.getName();
			if (mname.equals("isAnnotationPresent")) {
				if (target instanceof Constructor) {
					// TODO what about null target - how should things go bang?
					return jlrConstructorIsAnnotationPresent((Constructor) target, (Class) params[0]);
				}
				else if (target instanceof Method) {
					return jlrMethodIsAnnotationPresent((Method) target, (Class) params[0]);
				}
				else if (target instanceof Field) {
					return jlrFieldIsAnnotationPresent((Field) target, (Class) params[0]);
				}
			}
			else if (mname.equals("getAnnotations")) {
				if (target instanceof Constructor) {
					return jlrConstructorGetAnnotations((Constructor) target);
				}
				else if (target instanceof Method) {
					return jlrMethodGetAnnotations((Method) target);
				}
				else if (target instanceof Field) {
					return jlrFieldGetAnnotations((Field) target);
				}
			}
			else if (mname.equals("getDeclaredAnnotations")) {
				if (target instanceof Constructor) {
					return jlrConstructorGetDeclaredAnnotations((Constructor) target);
				}
				else if (target instanceof Method) {
					return jlrMethodGetDeclaredAnnotations((Method) target);
				}
				else if (target instanceof Field) {
					return jlrFieldGetDeclaredAnnotations((Field) target);
				}
			}
			else if (mname.equals("getAnnotation")) {
				if (target instanceof Constructor) {
					return jlrConstructorGetAnnotation((Constructor) target, (Class) params[0]);
				}
				else if (target instanceof Method) {
					return jlrMethodGetAnnotation((Method) target, (Class) params[0]);
				}
				else if (target instanceof Field) {
					return jlrFieldGetAnnotation((Field) target, (Class) params[0]);
				}
			}
		}

		// Even though we tinker with the visibility of methods, we don't damage private ones (which would really cause chaos if we tried
		// to allow the JVM to do the dispatch).  That means this should be OK:
		if (TypeRegistry.nothingReloaded) {
			method = asAccessibleMethod(null, method, target, true);
			return method.invoke(target, params);
		}
		ReloadableType declaringType = getRType(declaringClass);
		if (declaringType == null) {
			//Not reloadable...
			method = asAccessibleMethod(declaringType, method, target, true);
			return method.invoke(target, params);
		}
		else {
			//Reloadable...
			asAccessibleMethod(declaringType, method, target, false);
			int mods = method.getModifiers();
			Invoker invoker;
			if ((mods & (Modifier.STATIC | Modifier.PRIVATE)) != 0) {
				//These methods are dispatched statically
				MethodProvider methods = MethodProvider.create(declaringType);
				invoker = methods.staticLookup(mods, method.getName(), Type.getMethodDescriptor(method));
			}
			else {
				//These methods are dispatched dynamically
				ReloadableType targetType = getRType(target.getClass()); //NPE possible but is what should happen here!
				if (targetType == null) {
					if (GlobalConfiguration.verboseMode) {
						System.out.println("UNEXPECTED: Subtype '"
								+ target.getClass().getName()
								+ "' of reloadable type "
								+ method.getDeclaringClass().getName()
								+ " is not reloadable: may not see changes reloaded in this hierarchy");
					}
					method = asAccessibleMethod(declaringType, method, target, true);
					return method.invoke(target, params);
				}
				MethodProvider methods = MethodProvider.create(targetType); //use target not declaring type for Dynamic lookkup
				invoker = methods.dynamicLookup(mods, method.getName(), Type.getMethodDescriptor(method));
			}
			return invoker.invoke(target, params);
		}
	}

	public static boolean jlrMethodIsAnnotationPresent(Method method, Class<? extends Annotation> annotClass) {
		return jlrMethodGetAnnotation(method, annotClass) != null;
	}

	public static Annotation jlrMethodGetAnnotation(Method method, Class<? extends Annotation> annotClass) {
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(method.getDeclaringClass());
		if (rtype == null) {
			//Nothing special to be done
			return method.getAnnotation(annotClass);
		}
		else {
			if (annotClass == null) {
				throw new NullPointerException();
			}
			// Method could have changed...
			Annotation[] annots = jlrMethodGetDeclaredAnnotations(method);
			for (Annotation annotation : annots) {
				if (annotClass.equals(annotation.annotationType())) {
					return annotation;
				}
			}
			return null;
		}
	}

	public static Annotation[] jlrAnnotatedElementGetAnnotations(AnnotatedElement elem) {
		if (elem instanceof Class<?>) {
			return jlClassGetAnnotations((Class<?>) elem);
		}
		else if (elem instanceof AccessibleObject) {
			return jlrAccessibleObjectGetAnnotations((AccessibleObject) elem);
		}
		else {
			//Don't know what it is... not something we handle anyway
			return elem.getAnnotations();
		}
	}

	public static Annotation[] jlrAnnotatedElementGetDeclaredAnnotations(AnnotatedElement elem) {
		if (elem instanceof Class<?>) {
			return jlClassGetDeclaredAnnotations((Class<?>) elem);
		}
		else if (elem instanceof AccessibleObject) {
			return jlrAccessibleObjectGetDeclaredAnnotations((AccessibleObject) elem);
		}
		else {
			//Don't know what it is... not something we handle anyway
			return elem.getDeclaredAnnotations();
		}
	}

	public static Annotation[] jlrAccessibleObjectGetDeclaredAnnotations(AccessibleObject obj) {
		if (obj instanceof Method) {
			return jlrMethodGetDeclaredAnnotations((Method) obj);
		}
		else if (obj instanceof Field) {
			return jlrFieldGetDeclaredAnnotations((Field) obj);
		}
		else if (obj instanceof Constructor<?>) {
			return jlrConstructorGetDeclaredAnnotations((Constructor<?>) obj);
		}
		else {
			//Some other type of member which we don't support reloading...
			return obj.getDeclaredAnnotations();
		}
	}

	public static Annotation[] jlrFieldGetDeclaredAnnotations(Field field) {
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(field.getDeclaringClass());
		if (rtype == null) {
			//Nothing special to be done
			return field.getDeclaredAnnotations();
		}
		else {
			// Field could have changed...
			CurrentLiveVersion clv = rtype.getLiveVersion();
			Field executor;
			try {
				executor = clv.getExecutorField(field.getName());
				return executor.getAnnotations();
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public static boolean jlrFieldIsAnnotationPresent(Field field, Class<? extends Annotation> annotType) {
		if (annotType == null) {
			throw new NullPointerException();
		}
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(field.getDeclaringClass());
		if (rtype == null) {
			//Nothing special to be done
			return field.isAnnotationPresent(annotType);
		}
		else {
			// Field could have changed...
			CurrentLiveVersion clv = rtype.getLiveVersion();
			try {
				Field executor = clv.getExecutorField(field.getName());
				return executor.isAnnotationPresent(annotType);
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public static Annotation[] jlrFieldGetAnnotations(Field field) {
		//Fields do not inherit annotations so we can just call...
		return jlrFieldGetDeclaredAnnotations(field);
	}

	public static Annotation[] jlrAccessibleObjectGetAnnotations(AccessibleObject obj) {
		if (obj instanceof Method) {
			return jlrMethodGetAnnotations((Method) obj);
		}
		else if (obj instanceof Field) {
			return jlrFieldGetAnnotations((Field) obj);
		}
		else if (obj instanceof Constructor<?>) {
			return jlrConstructorGetAnnotations((Constructor<?>) obj);
		}
		else {
			//Some other type of member which we don't support reloading...
			// (actually there are really no other cases any more!)
			return obj.getAnnotations();
		}
	}

	public static Annotation[] jlrConstructorGetAnnotations(Constructor<?> c) {
		return jlrConstructorGetDeclaredAnnotations(c);
	}

	public static Annotation[] jlrConstructorGetDeclaredAnnotations(Constructor<?> c) {
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(c.getDeclaringClass());
		if (rtype == null) {
			//Nothing special to be done
			return c.getDeclaredAnnotations();
		}
		else {
			// Constructor could have changed...
			CurrentLiveVersion clv = rtype.getLiveVersion();
			Method executor = clv.getExecutorMethod(rtype.getCurrentConstructor(Type.getConstructorDescriptor(c)));
			return executor.getAnnotations();
		}
	}

	public static Annotation jlrConstructorGetAnnotation(Constructor<?> c, Class<? extends Annotation> annotType) {
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(c.getDeclaringClass());
		if (rtype == null) {
			//Nothing special to be done
			return c.getAnnotation(annotType);
		}
		else {
			// Constructor could have changed...
			CurrentLiveVersion clv = rtype.getLiveVersion();
			Method executor = clv.getExecutorMethod(rtype.getCurrentConstructor(Type.getConstructorDescriptor(c)));
			return executor.getAnnotation(annotType);
		}
	}

	public static Annotation[][] jlrConstructorGetParameterAnnotations(Constructor<?> c) {
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(c.getDeclaringClass());
		if (rtype == null) {
			//Nothing special to be done
			return c.getParameterAnnotations();
		}
		else {
			// Method could have changed...
			CurrentLiveVersion clv = rtype.getLiveVersion();
			MethodMember currentConstructor = rtype.getCurrentConstructor(Type.getConstructorDescriptor(c));
			Method executor = clv.getExecutorMethod(currentConstructor);
			Annotation[][] result = executor.getParameterAnnotations();
			//Constructor executor methods have an extra param.
			//Though extra param is added to front... annotations aren't being moved so we have to actually drop
			//the *last* array element
			result = Utils.arrayCopyOf(result, result.length - 1);
			return result;
		}
	}

	public static boolean jlrConstructorIsAnnotationPresent(Constructor<?> c, Class<? extends Annotation> annotType) {
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(c.getDeclaringClass());
		if (rtype == null) {
			//Nothing special to be done
			return c.isAnnotationPresent(annotType);
		}
		else {
			// Constructor could have changed...
			CurrentLiveVersion clv = rtype.getLiveVersion();
			Method executor = clv.getExecutorMethod(rtype.getCurrentConstructor(Type.getConstructorDescriptor(c)));
			return executor.isAnnotationPresent(annotType);
		}
	}

	public static Annotation jlrFieldGetAnnotation(Field field, Class<? extends Annotation> annotType) {
		if (annotType == null) {
			throw new NullPointerException();
		}
		ReloadableType rtype = getReloadableTypeIfHasBeenReloaded(field.getDeclaringClass());
		if (rtype == null) {
			//Nothing special to be done
			return field.getAnnotation(annotType);
		}
		else {
			// Field could have changed...
			CurrentLiveVersion clv = rtype.getLiveVersion();
			try {
				Field executor = clv.getExecutorField(field.getName());
				return executor.getAnnotation(annotType);
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public static Annotation[] jlrMethodGetAnnotations(Method method) {
		return jlrMethodGetDeclaredAnnotations(method);
	}

	public static boolean jlrAnnotatedElementIsAnnotationPresent(AnnotatedElement elem,
			Class<? extends Annotation> annotType) {
		if (elem instanceof Class<?>) {
			return jlClassIsAnnotationPresent((Class<?>) elem, annotType);
		}
		else if (elem instanceof AccessibleObject) {
			return jlrAccessibleObjectIsAnnotationPresent((AccessibleObject) elem, annotType);
		}
		else {
			//Don't know what it is... not something we handle anyway
			return elem.isAnnotationPresent(annotType);
		}
	}

	public static boolean jlrAccessibleObjectIsAnnotationPresent(AccessibleObject obj,
			Class<? extends Annotation> annotType) {
		if (obj instanceof Method) {
			return jlrMethodIsAnnotationPresent((Method) obj, annotType);
		}
		else if (obj instanceof Field) {
			return jlrFieldIsAnnotationPresent((Field) obj, annotType);
		}
		else if (obj instanceof Constructor) {
			return jlrConstructorIsAnnotationPresent((Constructor<?>) obj, annotType);
		}
		else {
			//Some other type of member which we don't support reloading...
			return obj.isAnnotationPresent(annotType);
		}
	}

	public static Annotation jlrAnnotatedElementGetAnnotation(AnnotatedElement elem,
			Class<? extends Annotation> annotType) {
		if (elem instanceof Class<?>) {
			return jlClassGetAnnotation((Class<?>) elem, annotType);
		}
		else if (elem instanceof AccessibleObject) {
			return jlrAccessibleObjectGetAnnotation((AccessibleObject) elem, annotType);
		}
		else {
			//Don't know what it is... not something we handle anyway
			// Note: only thing it can be is probably java.lang.Package
			return elem.getAnnotation(annotType);
		}
	}

	public static Annotation jlrAccessibleObjectGetAnnotation(AccessibleObject obj,
			Class<? extends Annotation> annotType) {
		if (obj instanceof Method) {
			return jlrMethodGetAnnotation((Method) obj, annotType);
		}
		else if (obj instanceof Field) {
			return jlrFieldGetAnnotation((Field) obj, annotType);
		}
		else if (obj instanceof Constructor<?>) {
			return jlrConstructorGetAnnotation((Constructor<?>) obj, annotType);
		}
		else {
			//Some other type of member which we don't support reloading...
			return obj.getAnnotation(annotType);
		}
	}

	public static Field jlClassGetField(Class<?> clazz, String name) throws SecurityException, NoSuchFieldException {
		ReloadableType rtype = getRType(clazz);
		if (name.startsWith(Constants.PREFIX)) {
			throw Exceptions.noSuchFieldException(name);
		}
		if (rtype == null) {
			//Not reloadable
			return clazz.getField(name);
		}
		else {
			//Reloadable
			Field f = GetFieldLookup.lookup(rtype, name);
			if (f != null) {
				return f;
			}
			throw Exceptions.noSuchFieldException(name);
		}
	}

	public static Field jlClassGetDeclaredField(Class<?> clazz, String name) throws SecurityException,
			NoSuchFieldException {
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			return clazz.getDeclaredField(name);
		}
		else if (name.startsWith(Constants.PREFIX)) {
			throw Exceptions.noSuchFieldException(name);
		}
		else if (!rtype.hasBeenReloaded()) {
			Field f = clazz.getDeclaredField(name);
			fixModifier(rtype.getLatestTypeDescriptor(), f);
			return f;
		}
		else {
			Field f = GetDeclaredFieldLookup.lookup(rtype, name);
			if (f == null) {
				throw Exceptions.noSuchFieldException(name);
			}
			else {
				return f;
			}
		}
	}

	public static Field[] jlClassGetDeclaredFields(Class<?> clazz) {
		Field[] fields = clazz.getDeclaredFields();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			return fields;
		}
		else {
			if (!rtype.hasBeenReloaded()) {
				//Not reloaded yet...
				fields = removeMetaFields(fields);
				fixModifiers(rtype, fields);
				return fields;
			}
			else {
				// Was reloaded, it's up to us to create the field objects
				TypeDescriptor typeDesc = rtype.getLatestTypeDescriptor();
				FieldMember[] members = typeDesc.getFields();
				fields = new Field[members.length];
				int i = 0;
				for (FieldMember f : members) {
					String fieldTypeDescriptor = f.getDescriptor();
					Class<?> type;
					try {
						type = Utils.toClass(Type.getType(fieldTypeDescriptor), rtype.typeRegistry.getClassLoader());
					}
					catch (ClassNotFoundException e) {
						throw new IllegalStateException(e);
					}
					fields[i++] = JVM.newField(clazz, type, f.getModifiers(), f.getName(), f.getGenericSignature());
				}
				if (GlobalConfiguration.assertsMode) {
					Utils.assertTrue(i == fields.length, "Bug: unexpected number of fields");
				}
				return fields;
			}
		}
	}

	/**
	 * Given a list of fields filter out those fields that are created by springloaded (leaving only the "genuine"
	 * fields)
	 */
	private static Field[] removeMetaFields(Field[] fields) {
		Field[] realFields = new Field[fields.length - 1];
		//We'll delete at least one, sometimes more than one field (because there's at least the r$type field).
		int i = 0;
		for (Field field : fields) {
			if (!field.getName().startsWith(Constants.PREFIX)) {
				realFields[i++] = field;
			}
		}
		if (i < realFields.length) {
			realFields = Utils.arrayCopyOf(realFields, i);
		}
		if (GlobalConfiguration.assertsMode) {
			Utils.assertTrue(i == realFields.length, "Bug in removeMetaFields, created array of wrong length");
		}
		return realFields;
	}

	/**
	 * Although fields are not reloadable, we have to intercept this because otherwise we'll return the r$type field as
	 * a result here.
	 *
	 * @param clazz the class for which to retrieve the fields
	 * @return array of fields in the class
	 */
	public static Field[] jlClassGetFields(Class<?> clazz) {
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			return clazz.getFields();
		}
		else {
			List<Field> allFields = new ArrayList<Field>();
			gatherFields(clazz, allFields, new HashSet<Class<?>>());
			return allFields.toArray(new Field[allFields.size()]);
		}
	}

	/**
	 * Gather up all (public) fields in an interface and all its super interfaces recursively.
	 *
	 * @param clazz the class for which to collect up fields
	 * @param collected a collector that has fields added to it as this method runs (recursively)
	 * @param visited a set recording which types have already been visited
	 */
	private static void gatherFields(Class<?> clazz, List<Field> collected, HashSet<Class<?>> visited) {
		if (visited.contains(clazz)) {
			return;
		}
		visited.add(clazz);
		Field[] fields = jlClassGetDeclaredFields(clazz);
		for (Field f : fields) {
			if (Modifier.isPublic(f.getModifiers())) {
				collected.add(f);
			}
		}
		if (!clazz.isInterface()) {
			Class<?> supr = clazz.getSuperclass();
			if (supr != null) {
				gatherFields(supr, collected, visited);
			}
		}
		for (Class<?> itf : clazz.getInterfaces()) {
			gatherFields(itf, collected, visited);
		}
	}

	public static Object jlrFieldGet(Field field, Object target) throws IllegalArgumentException,
			IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			field = asAccessibleField(field, target, true);
			return field.get(target);
		}
		else {
			asAccessibleField(field, target, false);
			return rtype.getField(target, field.getName(), Modifier.isStatic(field.getModifiers()));
		}
	}

	public static int jlrFieldGetInt(Field field, Object target) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			field = asAccessibleField(field, target, true);
			return field.getInt(target);
		}
		else {
			asAccessibleField(field, target, false);
			typeCheckFieldGet(field, int.class);
			Object value = rtype.getField(target, field.getName(), Modifier.isStatic(field.getModifiers()));
			if (value instanceof Character) {
				return ((Character) value).charValue();
			}
			else {
				return ((Number) value).intValue();
			}
		}
	}

	public static byte jlrFieldGetByte(Field field, Object target) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			field = asAccessibleField(field, target, true);
			return field.getByte(target);
		}
		else {
			asAccessibleField(field, target, false);
			typeCheckFieldGet(field, byte.class);
			Object value = rtype.getField(target, field.getName(), Modifier.isStatic(field.getModifiers()));
			return ((Number) value).byteValue();
		}
	}

	public static char jlrFieldGetChar(Field field, Object target) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			field = asAccessibleField(field, target, true);
			return field.getChar(target);
		}
		else {
			asAccessibleField(field, target, false);
			typeCheckFieldGet(field, char.class);
			Object value = rtype.getField(target, field.getName(), Modifier.isStatic(field.getModifiers()));
			return ((Character) value).charValue();
		}
	}

	public static short jlrFieldGetShort(Field field, Object target) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			field = asAccessibleField(field, target, true);
			return field.getShort(target);
		}
		else {
			asAccessibleField(field, target, false);
			typeCheckFieldGet(field, short.class);
			Object value = rtype.getField(target, field.getName(), Modifier.isStatic(field.getModifiers()));
			if (value instanceof Character) {
				return (short) ((Character) value).charValue();
			}
			else {
				return ((Number) value).shortValue();
			}
		}
	}

	public static double jlrFieldGetDouble(Field field, Object target) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			field = asAccessibleField(field, target, true);
			return field.getDouble(target);
		}
		else {
			asAccessibleField(field, target, false);
			typeCheckFieldGet(field, double.class);
			Object value = rtype.getField(target, field.getName(), Modifier.isStatic(field.getModifiers()));
			if (value instanceof Character) {
				return ((Character) value).charValue();
			}
			else {
				return ((Number) value).doubleValue();
			}
		}
	}

	public static float jlrFieldGetFloat(Field field, Object target) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			field = asAccessibleField(field, target, true);
			return field.getFloat(target);
		}
		else {
			asAccessibleField(field, target, false);
			typeCheckFieldGet(field, float.class);
			Object value = rtype.getField(target, field.getName(), Modifier.isStatic(field.getModifiers()));
			if (value instanceof Character) {
				return ((Character) value).charValue();
			}
			else {
				return ((Number) value).floatValue();
			}
		}
	}

	public static boolean jlrFieldGetBoolean(Field field, Object target) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			field = asAccessibleField(field, target, true);
			return field.getBoolean(target);
		}
		else {
			asAccessibleField(field, target, false);
			typeCheckFieldGet(field, boolean.class);
			Object value = rtype.getField(target, field.getName(), Modifier.isStatic(field.getModifiers()));
			return ((Boolean) value).booleanValue();
		}
	}

	public static long jlrFieldGetLong(Field field, Object target) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			field = asAccessibleField(field, target, true);
			return field.getLong(target);
		}
		else {
			asAccessibleField(field, target, false);
			typeCheckFieldGet(field, long.class);
			Object value = rtype.getField(target, field.getName(), Modifier.isStatic(field.getModifiers()));
			if (value instanceof Character) {
				return ((Character) value).charValue();
			}
			else {
				return ((Number) value).longValue();
			}
		}
	}

	private static void typeCheckFieldGet(Field field, Class<?> returnType) {
		Class<?> fieldType = field.getType();
		if (!Utils.isConvertableFrom(returnType, fieldType)) {
			throw Exceptions.illegalGetFieldType(field, returnType);
		}
	}

	public static void jlrFieldSet(Field field, Object target, Object value) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable
			field = asSetableField(field, target, valueType(value), value, true);
			field.set(target, value);
		}
		else {
			asSetableField(field, target, valueType(value), value, false);
			rtype.setField(target, field.getName(), Modifier.isStatic(field.getModifiers()), value);
		}
	}

	public static void jlrFieldSetInt(Field field, Object target, int value) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable
			field = asSetableField(field, target, int.class, value, true);
			field.setInt(target, value);
		}
		else {
			asSetableField(field, target, int.class, value, false);
			rtype.setField(target, field.getName(), Modifier.isStatic(field.getModifiers()), value);
		}
	}

	public static void jlrFieldSetByte(Field field, Object target, byte value) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable
			field = asSetableField(field, target, byte.class, value, true);
			field.setByte(target, value);
		}
		else {
			asSetableField(field, target, byte.class, value, false);
			rtype.setField(target, field.getName(), Modifier.isStatic(field.getModifiers()), value);
		}
	}

	public static void jlrFieldSetChar(Field field, Object target, char value) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable
			field = asSetableField(field, target, char.class, value, true);
			field.setChar(target, value);
		}
		else {
			asSetableField(field, target, char.class, value, false);
			rtype.setField(target, field.getName(), Modifier.isStatic(field.getModifiers()), value);
		}
	}

	public static void jlrFieldSetShort(Field field, Object target, short value) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable
			field = asSetableField(field, target, short.class, value, true);
			field.setShort(target, value);
		}
		else {
			asSetableField(field, target, short.class, value, false);
			rtype.setField(target, field.getName(), Modifier.isStatic(field.getModifiers()), value);
		}
	}

	public static void jlrFieldSetDouble(Field field, Object target, double value) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable
			field = asSetableField(field, target, double.class, value, true);
			field.setDouble(target, value);
		}
		else {
			asSetableField(field, target, double.class, value, false);
			rtype.setField(target, field.getName(), Modifier.isStatic(field.getModifiers()), value);
		}
	}

	public static void jlrFieldSetFloat(Field field, Object target, float value) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable
			field = asSetableField(field, target, float.class, value, true);
			field.setFloat(target, value);
		}
		else {
			asSetableField(field, target, float.class, value, false);
			rtype.setField(target, field.getName(), Modifier.isStatic(field.getModifiers()), value);
		}
	}

	public static void jlrFieldSetLong(Field field, Object target, long value) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable
			field = asSetableField(field, target, long.class, value, true);
			field.setLong(target, value);
		}
		else {
			asSetableField(field, target, long.class, value, false);
			rtype.setField(target, field.getName(), Modifier.isStatic(field.getModifiers()), value);
		}
	}

	public static void jlrFieldSetBoolean(Field field, Object target, boolean value) throws IllegalAccessException {
		Class<?> clazz = field.getDeclaringClass();
		ReloadableType rtype = getRType(clazz);
		if (rtype == null) {
			// Not reloadable
			field = asSetableField(field, target, boolean.class, value, true);
			field.setBoolean(target, value);
		}
		else {
			asSetableField(field, target, boolean.class, value, false);
			rtype.setField(target, field.getName(), Modifier.isStatic(field.getModifiers()), value);
		}
	}

	/**
	 * What's the "boxed" version of a given primtive type.
	 */
	private static Class<?> boxTypeFor(Class<?> primType) {
		if (primType == int.class) {
			return Integer.class;
		}
		else if (primType == boolean.class) {
			return Boolean.class;
		}
		else if (primType == byte.class) {
			return Byte.class;
		}
		else if (primType == char.class) {
			return Character.class;
		}
		else if (primType == double.class) {
			return Double.class;
		}
		else if (primType == float.class) {
			return Float.class;
		}
		else if (primType == long.class) {
			return Long.class;
		}
		else if (primType == short.class) {
			return Short.class;
		}
		throw new IllegalStateException("Forgotten a case in this method?");
	}

}
