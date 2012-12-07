package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLCGetDecField2 extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredField", String.class);
		Field f = (Field) m.invoke(JLCGetDecField2.class, "bar");
		return format(f);
	}

	// this wasn't in the original type
	int bar;

	public static void main(String[] argv) throws Exception {
		new JLCGetDecField2().run();
	}

}
