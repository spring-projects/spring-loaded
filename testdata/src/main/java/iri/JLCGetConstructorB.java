package iri;

import java.lang.reflect.Constructor;

public class JLCGetConstructorB extends FormattingHelper {

	public JLCGetConstructorB() {

	}

	public String run() throws Exception {
		Constructor<JLCGetConstructorB> o = JLCGetConstructorB.class.getConstructor();
		//		Method m = Class.class.getMethod("getConstructor", Class[].class);
		//		Constructor<?> o = (Constructor<?>) m.invoke(JLCGetConstructorB.class, (Object) null);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetConstructorB().run());
	}

}
