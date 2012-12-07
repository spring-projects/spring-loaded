package iri;

import java.lang.reflect.Constructor;

public class JLCGetConstructorB2 extends FormattingHelper {

	public JLCGetConstructorB2() {

	}

	@SuppressWarnings("unused")
	private JLCGetConstructorB2(String s) {

	}

	public String run() throws Exception {
		Constructor<?> o = JLCGetConstructorB2.class.getConstructor(String.class);
		return format(o);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLCGetConstructorB2().run());
	}

}
