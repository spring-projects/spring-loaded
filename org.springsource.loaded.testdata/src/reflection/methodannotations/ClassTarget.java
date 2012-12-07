package reflection.methodannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;

@SuppressWarnings("unused")
public class ClassTarget {

	@AnnoT3("field")
	String s;

	@AnnoT
	public static final int ZERO = 0;

	@AnnoT
	public ClassTarget(String s) {
		this.s = s;
	}

	@AnnoT
	@AnnoT2
	public void pubMethod() {
	}

	@AnnoT3(value = "Foo")
	private void privMethod() {
	}

	boolean defaultMethod(String a, int b) {
		return ("" + b).equals(a);
	}

}
