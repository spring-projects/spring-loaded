package reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({ "rawtypes" })
public class ConstructorInvoker {

	///////////////////////////////////////////////////////////////////////////////////
	/// Section below contains an invoker for each method on the Constructor class

	//TODO: [refl] tests calling this
	public static boolean callEquals(Constructor thiz, Object a0) {
		return thiz.equals(a0);
	}

	//TODO: [refl] tests calling this
	public static String callToString(Constructor thiz) {
		return thiz.toString();
	}

	//TODO: [refl] tests calling this
	public static int callHashCode(Constructor thiz) {
		return thiz.hashCode();
	}

	public static int callGetModifiers(Constructor thiz) {
		return thiz.getModifiers();
	}

	public static String callGetName(Constructor thiz) {
		return thiz.getName();
	}

	// See AnnotationsInvoker
	//    public static Annotation callGetAnnotation(Constructor thiz, Class a0)
	//    {
	//        return thiz.getAnnotation(a0);
	//    }
	//
	//    public static Annotation[] callGetDeclaredAnnotations(Constructor thiz)
	//    {
	//        return thiz.getDeclaredAnnotations();
	//    }

	public static Class callGetDeclaringClass(Constructor thiz) {
		return thiz.getDeclaringClass();
	}

	public static Class[] callGetParameterTypes(Constructor thiz) {
		return thiz.getParameterTypes();
	}

	public static TypeVariable[] callGetTypeParameters(Constructor thiz) {
		return thiz.getTypeParameters();
	}

	public static boolean callIsSynthetic(Constructor thiz) {
		return thiz.isSynthetic();
	}

	public static Object callNewInstance(Constructor thiz, Object[] a0) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		return thiz.newInstance(a0);
	}

	public static String callToGenericString(Constructor thiz) {
		return thiz.toGenericString();
	}

	public static Class[] callGetExceptionTypes(Constructor thiz) {
		return thiz.getExceptionTypes();
	}

	public static List<Type> callGetGenericExceptionTypes(Constructor thiz) {
		return Arrays.asList(thiz.getGenericExceptionTypes());
	}

	public static Type[] callGetGenericParameterTypes(Constructor thiz) {
		return thiz.getGenericParameterTypes();
	}

	public static Annotation[][] callGetParameterAnnotations(Constructor thiz) {
		return thiz.getParameterAnnotations();
	}

	public static boolean callIsVarArgs(Constructor thiz) {
		return thiz.isVarArgs();
	}

	///////////////////////////////////////////////////////////////////////////////////
	/// Section below contains 'Ad-Hoc' invokers. Used in testing related 
	/// functionality

	public static String callClassNewInstance(Class<?> clazz) throws InstantiationException, IllegalAccessException {
		return clazz.newInstance().toString();
	}

}
