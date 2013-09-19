package tgt;

public class SimpleIClass003 implements SimpleI003 {
	public int toInt(String s) {
		return Integer.parseInt(s);
	}

	public String fromInt() {
		return Integer.toString(42);
	}

	public String stringify(double d, int i, long l, boolean b) {
		return "" + d + i + l + b;
	}

}
