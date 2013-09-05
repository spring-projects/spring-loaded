package enumtests;

public enum ColoursD2 {
	Red("aaa"), Green("bbb"), Blue("ccc");

	char value;

	private ColoursD2(String i) {
		this.value = i.charAt(0);
	}

	public char getCharValue() {
		return value;
	}
}
