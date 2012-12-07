package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLRFGetAnnotations2 extends FormattingHelper {

	public String string;

	@Deprecated
	public int i;

	public String run() throws Exception {
		Method m = Field.class.getMethod("getAnnotations");
		Field f = JLRFGetAnnotations2.class.getDeclaredField("i");
		Annotation[] o = (Annotation[]) m.invoke(f);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFGetAnnotations2().run());
	}

}
