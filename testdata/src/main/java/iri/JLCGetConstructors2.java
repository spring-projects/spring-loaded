package iri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class JLCGetConstructors2 extends FormattingHelper {

	public JLCGetConstructors2() {

	}

	// not in original
	public JLCGetConstructors2(String s) {

	}

	@SuppressWarnings("rawtypes")
	public String run() throws Exception {
		Method m = Class.class.getMethod("getConstructors");
		Constructor[] o = (Constructor[]) m.invoke(JLCGetConstructors2.class);
		//		Method m2 = (Method) m.invoke(JLCGetDecMethod.class, "foo", new Class[] { String.class, Integer.TYPE });
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetConstructors2().run());
	}

}
