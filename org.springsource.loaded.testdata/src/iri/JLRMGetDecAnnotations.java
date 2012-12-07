package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRMGetDecAnnotations extends FormattingHelper {

	@AnnoT
	public void string() {
	}

	public String run() throws Exception {
		Method m = Method.class.getMethod("getDeclaredAnnotations");
		Method mm = JLRMGetDecAnnotations.class.getDeclaredMethod("string");
		Annotation[] o = (Annotation[]) m.invoke(mm);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRMGetDecAnnotations().run());
	}

}
