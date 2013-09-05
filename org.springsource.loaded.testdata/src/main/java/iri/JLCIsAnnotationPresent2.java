package iri;

import java.lang.reflect.Method;

//@Deprecated
public class JLCIsAnnotationPresent2 extends FormattingHelper {

	public JLCIsAnnotationPresent2() {

	}

	public String run() throws Exception {
		Method m = Class.class.getMethod("isAnnotationPresent", Class.class);
		boolean b = (Boolean) m.invoke(JLCIsAnnotationPresent2.class, Deprecated.class);
		return Boolean.toString(b);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCIsAnnotationPresent2().run());
	}

}
