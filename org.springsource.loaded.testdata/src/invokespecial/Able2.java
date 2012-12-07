package invokespecial;

class Top {
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

public class Able2 extends Top {
	@SuppressWarnings("unused")
	private int iii = 4;

	//	public String toString() {
	//		return "abc";
	//	}
	//
	//	public String withParam(int i) {
	//		return Integer.toString(i);
	//	}
	//
	//	public String withDoubleSlotParam(long l) {
	//		return Long.toString(l);
	//	}
	//
	//	public String withDoubleSlotParamPrivateVariable() {
	//		return Integer.toString(iii);
	//	}
}