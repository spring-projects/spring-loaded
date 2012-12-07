package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLRFGet2 extends FormattingHelper {

	public String string;

	public String string2;

	public String run() throws Exception {
		Method m = Field.class.getMethod("get", Object.class);
		Field f = JLRFGet2.class.getDeclaredField("string2");
		this.string2 = "goodbye";
		String o = (String) m.invoke(f, this);
		return o;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFGet2().run());
	}

}
