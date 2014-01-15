package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLCGetField extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getField", String.class);
		Field f = (Field) m.invoke(JLCGetField.class, "foo");
		//		Method m2 = (Method) m.invoke(JLCGetDecMethod.class, "foo", new Class[] { String.class, Integer.TYPE });
		return format(f);
	}

	public String foo;

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetField().run());
	}

}
