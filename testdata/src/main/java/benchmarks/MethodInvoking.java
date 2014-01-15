package benchmarks;

public class MethodInvoking {
	int i;

	public String run() {
		// warmup
		for (int i = 0; i < 1000000; i++) {
			m();
		}
		// measure
		long l = System.currentTimeMillis();
		for (int i = 0; i < 50000000; i++) {
			m();
		}
		return Long.toString(System.currentTimeMillis() - l);
	}

	public void m() {
		i = 4;
	}

	public static void main(String[] args) {
		System.out.println(new MethodInvoking().run());
	}
}