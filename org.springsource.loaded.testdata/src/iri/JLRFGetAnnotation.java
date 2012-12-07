package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRFGetAnnotation extends FormattingHelper {

	@AnnoT
	String string;

	public String run() throws Exception {
		Method m = Field.class.getMethod("getAnnotation", Class.class);
		Field f = JLRFGetAnnotation.class.getDeclaredField("string");
		Annotation a = (Annotation) m.invoke(f, AnnoT.class);
		Annotation b = (Annotation) m.invoke(f, Deprecated.class);
		return a + " " + b;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFGetAnnotation().run());
	}

}
