package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLCGetField2 extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getField", String.class);
		Field f = (Field) m.invoke(JLCGetField2.class, "bar");
		return format(f);
	}

	// this wasn't in the original type
	public int bar;

	public static void main(String[] argv) throws Exception {
		new JLCGetField2().run();
	}

}
