package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRMGetDecAnnotations2 extends FormattingHelper {

	@AnnoT
	public void string() {
	}

	@Deprecated
	public void newmethod() {

	}

	public String run() throws Exception {
		Method m = Method.class.getMethod("getDeclaredAnnotations");
		Method mm = JLRMGetDecAnnotations2.class.getDeclaredMethod("newmethod");
		Annotation[] o = (Annotation[]) m.invoke(mm);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRMGetDecAnnotations2().run());
	}

}
