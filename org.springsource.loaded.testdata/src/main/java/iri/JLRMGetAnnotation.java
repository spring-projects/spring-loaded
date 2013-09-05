package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRMGetAnnotation extends FormattingHelper {

	@AnnoT
	public void string() {

	}

	public String run() throws Exception {
		Method m = Method.class.getMethod("getAnnotation", Class.class);
		Method mm = JLRMGetAnnotation.class.getDeclaredMethod("string");
		Annotation a = (Annotation) m.invoke(mm, AnnoT.class);
		Annotation b = (Annotation) m.invoke(mm, Deprecated.class);
		return a + " " + b;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRMGetAnnotation().run());
	}

}
