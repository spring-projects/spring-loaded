package fields;

public class Two extends One {
	String b = "b from Two";
	private String c = "c from Two";

	public String getTwoA() {
		return a;
	}

	public String getTwoB() {
		return b;
	}

	public String getTwoC() {
		return c;
	}

	public void setTwoA(String newValue) {
		a = newValue;
	}

	public void setTwoB(String newValue) {
		b = newValue;
	}

	public void setTwoC(String newValue) {
		c = newValue;
	}

}
