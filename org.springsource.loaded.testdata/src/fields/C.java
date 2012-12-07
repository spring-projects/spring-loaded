package fields;

public class C extends B {

	public void setInt(int newvalue) {
		this.i = newvalue;
	}

	public void setString(String newString) {
		this.s = newString;
	}

	public String getString() {
		return this.s;
	}

	public int getInt() {
		return this.i;
	}
}
