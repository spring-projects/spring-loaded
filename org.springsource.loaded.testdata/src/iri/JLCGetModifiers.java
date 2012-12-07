package iri;

import java.lang.reflect.Method;

public class JLCGetModifiers extends FormattingHelper {

	public String run() throws Exception {
		JLCGetModifiers.class.getConstructor();
		Method m = Class.class.getMethod("getModifiers");
		int o = (Integer) m.invoke(Helper.class);
		return Integer.toString(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetModifiers().run());
	}

}

class Helper {

}
