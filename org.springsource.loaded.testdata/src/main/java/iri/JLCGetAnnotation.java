package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import reflection.AnnoT;

@AnnoT
public class JLCGetAnnotation extends FormattingHelper {

	public JLCGetAnnotation() {

	}

	public String run() throws Exception {
		Method m = Class.class.getMethod("getAnnotation", Class.class);
		Annotation a = (Annotation) m.invoke(JLCGetAnnotation.class, AnnoT.class);
		Annotation b = (Annotation) m.invoke(JLCGetAnnotation.class, Deprecated.class);
		return a + " " + b;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetAnnotation().run());
	}

}
