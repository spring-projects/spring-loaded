package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import reflection.AnnoT;
import reflection.AnnoT2;

public class JLRMGetParameterAnnotations2 extends FormattingHelper {

	public void string(@AnnoT String s, int i, @AnnoT2 int j) {
	}

	public void newmethod(@AnnoT2 String s, @AnnoT int i, int j) {
	}

	public String run() throws Exception {
		Method m = Method.class.getMethod("getParameterAnnotations");
		Method mm = JLRMGetParameterAnnotations2.class.getDeclaredMethod("newmethod", String.class, Integer.TYPE, Integer.TYPE);
		Annotation[][] arrayofannos = (Annotation[][]) m.invoke(mm);
		StringBuilder s = new StringBuilder();
		for (Annotation[] annos : arrayofannos) {
			s.append("[");
			s.append(format(annos));
			s.append("]");
		}
		return s.toString().trim();
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRMGetParameterAnnotations2().run());
	}

}
