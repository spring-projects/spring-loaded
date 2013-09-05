package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRMGetAnnotationViaAccessibleObject2 extends FormattingHelper {

	@AnnoT
	public void string() {

	}

	@Deprecated
	public void newmethod() {

	}

	public String run() throws Exception {
		Method m = AccessibleObject.class.getMethod("getAnnotation", Class.class);
		Method mm = JLRMGetAnnotationViaAccessibleObject2.class.getDeclaredMethod("newmethod");
		Annotation a = (Annotation) m.invoke(mm, AnnoT.class);
		Annotation b = (Annotation) m.invoke(mm, Deprecated.class);
		return a + " " + b;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRMGetAnnotationViaAccessibleObject2().run());
	}

}
