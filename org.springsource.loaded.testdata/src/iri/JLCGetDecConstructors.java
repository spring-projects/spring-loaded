package iri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class JLCGetDecConstructors extends FormattingHelper {

	public JLCGetDecConstructors() {

	}

	@SuppressWarnings("rawtypes")
	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredConstructors");
		Constructor[] o = (Constructor[]) m.invoke(JLCGetDecConstructors.class);
		//		Method m2 = (Method) m.invoke(JLCGetDecMethod.class, "foo", new Class[] { String.class, Integer.TYPE });
		return format(o);
	}

	public void foo() {

		//	public void foo(String s, int i) {
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetDecConstructors().run());
	}

}
