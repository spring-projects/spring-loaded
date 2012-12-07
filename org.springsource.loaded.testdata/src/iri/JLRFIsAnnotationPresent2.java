package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRFIsAnnotationPresent2 extends FormattingHelper {

	@Deprecated
	public String string;

	@AnnoT
	public int i;

	public String run() throws Exception {
		Method m = Field.class.getMethod("isAnnotationPresent", Class.class);
		Field f = JLRFIsAnnotationPresent2.class.getDeclaredField("i");
		boolean b = (Boolean) m.invoke(f, Deprecated.class);
		boolean c = (Boolean) m.invoke(f, AnnoT.class);
		return Boolean.toString(b) + Boolean.toString(c);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFIsAnnotationPresent2().run());
	}

}
