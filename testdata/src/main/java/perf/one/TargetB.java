package perf.one;

public class TargetB {

	int j;

	public void foo() {
		for (int i = 0; i < 20; i++) {
			j = 5;
		}
	}

	public int bar(int i) {
		j = 5;
		return i;
	}

	public void goo(String s, int i) {
		j = 5;
	}

}
