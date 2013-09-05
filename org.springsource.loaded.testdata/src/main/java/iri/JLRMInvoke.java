package iri;

import java.lang.reflect.Method;

public class JLRMInvoke extends FormattingHelper {

	public String runner() {
		return "ran";
	}

	public String run() throws Exception {
		Method m = Method.class.getMethod("invoke", Object.class, Object[].class);
		Method mm = JLRMInvoke.class.getDeclaredMethod("runner");
		String s = (String) m.invoke(mm, this, (Object[]) null);
		return s;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRMInvoke().run());
	}

}
