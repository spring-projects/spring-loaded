package iri;

import java.lang.reflect.Method;

public class JLCGetDecMethods extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredMethods");
		Method[] m2 = (Method[]) m.invoke(JLCGetDecMethods.class);
		//		Method m2 = (Method) m.invoke(JLCGetDecMethod.class, "foo", new Class[] { String.class, Integer.TYPE });
		return format(m2);
	}

	public void foo() {

	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetDecMethods().run());
	}

}
