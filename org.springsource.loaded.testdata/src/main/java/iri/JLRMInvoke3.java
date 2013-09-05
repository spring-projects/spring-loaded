package iri;

import java.lang.reflect.Method;

public class JLRMInvoke3 extends FormattingHelper {

	public String runner() {
		return "ran";
	}

	public String newmethod2(String s, int i) {
		return s + Integer.toString(i);
	}

	public String run() throws Exception {
		Method m = Method.class.getMethod("invoke", Object.class, Object[].class);
		Method mm = JLRMInvoke3.class.getDeclaredMethod("newmethod2", String.class, Integer.TYPE);
		String s = (String) m.invoke(mm, this, new Object[] { "abc", 3 });
		return s;
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRMInvoke3().run());
	}

}
