package enumtests;

public enum ColoursB implements Intface {
	Red(111), Green(222), Blue(333);

	int value;

	private ColoursB(int i) {
		this.value = i;
	}

	@Override
	public int getIntValue() {
		return value;
	}
}
