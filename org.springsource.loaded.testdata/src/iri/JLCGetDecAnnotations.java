package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Deprecated
public class JLCGetDecAnnotations extends FormattingHelper {

	public JLCGetDecAnnotations() {

	}

	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredAnnotations");
		Annotation[] o = (Annotation[]) m.invoke(JLCGetDecAnnotations.class);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetDecAnnotations().run());
	}

}
