package tgt;

public class SimpleIClass005 implements SimpleI005 {
	public int toInt(String s) {
		return Integer.parseInt(s);
	}

	public String fromInt() {
		return Integer.toString(42);
	}

	public int toInt(int i) {
		return i * 2;
	}

	public String changingReturnType() {
		return "abc";
	}
}
