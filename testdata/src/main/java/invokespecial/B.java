package invokespecial;

public class B extends A {

	public int getInt() {
		return 66;
	}

	public String toString(boolean b, String s) {
		return new StringBuilder("66").append(b).append(s).toString();
	}
}
