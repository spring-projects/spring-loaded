package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Deprecated
public class JLCGetAnnotations2 extends FormattingHelper {

	public JLCGetAnnotations2() {

	}

	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredAnnotations");
		Annotation[] o = (Annotation[]) m.invoke(JLCGetAnnotations2.class);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetAnnotations2().run());
	}

}
