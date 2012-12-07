package reflection.methodannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;

public class ClassTarget002 {

	@AnnoT3("field")
	String s;

	@AnnoT
	public ClassTarget002(String s) {
		this.s = s;
	}

	@AnnoT2
	@AnnoT3("noisy")
	public void pubMethod() {
	}

	@SuppressWarnings("unused")
	private void privMethod() {
	}

	@Deprecated
	boolean defaultMethod(String a, int b) {
		return ("" + b).equals(a);
	}

	public String addedMethod() {
		return "ha";
	}

}
