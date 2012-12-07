package invokespecial;

public class Able {
	private int iii = 1;

	public String toString() {
		return "abc";
	}

	public String withParam(int i) {
		return Integer.toString(i);
	}

	public String withDoubleSlotParam(long l) {
		return Long.toString(l);
	}

	public String withDoubleSlotParamPrivateVariable() {
		return Integer.toString(iii);
	}

}