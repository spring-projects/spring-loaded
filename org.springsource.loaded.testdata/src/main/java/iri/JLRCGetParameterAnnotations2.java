package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import reflection.AnnoT;
import reflection.AnnoT2;

public class JLRCGetParameterAnnotations2 extends FormattingHelper {

	public JLRCGetParameterAnnotations2() {

	}

	public JLRCGetParameterAnnotations2(@AnnoT String s, int i, @AnnoT2 int j) {
	}

	public JLRCGetParameterAnnotations2(@AnnoT2 int s, @AnnoT float i, int j) {
	}

	public String run() throws Exception {
		Method m = Constructor.class.getMethod("getParameterAnnotations");
		Constructor<JLRCGetParameterAnnotations2> c = JLRCGetParameterAnnotations2.class.getDeclaredConstructor(Integer.TYPE,
				Float.TYPE, Integer.TYPE);
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
		System.out.println(new JLRCGetParameterAnnotations2("a", 1, 2).run());
	}
}
