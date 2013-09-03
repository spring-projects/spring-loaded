package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRMGetAnnotationViaAnnotatedElement extends FormattingHelper {

	@AnnoT
	public void string() {

	}

	public String run() throws Exception {
		Method m = AnnotatedElement.class.getMethod("getAnnotation", Class.class);
		Method mm = JLRMGetAnnotationViaAnnotatedElement.class.getDeclaredMethod("string");
		Annotation a = (Annotation) m.invoke(mm, AnnoT.class);
		Annotation b = (Annotation) m.invoke(mm, Deprecated.class);
		return a + " " + b;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRMGetAnnotationViaAnnotatedElement().run());
	}

}
