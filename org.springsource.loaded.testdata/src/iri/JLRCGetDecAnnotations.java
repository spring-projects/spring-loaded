package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRCGetDecAnnotations extends FormattingHelper {

	@AnnoT
	public JLRCGetDecAnnotations() {
	}

	public String run() throws Exception {
		Method m = Constructor.class.getMethod("getDeclaredAnnotations");
		Constructor<JLRCGetDecAnnotations> c = JLRCGetDecAnnotations.class.getDeclaredConstructor();
		Annotation[] o = (Annotation[]) m.invoke(c);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRCGetDecAnnotations().run());
	}

}
