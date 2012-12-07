package iri;

import java.lang.reflect.Method;

public class JLCGetDecMethod3 extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredMethod", String.class, Class[].class);
		Method m2 = (Method) m.invoke(JLCGetDecMethod3.class, "bar2", new Class[] { String.class, Integer.TYPE });
		return format(m2);
	}

	public void foo() {

	}

	// this method wasn't in the original type
	public void bar2(String s, int i) {

	}

	public static void main(String[] argv) throws Exception {
		new JLCGetDecMethod3().run();
	}

}
