package iri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRCIsAnnotationPresent extends FormattingHelper {

	@Deprecated
	public JLRCIsAnnotationPresent() {

	}

	public String run() throws Exception {
		Method m = Constructor.class.getMethod("isAnnotationPresent", Class.class);
		Constructor<JLRCIsAnnotationPresent> c = JLRCIsAnnotationPresent.class.getDeclaredConstructor();
		boolean b = (Boolean) m.invoke(c, Deprecated.class);
		boolean cc = (Boolean) m.invoke(c, AnnoT.class);
		return Boolean.toString(b) + Boolean.toString(cc);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRCIsAnnotationPresent().run());
	}

}
