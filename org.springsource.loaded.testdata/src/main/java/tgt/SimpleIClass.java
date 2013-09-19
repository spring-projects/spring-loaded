package tgt;

public class SimpleIClass implements SimpleI {
	public int toInt(String s) {
		return Integer.parseInt(s);
	}

	public String fromInt() {
		return Integer.toString(42);
	}

	public int changingReturnType() {
		return 111;
	}
}
