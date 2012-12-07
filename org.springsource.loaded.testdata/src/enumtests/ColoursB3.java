package enumtests;

public enum ColoursB3 implements Intface3 {
	Red(111), Green(222), Blue(333);

	int value;

	private ColoursB3(int i) {
		this.value = i;
	}

	@Override
	public int getIntValue() {
		return value;
	}

	@Override
	public int getDoubleIntValue() {
		return value * 2;
	}
}
