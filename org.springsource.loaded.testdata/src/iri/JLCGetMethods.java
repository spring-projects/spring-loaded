package iri;

import java.lang.reflect.Method;

public class JLCGetMethods extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getMethods");
		Method[] m2 = (Method[]) m.invoke(JLCGetMethods.class);
		//		Method m2 = (Method) m.invoke(JLCGetDecMethod.class, "foo", new Class[] { String.class, Integer.TYPE });
		return format(m2);
	}

	public void foo() {

	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetMethods().run());
	}

}
