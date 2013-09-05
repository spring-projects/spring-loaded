package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import reflection.AnnoT;

@AnnoT
public class JLCGetAnnotations extends FormattingHelper {

	public JLCGetAnnotations() {

	}

	public String run() throws Exception {
		Method m = Class.class.getMethod("getAnnotations");
		Annotation[] o = (Annotation[]) m.invoke(JLCGetAnnotations.class);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetAnnotations().run());
	}

}
