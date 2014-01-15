package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import reflection.AnnoT;

@Deprecated
public class JLCGetAnnotation2 extends FormattingHelper {

	public JLCGetAnnotation2() {

	}

	public String run() throws Exception {
		Method m = Class.class.getMethod("getAnnotation", Class.class);
		Annotation a = (Annotation) m.invoke(JLCGetAnnotation2.class, AnnoT.class);
		Annotation b = (Annotation) m.invoke(JLCGetAnnotation2.class, Deprecated.class);
		return a + " " + b;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetAnnotation2().run());
	}

}
