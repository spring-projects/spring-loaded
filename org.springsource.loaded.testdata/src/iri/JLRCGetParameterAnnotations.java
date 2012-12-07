package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import reflection.AnnoT;
import reflection.AnnoT2;

public class JLRCGetParameterAnnotations extends FormattingHelper {

	public JLRCGetParameterAnnotations() {

	}

	public JLRCGetParameterAnnotations(@AnnoT String s, int i, @AnnoT2 int j) {
	}

	public String run() throws Exception {
		Method m = Constructor.class.getMethod("getParameterAnnotations");
		Constructor<JLRCGetParameterAnnotations> c = JLRCGetParameterAnnotations.class.getDeclaredConstructor(String.class,
				Integer.TYPE, Integer.TYPE);
		Annotation[][] arrayofannos = (Annotation[][]) m.invoke(c);
		StringBuilder s = new StringBuilder();
		for (Annotation[] annos : arrayofannos) {
			s.append("[");
			s.append(format(annos));
			s.append("]");
		}
		return s.toString().trim();
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRCGetParameterAnnotations("a", 1, 2).run());
	}
}
