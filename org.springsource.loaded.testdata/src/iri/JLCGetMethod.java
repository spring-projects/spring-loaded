package iri;

import java.lang.reflect.Method;

public class JLCGetMethod extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getMethod", String.class, Class[].class);
		Method m2 = (Method) m.invoke(JLCGetMethod.class, "foo", null);
		//		Method m2 = (Method) m.invoke(JLCGetDecMethod.class, "foo", new Class[] { String.class, Integer.TYPE });
		return format(m2);
	}

	public void foo() {

		//	public void foo(String s, int i) {
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetMethod().run());
	}

}
