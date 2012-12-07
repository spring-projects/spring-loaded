package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import reflection.AnnoT;

@AnnoT
public class JLRFSet2 extends FormattingHelper {

	public JLRFSet2() {

	}

	public String string;

	public String string2;

	public String run() throws Exception {
		Method m = Field.class.getMethod("set", Object.class, Object.class);
		Field f = JLRFSet2.class.getDeclaredField("string2");
		m.invoke(f, this, "goodbye");
		return string2;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFSet2().run());
	}

}
