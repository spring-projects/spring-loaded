package reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import reflection.targets.ClassTarget;

/**
 * A rather 'add-hoc' created ClassInvoker containing various bits and pieces of reflective code that calls (mostly) methods in the
 * class API.
 * <p>
 * This is renamed from 'ClassInvoker' which has since been replaced with a more systematic 'generated' Invoker class.
 * 
 * @author kdvolder
 */
public class AdHocClassInvoker {

	static ClassTarget t = new ClassTarget();

	public Field callGetDeclaredField(Class<?> clazz, String fieldName) throws Exception {
		return clazz.getDeclaredField(fieldName);
	}

	public Field callGetDeclaredFieldForThisName(Class<?> clazz, String name) throws Exception {
		Field f = clazz.getDeclaredField(name);
		return f;
	}

	public List<Field> callClassGetDeclaredFields(Class<?> clazz) throws Exception {
		return Arrays.asList(clazz.getDeclaredFields());
	}

	public List<Field> callClassGetFields(Class<?> clazz) throws Exception {
		return Arrays.asList(clazz.getFields());
	}

	public int callClassGetModifiers(Class<?> clazz) throws Exception {
		System.out.println(clazz);
		System.out.println(clazz.getModifiers());
		return clazz.getModifiers();
	}

	public Field callClassGetField(Class<?> clazz, String name) throws Exception {
		return clazz.getField(name);
	}

	public Field callClassGetDeclaredField(Class<?> clazz, String name) throws Exception {
		return clazz.getDeclaredField(name);
	}

	public Method callGetDeclaredMethod(Class<?> clazz, String name, Class<?>... params) throws Exception {
		return clazz.getDeclaredMethod(name, params);
	}

	public Method callGetDeclaredMethodForThisName(Class<?> clazz, String name, Class<?>... paramTypes) throws Exception {
		Method m = clazz.getDeclaredMethod(name, paramTypes);
		return m;
	}

	public List<Method> callGetDeclaredMethods(Class<?> clazz) throws Exception {
		return Arrays.asList(clazz.getDeclaredMethods());
	}

	public List<Method> callGetMethods(Class<?> clazz) throws Exception {
		return Arrays.asList(clazz.getMethods());
	}

	public Method callGetMethod(Class<?> clazz, String name, Class<?>... params) throws SecurityException, NoSuchMethodException {
		return clazz.getMethod(name, params);
	}

	public Constructor<?> callGetDeclaredConstructor(Class<?> clazz, Class<?>... params) throws SecurityException,
			NoSuchMethodException {
		return clazz.getDeclaredConstructor(params);
	}

	public Constructor<?>[] callGetDeclaredConstructors(Class<?> clazz) throws SecurityException, NoSuchMethodException {
		return clazz.getDeclaredConstructors();
	}

	public <T> T callNewInstance(Constructor<T> ctor, Object... params) throws IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		return ctor.newInstance(params);
	}

	/**
	 * Calls private method in reloadable class, can override access constraints if requested to do so.
	 */
	public String callMethodWithAccess(String whichMethod, boolean setAccess) throws Exception {
		Method theMethod = ClassTarget.class.getDeclaredMethod(whichMethod);
		if (setAccess) {
			theMethod.setAccessible(true);
		}
		return (String) theMethod.invoke(t);
	}

	public Object getFieldValue(Field f) throws Exception {
		return f.get(t);
	}

	public Object runThisMethod(Method m) throws Exception {
		return m.invoke(t);
	}

	public Object runThisMethodOn(Object t, Method m, Object... args) throws Exception {
		return m.invoke(t, args);
	}

	public Object runThisMethodWithParam(Method m, Object[] params) throws Exception {
		return m.invoke(t, params);
	}

	public void setFieldValue(Field f, Object newValue) throws Exception {
		f.set(t, newValue);
	}

	public boolean callMethodIsSynthetic(Method m) {
		return m.isSynthetic();
	}

	public boolean callMethodIsBridge(Method m) {
		return m.isBridge();
	}

}
