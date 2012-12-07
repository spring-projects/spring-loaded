package enumtests;

public enum ColoursB2 implements Intface {
	Red(111), Green(222), Blue(333), Yellow(444);

	int value;

	private ColoursB2(int i) {
		this.value = i * 2;
	}

	@Override
	public int getIntValue() {
		return value;
	}
}
