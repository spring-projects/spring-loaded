package enumtests;

public enum ColoursD {
	Red(1111), Green(2222), Blue(3333);

	int value;

	private ColoursD(int i) {
		this.value = i;
	}

	public int getIntValue() {
		return value;
	}
}
