package iri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class JLRCNewInstance2 extends FormattingHelper {

	public JLRCNewInstance2() {
	}

	public JLRCNewInstance2(String s) {

	}

	public String run() throws Exception {
		Method m = Constructor.class.getMethod("newInstance", Object[].class);
		Constructor<JLRCNewInstance2> c = JLRCNewInstance2.class.getDeclaredConstructor(String.class);
		JLRCNewInstance2 a = (JLRCNewInstance2) m.invoke(c, new Object[] { new Object[] { "abc" } });
		return a.toString();
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRCNewInstance2().run());
	}

	public String toString() {
		return "instance";
	}
}
