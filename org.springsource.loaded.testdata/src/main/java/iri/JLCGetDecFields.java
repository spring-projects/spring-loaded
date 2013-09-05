package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLCGetDecFields extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getDeclaredFields");
		Field[] fs = (Field[]) m.invoke(JLCGetDecFields.class);
		return format(fs);
	}

	String aString;

}
