package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import reflection.AnnoT;

@AnnoT
public class JLCGetDecAnnotations2 extends FormattingHelper {

	public JLCGetDecAnnotations2() {

	}

	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredAnnotations");
		Annotation[] o = (Annotation[]) m.invoke(JLCGetDecAnnotations2.class);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetDecAnnotations2().run());
	}

}
