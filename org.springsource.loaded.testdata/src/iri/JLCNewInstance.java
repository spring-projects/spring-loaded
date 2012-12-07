package iri;

import java.lang.reflect.Method;

public class JLCNewInstance extends FormattingHelper {

	public JLCNewInstance() {

	}

	public String run() throws Exception {
		Method m = Class.class.getMethod("newInstance");
		Object o = m.invoke(JLCNewInstance.class);
		return o.toString();
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCNewInstance().run());
	}

	public String toString() {
		return "I am an instance";
	}

}
