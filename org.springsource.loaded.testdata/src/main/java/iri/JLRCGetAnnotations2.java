package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRCGetAnnotations2 extends FormattingHelper {

	@AnnoT
	public JLRCGetAnnotations2() {
	}

	@Deprecated
	public JLRCGetAnnotations2(String s) {

	}

	public String run() throws Exception {
		Method m = Method.class.getMethod("getAnnotations");
		Constructor<JLRCGetAnnotations2> c = JLRCGetAnnotations2.class.getDeclaredConstructor(String.class);
		Annotation[] o = (Annotation[]) m.invoke(c);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRCGetAnnotations2().run());
	}

}
