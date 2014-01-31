package reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class FieldInvoker {

	public static boolean callEquals(Field thiz, Object a0) {
		return thiz.equals(a0);
	}

	public static String callToString(Field thiz) {
		return thiz.toString();
	}

	public static int callHashCode(Field thiz) {
		return thiz.hashCode();
	}

	public static int callGetModifiers(Field thiz) {
		return thiz.getModifiers();
	}

	public static String callToGenericString(Field thiz) {
		return thiz.toGenericString();
	}

	public static Object callGet(Field thiz, Object o) throws IllegalArgumentException, IllegalAccessException {
		return thiz.get(o);
	}

	public static long callSetAndGetLong(Field thiz, Object o) throws IllegalArgumentException, IllegalAccessException {
		thiz.setLong(o, thiz.getLong(o));
		return thiz.getLong(o);
	}

	public static short callSetAndGetShort(Field thiz, Object o) throws IllegalArgumentException, IllegalAccessException {
		thiz.setShort(o, (short) (thiz.getShort(o) + 1));
		return thiz.getShort(o);
	}

	public static boolean callSetAndGetBoolean(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setBoolean(obj, !thiz.getBoolean(obj));
		return thiz.getBoolean(obj);
	}

	public static byte callSetAndGetByte(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setByte(obj, (byte) (thiz.getByte(obj) + 1));
		return thiz.getByte(obj);
	}

	public static char callSetAndGetChar(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setChar(obj, (char) (thiz.getChar(obj) + 1));
		return thiz.getChar(obj);
	}

	public static int callSetAndGetInt(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setInt(obj, thiz.getInt(obj) + 1);
		return thiz.getInt(obj);
	}

	public static float callSetAndGetFloat(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setFloat(obj, (float) (thiz.getFloat(obj) + 1.5));
		return thiz.getFloat(obj);
	}

	public static double callSetAndGetDouble(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setDouble(obj, thiz.getDouble(obj) + 1.5);
		return thiz.getDouble(obj);
	}

	public static long callSetLong(Field thiz, Object o) throws IllegalArgumentException, IllegalAccessException {
		thiz.setLong(o, 12345);
		return thiz.getLong(o);
	}

	public static short callSetShort(Field thiz, Object o) throws IllegalArgumentException, IllegalAccessException {
		thiz.setShort(o, (short) 1234);
		return thiz.getShort(o);
	}

	public static boolean callSetBoolean(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setBoolean(obj, true);
		return thiz.getBoolean(obj);
	}

	public static byte callSetByte(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setByte(obj, (byte) 123);
		return thiz.getByte(obj);
	}

	public static char callSetChar(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setChar(obj, 'Y');
		return thiz.getChar(obj);
	}

	public static int callSetInt(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setInt(obj, 1234);
		return thiz.getInt(obj);
	}

	public static float callSetFloat(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setFloat(obj, (float) 1.234);
		return thiz.getFloat(obj);
	}

	public static double callSetDouble(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.setDouble(obj, 1.234);
		return thiz.getDouble(obj);
	}

	public static String callGetName(Field thiz) {
		return thiz.getName();
	}

	public static Annotation callGetAnnotation(Field thiz, Class<? extends Annotation> a0) {
		return thiz.getAnnotation(a0);
	}

	public static Annotation[] callGetDeclaredAnnotations(Field thiz) {
		return thiz.getDeclaredAnnotations();
	}

	public static Class<?> callGetDeclaringClass(Field thiz) {
		return thiz.getDeclaringClass();
	}

	public static boolean callIsSynthetic(Field thiz) {
		return thiz.isSynthetic();
	}

	public static Type callGetGenericType(Field thiz) {
		return thiz.getGenericType();
	}

	public static Class<?> callGetType(Field thiz) {
		return thiz.getType();
	}

	public static boolean callIsEnumConstant(Field thiz) {
		return thiz.isEnumConstant();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/// Methods below don't correspond directly to a single method in the Field type, but do a bunch of things at
	/// once.

	/**
	 * Gets a field in class, can override access constraints if requested to do so.
	 */
	public String getFieldWithAccess(Class<?> targetClass, String whichField, boolean setAccess) throws Exception {
		Object targetInstance = targetClass.newInstance();
		Field field = targetClass.getDeclaredField(whichField);
		if (setAccess) {
			field.setAccessible(true);
		}
		return (String) field.get(targetInstance);
	}

	/**
	 * Sets a field in class, can override access constraints if requested to do so.
	 */
	public void setFieldWithAccess(Class<?> targetClass, String whichField, boolean setAccess) throws Exception {
		Object targetInstance = targetClass.newInstance();
		Field field = targetClass.getDeclaredField(whichField);
		if (setAccess) {
			field.setAccessible(true);
		}
		// Not checking for type errors in this test, make sure we set correct type of value
		if (field.getType().equals(int.class)) {
			field.set(targetInstance, 888);
		} else {
			field.set(targetInstance, "<BANG>");
		}
	}

	/**
	 * Sets and gets a field in so we can see if the value was actually set.
	 */
	public String setAndGetFieldWithAccess(Class<?> targetClass, String whichField, boolean setAccess) throws Exception {
		Object targetInstance = targetClass.newInstance();
		Field field = targetClass.getDeclaredField(whichField);
		if (setAccess) {
			field.setAccessible(true);
		}
		String orgVal = (String) field.get(targetInstance);
		field.set(targetInstance, orgVal + "<BANG>");
		return (String) field.get(targetInstance);
	}

	public static Object callSetAndGet(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.set(obj, thiz.get(obj) + "<BANG>");
		return thiz.get(obj);
	}

	public static Object callSetNull(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		thiz.set(obj, null);
		return thiz.get(obj);
	}

	public static Object callSetUnboxAndGet(Field thiz, Object obj) throws IllegalArgumentException, IllegalAccessException {
		Object val = thiz.get(obj);
		if (val instanceof Integer) {
			thiz.set(obj, ((Integer) val) + 1);
		} else if (val instanceof Boolean) {
			thiz.set(obj, !((Boolean) val));
		} else if (val instanceof Float) {
			thiz.set(obj, new Float(((Float) val) + 1.5));
		} else if (val instanceof Double) {
			thiz.set(obj, new Double(((Double) val) + 1.5));
		} else if (val instanceof SubTestVal) {
			//Try to put a value of a super type instead
			thiz.set(obj, TestVal.it);
		} else if (val instanceof TestVal) {
			//Try to put a value of a sub type instead
			thiz.set(obj, SubTestVal.it);
		}
		// Could add other primitive type cases but this is probably ok
		return thiz.get(obj);
	}

}