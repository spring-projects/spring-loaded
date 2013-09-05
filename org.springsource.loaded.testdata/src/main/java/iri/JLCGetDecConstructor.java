package iri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class JLCGetDecConstructor extends FormattingHelper {

	public JLCGetDecConstructor() {

	}

	public String run() throws Exception {
		JLCGetDecConstructor.class.getConstructor();
		Method m = Class.class.getMethod("getDeclaredConstructor", Class[].class);
		Constructor<?> o = (Constructor<?>) m.invoke(JLCGetDecConstructor.class, (Object) null);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetDecConstructor().run());
	}

}
