package iri;

import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRMIsAnnotationPresent2 extends FormattingHelper {

	@Deprecated
	public void string() {
	}

	@AnnoT
	public void newmethod() {
	}

	public String run() throws Exception {
		Method m = Method.class.getMethod("isAnnotationPresent", Class.class);
		Method mm = JLRMIsAnnotationPresent2.class.getDeclaredMethod("newmethod");
		boolean b = (Boolean) m.invoke(mm, Deprecated.class);
		boolean c = (Boolean) m.invoke(mm, AnnoT.class);
		return Boolean.toString(b) + Boolean.toString(c);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRMIsAnnotationPresent2().run());
	}

}
