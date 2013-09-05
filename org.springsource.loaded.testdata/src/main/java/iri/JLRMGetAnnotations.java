package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRMGetAnnotations extends FormattingHelper {

	@AnnoT
	public void string() {
	}

	public String run() throws Exception {
		Method m = Method.class.getMethod("getAnnotations");
		Method mm = JLRMGetAnnotations.class.getDeclaredMethod("string");
		Annotation[] o = (Annotation[]) m.invoke(mm);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRMGetAnnotations().run());
	}

}
