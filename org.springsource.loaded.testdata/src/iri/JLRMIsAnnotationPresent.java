package iri;

import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRMIsAnnotationPresent extends FormattingHelper {

	@Deprecated
	public void string() {
	}

	public String run() throws Exception {
		Method m = Method.class.getMethod("isAnnotationPresent", Class.class);
		Method mm = JLRMIsAnnotationPresent.class.getDeclaredMethod("string");
		boolean b = (Boolean) m.invoke(mm, Deprecated.class);
		boolean c = (Boolean) m.invoke(mm, AnnoT.class);
		return Boolean.toString(b) + Boolean.toString(c);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRMIsAnnotationPresent().run());
	}

}
