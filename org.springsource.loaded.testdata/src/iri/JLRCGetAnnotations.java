package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRCGetAnnotations extends FormattingHelper {

	@AnnoT
	public JLRCGetAnnotations() {
	}

	public String run() throws Exception {
		Method m = Method.class.getMethod("getAnnotations");
		Constructor<JLRCGetAnnotations> c = JLRCGetAnnotations.class.getDeclaredConstructor();
		Annotation[] o = (Annotation[]) m.invoke(c);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRCGetAnnotations().run());
	}

}
