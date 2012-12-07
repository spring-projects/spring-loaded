package iri;

import java.lang.reflect.Method;

public class JLCGetMethod2 extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getMethod", String.class, Class[].class);
		Method m2 = (Method) m.invoke(JLCGetMethod2.class, "bar", null);
		return format(m2);
	}

	public void foo() {

	}

	// this method wasn't in the original type
	public void bar() {

	}

	public static void main(String[] argv) throws Exception {
		new JLCGetMethod2().run();
	}

}
