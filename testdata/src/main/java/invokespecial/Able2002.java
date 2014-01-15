package invokespecial;

public class Able2002 extends Top {
	private int iii = 4;

	public String toString() {
		return "def";
	}

	public String withParam(int i) {
		return Integer.toString(i) + Integer.toString(i);
	}

	public String withDoubleSlotParam(long l) {
		return Long.toString(l) + Long.toString(l);
	}

	public String withDoubleSlotParamPrivateVariable() {
		return Integer.toString(iii) + Integer.toString(iii);
	}
}