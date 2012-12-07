package iri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class JLCGetDecConstructor2 extends FormattingHelper {

	public JLCGetDecConstructor2() {

	}

	public JLCGetDecConstructor2(String s) {

	}

	public String run() throws Exception {
		JLCGetDecConstructor2.class.getConstructor();
		Method m = Class.class.getMethod("getDeclaredConstructor", Class[].class);
		Constructor<?> o = (Constructor<?>) m.invoke(JLCGetDecConstructor2.class, (Object) new Class[] { String.class });
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetDecConstructor2().run());
	}

}
