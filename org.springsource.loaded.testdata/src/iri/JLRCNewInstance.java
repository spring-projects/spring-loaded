package iri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class JLRCNewInstance extends FormattingHelper {

	public JLRCNewInstance() {
	}

	public String run() throws Exception {
		Method m = Constructor.class.getMethod("newInstance", Object[].class);
		Constructor<JLRCNewInstance> c = JLRCNewInstance.class.getDeclaredConstructor();
		JLRCNewInstance a = (JLRCNewInstance) m.invoke(c, new Object[] { (Object[]) null });
		return a.toString();
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRCNewInstance().run());
	}

	public String toString() {
		return "instance";
	}
}
