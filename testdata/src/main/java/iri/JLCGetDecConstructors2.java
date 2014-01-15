package iri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class JLCGetDecConstructors2 extends FormattingHelper {

	public JLCGetDecConstructors2() {

	}

	// not in original
	public JLCGetDecConstructors2(String s) {

	}

	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredConstructors");
		Constructor[] o = (Constructor[]) m.invoke(JLCGetDecConstructors2.class);
		//		Method m2 = (Method) m.invoke(JLCGetDecMethod.class, "foo", new Class[] { String.class, Integer.TYPE });
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetDecConstructors2().run());
	}

}
