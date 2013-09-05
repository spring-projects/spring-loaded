package reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;

/**
 * Class containing one method for each method in the java.lang.reflect.Method containing code calling that method.
 */
@SuppressWarnings({ "unchecked" })
public class MethodInvoker {

	public static Object callInvoke(Method thiz, Object a0, Object[] a1) throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		return thiz.invoke(a0, a1);
	}

	public static boolean callEquals(Method thiz, Object a0) {
		return thiz.equals(a0);
	}

	public static String callToString(Method thiz) {
		return thiz.toString();
	}

	public static int callHashCode(Method thiz) {
		return thiz.hashCode();
	}

	public static int callGetModifiers(Method thiz) {
		return thiz.getModifiers();
	}

	public static String callGetName(Method thiz) {
		return thiz.getName();
	}

	public static Annotation callGetAnnotation(Method thiz, Class<? extends Annotation> a0) {
		return thiz.getAnnotation(a0);
	}

	public static Annotation[] callGetDeclaredAnnotations(Method thiz) {
		return thiz.getDeclaredAnnotations();
	}

	public static Class<?> callGetDeclaringClass(Method thiz) {
		return thiz.getDeclaringClass();
	}

	public static Class<?>[] callGetParameterTypes(Method thiz) {
		return thiz.getParameterTypes();
	}

	public static Class<?> callGetReturnType(Method thiz) {
		return thiz.getReturnType();
	}

	public static List<TypeVariable<Method>> callGetTypeParameters(Method thiz) {
		return Arrays.asList(thiz.getTypeParameters());
	}

	public static boolean callIsSynthetic(Method thiz) {
		return thiz.isSynthetic();
	}

	public static String callToGenericString(Method thiz) {
		return thiz.toGenericString();
	}

	public static Object callGetDefaultValue(Method thiz) {
		return thiz.getDefaultValue();
	}

	public static List<Class<?>> callGetExceptionTypes(Method thiz) {
		return Arrays.asList(thiz.getExceptionTypes());
	}

	public static List<Type> callGetGenericExceptionTypes(Method thiz) {
		return Arrays.asList(thiz.getGenericExceptionTypes());
	}

	public static List<Type> callGetGenericParameterTypes(Method thiz) {
		return Arrays.asList(thiz.getGenericParameterTypes());
	}

	public static Type callGetGenericReturnType(Method thiz) {
		return thiz.getGenericReturnType();
	}

	public static Annotation[][] callGetParameterAnnotations(Method thiz) {
		return thiz.getParameterAnnotations();
	}

	public static boolean callIsBridge(Method thiz) {
		return thiz.isBridge();
	}

	public static boolean callIsVarArgs(Method thiz) {
		return thiz.isVarArgs();
	}

}
