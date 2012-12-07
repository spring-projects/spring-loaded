package iri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class JLCGetConstructor2 extends FormattingHelper {

	public JLCGetConstructor2() {

	}

	public JLCGetConstructor2(String s) {

	}

	public String run() throws Exception {
		JLCGetConstructor2.class.getConstructor();
		Method m = Class.class.getMethod("getConstructor", Class[].class);
		Constructor<?> o = (Constructor<?>) m.invoke(JLCGetConstructor2.class, (Object) new Class[] { String.class });
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetConstructor2().run());
	}

}
