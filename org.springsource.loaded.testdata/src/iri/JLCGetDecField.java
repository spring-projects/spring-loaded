package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLCGetDecField extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredField", String.class);
		Field f = (Field) m.invoke(JLCGetDecField.class, "foo");
		//		Method m2 = (Method) m.invoke(JLCGetDecMethod.class, "foo", new Class[] { String.class, Integer.TYPE });
		return format(f);
	}

	String foo;

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetDecField().run());
	}

}
