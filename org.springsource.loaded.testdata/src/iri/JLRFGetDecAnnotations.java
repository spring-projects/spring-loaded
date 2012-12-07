package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import reflection.AnnoT;

public class JLRFGetDecAnnotations extends FormattingHelper {

	@AnnoT
	public String string;

	public String run() throws Exception {
		Method m = Field.class.getMethod("getDeclaredAnnotations");
		Field f = JLRFGetDecAnnotations.class.getDeclaredField("string");
		Annotation[] o = (Annotation[]) m.invoke(f);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFGetDecAnnotations().run());
	}

}
