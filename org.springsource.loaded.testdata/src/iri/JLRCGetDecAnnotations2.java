package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRCGetDecAnnotations2 extends FormattingHelper {

	@AnnoT
	public JLRCGetDecAnnotations2() {
	}

	@Deprecated
	public JLRCGetDecAnnotations2(String s) {

	}

	public String run() throws Exception {
		Method m = Constructor.class.getMethod("getDeclaredAnnotations");
		Constructor<JLRCGetDecAnnotations2> c = JLRCGetDecAnnotations2.class.getDeclaredConstructor(String.class);
		Annotation[] o = (Annotation[]) m.invoke(c);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRCGetDecAnnotations2().run());
	}

}
