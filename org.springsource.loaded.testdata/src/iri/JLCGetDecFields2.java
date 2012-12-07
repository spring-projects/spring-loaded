package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLCGetDecFields2 extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredFields");
		Field[] fs = (Field[]) m.invoke(JLCGetDecFields2.class);
		return format(fs);
	}

	String aString;

	int anInt;

}
