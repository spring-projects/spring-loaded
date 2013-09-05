package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLCGetFields2 extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getFields");
		Field[] fs = (Field[]) m.invoke(JLCGetFields2.class);
		return format(fs);
	}

	String aString;

	public int anInt;

}
