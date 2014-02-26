package reflection.methodannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;

//@SuppressWarnings("unused")
public class ClassTarget003 {

	@AnnoT3("field")
	String s;

	@AnnoT
	public ClassTarget003(String s) {
		this.s = s;
	}

	@AnnoT2
	public void pubMethod() {
	}

	@AnnoT3(value = "Bar")
	private void privMethod() {
	}

	@Deprecated
	boolean defaultMethod(String a, int b) {
		return ("" + b).equals(a);
	}

	@AnnoT3(value = "Hi")
	public String addedMethod() {
		return "ha";
	}

}
