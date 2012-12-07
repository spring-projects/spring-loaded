package invokespecial;

public class C extends B {

	public int getInt() {
		return super.getInt();
	}

	public String toString(boolean b, String s) {
		return super.toString(b, s);
	}

	public String run1() {
		return Integer.toString(getInt());
	}

	public String run2() {
		return toString(false, "abc");
	}
}
