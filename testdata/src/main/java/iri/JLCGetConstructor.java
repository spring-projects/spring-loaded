package iri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class JLCGetConstructor extends FormattingHelper {

	public JLCGetConstructor() {

	}

	public String run() throws Exception {
		JLCGetConstructor.class.getConstructor();
		Method m = Class.class.getMethod("getConstructor", Class[].class);
		Constructor<?> o = (Constructor<?>) m.invoke(JLCGetConstructor.class, (Object) null);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetConstructor().run());
	}

}
