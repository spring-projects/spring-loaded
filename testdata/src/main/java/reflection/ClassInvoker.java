package reflection;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

/**
 * Class containing one method for each method in the java.lang.Class
 * containing code calling that method.
 * <p>
 * Initial version generated with {@link InvokerGenerator} but afterwards
 * edited to:
 * 
 *  - wrap returned arrays into lists for easier testing and nicer toStrings
 * 
 */
@SuppressWarnings({"unchecked","rawtypes"})public class ClassInvoker{
	
    public static Class callAsSubclass(Class thiz, Class a0)
    {
        return thiz.asSubclass(a0);
    }

    public static Object callCast(Class thiz, Object a0)
    {
        return thiz.cast(a0);
    }
	
    public static boolean callDesiredAssertionStatus(Class thiz)
    {
        return thiz.desiredAssertionStatus();
    }

    public static Class callForName(String a0)
    throws ClassNotFoundException
    {
        return Class.forName(a0);
    }

    public static Class callForName(String a0, boolean a1, ClassLoader a2)
    throws ClassNotFoundException
    {
        return Class.forName(a0, a1, a2);
    }
    
    public static Annotation callGetAnnotation(Class thiz, Class a0) {
        return thiz.getAnnotation(a0);
    }

    public static Annotation[] callGetAnnotations(Class thiz)
    {
        return thiz.getAnnotations();
    }

    public static String callGetCanonicalName(Class thiz)
    {
        return thiz.getCanonicalName();
    }

    public static Class[] callGetClasses(Class thiz)
    {
        return thiz.getClasses();
    }

    public static ClassLoader callGetClassLoader(Class thiz)
    {
        return thiz.getClassLoader();
    }

    public static Class callGetComponentType(Class thiz)
    {
        return thiz.getComponentType();
    }

    public static Constructor callGetConstructor(Class thiz, Class[] a0)
    throws NoSuchMethodException, SecurityException
    {
        return thiz.getConstructor(a0);
    }

    public static List<Constructor> callGetConstructors(Class thiz)
    throws SecurityException
    {
        return Arrays.asList(thiz.getConstructors());
    }

    public static Annotation[] callGetDeclaredAnnotations(Class thiz)
    {
        return thiz.getDeclaredAnnotations();
    }

    public static Class[] callGetDeclaredClasses(Class thiz)
    throws SecurityException
    {
        return thiz.getDeclaredClasses();
    }

    public static Constructor callGetDeclaredConstructor(Class thiz, Class[] a0)
    throws NoSuchMethodException, SecurityException
    {
        return thiz.getDeclaredConstructor(a0);
    }

    public static List<Constructor> callGetDeclaredConstructors(Class thiz)
    throws SecurityException
    {
        return Arrays.asList(thiz.getDeclaredConstructors());
    }

    public static Field callGetDeclaredField(Class thiz, String a0)
    throws NoSuchFieldException, SecurityException
    {
        return thiz.getDeclaredField(a0);
    }

    public static List<Field> callGetDeclaredFields(Class thiz)
    throws SecurityException
    {
        return Arrays.asList(thiz.getDeclaredFields());
    }

    public static Method callGetDeclaredMethod(Class thiz, String a0, Class[] a1)
    throws NoSuchMethodException, SecurityException
    {
        return thiz.getDeclaredMethod(a0, a1);
    }

    public static List<Method> callGetDeclaredMethods(Class thiz)
    throws SecurityException
    {
        return Arrays.asList(thiz.getDeclaredMethods());
    }

    public static Class callGetDeclaringClass(Class thiz)
    {
        return thiz.getDeclaringClass();
    }

    public static Class callGetEnclosingClass(Class thiz)
    {
        return thiz.getEnclosingClass();
    }

    public static Constructor callGetEnclosingConstructor(Class thiz)
    {
        return thiz.getEnclosingConstructor();
    }

    public static Method callGetEnclosingMethod(Class thiz)
    {
        return thiz.getEnclosingMethod();
    }

    public static Object[] callGetEnumConstants(Class thiz)
    {
        return thiz.getEnumConstants();
    }

    public static Field callGetField(Class thiz, String a0)
    throws NoSuchFieldException, SecurityException
    {
        return thiz.getField(a0);
    }

    public static List<Field> callGetFields(Class thiz)
    throws SecurityException
    {
        return Arrays.asList(thiz.getFields());
    }

    public static Type[] callGetGenericInterfaces(Class thiz)
    {
        return thiz.getGenericInterfaces();
    }

    public static Type callGetGenericSuperclass(Class thiz)
    {
        return thiz.getGenericSuperclass();
    }

    public static Class[] callGetInterfaces(Class thiz)
    {
        return thiz.getInterfaces();
    }

    public static Method callGetMethod(Class thiz, String a0, Class[] a1)
    throws NoSuchMethodException, SecurityException
    {
        return thiz.getMethod(a0, a1);
    }

    public static List<Method> callGetMethods(Class thiz)
    throws SecurityException
    {
        return Arrays.asList(thiz.getMethods());
    }

    public static int callGetModifiers(Class thiz)
    {
        return thiz.getModifiers();
    }

    public static String callGetName(Class thiz)
    {
        return thiz.getName();
    }

    public static Package callGetPackage(Class thiz)
    {
        return thiz.getPackage();
    }

    public static ProtectionDomain callGetProtectionDomain(Class thiz)
    {
        return thiz.getProtectionDomain();
    }

    public static URL callGetResource(Class thiz, String a0)
    {
        return thiz.getResource(a0);
    }

    public static InputStream callGetResourceAsStream(Class thiz, String a0)
    {
        return thiz.getResourceAsStream(a0);
    }

    public static Object[] callGetSigners(Class thiz)
    {
        return thiz.getSigners();
    }

    public static String callGetSimpleName(Class thiz)
    {
        return thiz.getSimpleName();
    }

    public static Class callGetSuperclass(Class thiz)
    {
        return thiz.getSuperclass();
    }

    public static TypeVariable[] callGetTypeParameters(Class thiz)
    {
        return thiz.getTypeParameters();
    }

    public static boolean callIsAnnotation(Class thiz)
    {
        return thiz.isAnnotation();
    }

    public static boolean callIsAnnotationPresent(Class thiz, Class a0)
    {
        return thiz.isAnnotationPresent(a0);
    }

    public static boolean callIsAnonymousClass(Class thiz)
    {
        return thiz.isAnonymousClass();
    }

    public static boolean callIsArray(Class thiz)
    {
        return thiz.isArray();
    }

    public static boolean callIsAssignableFrom(Class thiz, Class a0)
    {
        return thiz.isAssignableFrom(a0);
    }

    public static boolean callIsEnum(Class thiz)
    {
        return thiz.isEnum();
    }

    public static boolean callIsInstance(Class thiz, Object a0)
    {
        return thiz.isInstance(a0);
    }

    public static boolean callIsInterface(Class thiz)
    {
        return thiz.isInterface();
    }

    public static boolean callIsLocalClass(Class thiz)
    {
        return thiz.isLocalClass();
    }

    public static boolean callIsMemberClass(Class thiz)
    {
        return thiz.isMemberClass();
    }

    public static boolean callIsPrimitive(Class thiz)
    {
        return thiz.isPrimitive();
    }

    public static boolean callIsSynthetic(Class thiz)
    {
        return thiz.isSynthetic();
    }

    public static Object callNewInstance(Class thiz)
    throws InstantiationException, IllegalAccessException
    {
        return thiz.newInstance();
    }

    public static String callToString(Class thiz)
    {
        return thiz.toString();
    }

}

