package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLCGetFields extends FormattingHelper {

	public String run() throws Exception {
		Method m = Class.class.getMethod("getFields");
		Field[] fs = (Field[]) m.invoke(JLCGetFields.class);
		return format(fs);
	}

	String aString;

}
