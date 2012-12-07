package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLRFGet extends FormattingHelper {

	public String string;

	public String run() throws Exception {
		Method m = Field.class.getMethod("get", Object.class);
		Field f = JLRFGet.class.getDeclaredField("string");
		this.string = "hello";
		String o = (String) m.invoke(f, this);
		return o;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFGet().run());
	}

}
