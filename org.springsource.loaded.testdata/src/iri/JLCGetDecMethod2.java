package iri;

import java.lang.reflect.Method;

public class JLCGetDecMethod2 extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredMethod", String.class, Class[].class);
		Method m2 = (Method) m.invoke(JLCGetDecMethod2.class, "bar", null);
		return format(m2);
	}

	public void foo() {

	}

	// this method wasn't in the original type
	public void bar() {

	}

	public static void main(String[] argv) throws Exception {
		new JLCGetDecMethod2().run();
	}

}
