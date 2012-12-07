package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLRFSet extends FormattingHelper {

	public String string;

	public String run() throws Exception {
		Method m = Field.class.getMethod("set", Object.class, Object.class);
		Field f = JLRFSet.class.getDeclaredField("string");
		m.invoke(f, this, "hello");
		System.out.println(string);
		return string;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFSet().run());
	}

}
