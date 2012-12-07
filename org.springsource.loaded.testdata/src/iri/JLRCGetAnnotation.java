package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRCGetAnnotation extends FormattingHelper {

	@AnnoT
	public JLRCGetAnnotation() {

	}

	public String run() throws Exception {
		Method m = Constructor.class.getMethod("getAnnotation", Class.class);
		Constructor<JLRCGetAnnotation> c = JLRCGetAnnotation.class.getDeclaredConstructor();
		Annotation a = ((Annotation) m.invoke(c, AnnoT.class));
		Annotation b = ((Annotation) m.invoke(c, Deprecated.class));
		return a + " " + b;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRCGetAnnotation().run());
	}

}
