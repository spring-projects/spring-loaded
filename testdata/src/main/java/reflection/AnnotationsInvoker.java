package reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class that provides method containing test code that calls different reflective methods related to annotations.
 * 
 * @author kdvolder
 */
public class AnnotationsInvoker {

	///////////////////////////////////////////
	// AnnotatedElement
	public List<Annotation> callAnnotatedElementGetAnnotations(AnnotatedElement m) {
		return Arrays.asList(m.getAnnotations());
	}
	public List<Annotation> callAnnotatedElementGetDeclaredAnnotations(AnnotatedElement m) {
		return Arrays.asList(m.getDeclaredAnnotations());
	}
	public Annotation callAnnotatedElementGetAnnotation(AnnotatedElement m, Class<? extends Annotation> annotClass) {
		return m.getAnnotation(annotClass);
	}
	public boolean callAnnotatedElementIsAnnotationPresent(AnnotatedElement m, Class<? extends Annotation> annotClass) {
		return m.isAnnotationPresent(annotClass);
	}
	
	///////////////////////////////////////////
	// AccessibleObject
	public List<Annotation> callAccessibleObjectGetAnnotations(AccessibleObject m) {
		return Arrays.asList(m.getAnnotations());
	}
	public List<Annotation> callAccessibleObjectGetDeclaredAnnotations(AccessibleObject m) {
		return Arrays.asList(m.getDeclaredAnnotations());
	}
	public Annotation callAccessibleObjectGetAnnotation(AccessibleObject m, Class<? extends Annotation> annotClass) {
		return m.getAnnotation(annotClass);
	}
	public boolean callAccessibleObjectIsAnnotationPresent(AccessibleObject m, Class<? extends Annotation> annotClass) {
		return m.isAnnotationPresent(annotClass);
	}
	
	///////////////////////////////////////////
	// Method
	public List<Annotation> callMethodGetAnnotations(Method m) {
		return Arrays.asList(m.getAnnotations());
	}
	public List<Annotation> callMethodGetDeclaredAnnotations(Method m) {
		return Arrays.asList(m.getDeclaredAnnotations());
	}
	public boolean callMethodIsAnnotationPresent(Method m, Class<? extends Annotation> annotClass) {
		return m.isAnnotationPresent(annotClass);
	}
	public Annotation callMethodGetAnnotation(Method m, Class<? extends Annotation> annotClass) {
		return m.getAnnotation(annotClass);
	}
	public List<List<Annotation>> callMethodGetParameterAnnotations(Method m) {
		Annotation[][] array = m.getParameterAnnotations();
		List<List<Annotation>> result = new ArrayList<List<Annotation>>(array.length);
		for (int i = 0; i < array.length; i++) {
			result.add(Arrays.asList(array[i]));
		}
		return result;
	}
	
	///////////////////////////////////////////
	// Constructor
	public List<Annotation> callConstructorGetAnnotations(Constructor<?> m) {
		return Arrays.asList(m.getAnnotations());
	}
	public List<Annotation> callConstructorGetDeclaredAnnotations(Constructor<?> m) {
		return Arrays.asList(m.getDeclaredAnnotations());
	}
	public boolean callConstructorIsAnnotationPresent(Constructor<?> m, Class<? extends Annotation> annotClass) {
		return m.isAnnotationPresent(annotClass);
	}
	public Annotation callConstructorGetAnnotation(Constructor<?> m, Class<? extends Annotation> annotClass) {
		return m.getAnnotation(annotClass);
	}
	public List<List<Annotation>> callConstructorGetParameterAnnotations(Constructor<?> m) {
		Annotation[][] array = m.getParameterAnnotations();
		List<List<Annotation>> result = new ArrayList<List<Annotation>>(array.length);
		for (int i = 0; i < array.length; i++) {
			result.add(Arrays.asList(array[i]));
		}
		return result;
	}
	
	///////////////////////////////////////////
	// Field
	public List<Annotation> callFieldGetAnnotations(Field m) {
		return Arrays.asList(m.getAnnotations());
	}
	public List<Annotation> callFieldGetDeclaredAnnotations(Field m) {
		return Arrays.asList(m.getDeclaredAnnotations());
	}
	public boolean callFieldIsAnnotationPresent(Field m, Class<? extends Annotation> annotClass) {
		return m.isAnnotationPresent(annotClass);
	}
	public Annotation callFieldGetAnnotation(Field m, Class<? extends Annotation> annotClass) {
		return m.getAnnotation(annotClass);
	}

	
	///////////////////////////////////////////
	// Class
	public List<Annotation> callClassGetAnnotations(Class<?> c) {
		return Arrays.asList(c.getAnnotations());
	}
	public List<Annotation> callClassGetDeclaredAnnotations(Class<?> c) {
		return Arrays.asList(c.getDeclaredAnnotations());
	}
	public boolean callClassIsAnnotationPresent(Class<?> c, Class<? extends Annotation> annotClass) {
		return c.isAnnotationPresent(annotClass);
	}
	public Annotation callClassGetAnnotation(Class<?> c, Class<? extends Annotation> annotClass) {
		return c.getAnnotation(annotClass);
	}

}
