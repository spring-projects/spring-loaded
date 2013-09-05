package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRCGetAnnotation2 extends FormattingHelper {

	@AnnoT
	public JLRCGetAnnotation2() {

	}

	@Deprecated
	public JLRCGetAnnotation2(String s) {

	}

	public String run() throws Exception {
		Method m = Constructor.class.getMethod("getAnnotation", Class.class);
		Constructor<JLRCGetAnnotation2> c = JLRCGetAnnotation2.class.getDeclaredConstructor(String.class);
		Annotation a = ((Annotation) m.invoke(c, AnnoT.class));
		Annotation b = ((Annotation) m.invoke(c, Deprecated.class));
		return a + " " + b;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRCGetAnnotation2().run());
	}

}
