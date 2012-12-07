package data;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class DemoMethods2 {

	public static void main(String[] args) throws Exception {
		getDeclaredMethod("foo");
		getDeclaredMethod("foo");
		getDeclaredMethod("foo");
		getDeclaredMethod("foo");
		System.out.println(getFirstDeclaredAnnotation("foo"));
		System.out.println(getFirstDeclaredAnnotation("foo"));
		System.out.println(getFirstDeclaredAnnotation("foo"));
		System.out.println(getFirstDeclaredAnnotation("foo"));
		System.out.println(getFirstDeclaredAnnotation("foo"));
	}

	@Wiggle("asc")
	public void foo() {

	}

	public static Method getDeclaredMethod(String name) {
		try {
			Method m = DemoMethods2.class.getDeclaredMethod(name);
			System.out.println("returning " + m);
			return m;
		} catch (NoSuchMethodException nsme) {
			System.out.println("No such method called foo");
			return null;
		}
	}

	public static Annotation getFirstDeclaredAnnotation(String name) throws Exception {
		Annotation[] annos = getDeclaredMethod(name).getDeclaredAnnotations();
		if (annos == null || annos.length == 0) {
			return null;
		} else {
			return annos[0];
		}
	}

}