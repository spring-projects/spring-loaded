package perf.one;

public class CallerB {

	TargetB target = new TargetB();

	public long run() {
		long stime = System.nanoTime();
		for (int i = 0; i < 100000; i++) {
			target.foo();
		}
		//		int total = 0;
		//		for (int i = 0; i < 8000000; i++) {
		//			total += target.bar(123);
		//		}
		//		for (int i = 0; i < 8000000; i++) {
		//			target.goo("abc", 123);
		//		}
		return (System.nanoTime() - stime);
	}

	public void warmup() {
		for (int i = 0; i < 1000; i++) {
			run();
		}
	}

	public static void main(String[] argv) {
		CallerB c = new CallerB();
		c.warmup();
		System.out.println("real run");
		c.execute();
		c.execute();
		c.execute();
		c.execute();
		c.execute();
	}

	public void execute() {
		System.out.println((run() / 1000000.0d) + "ms");
	}
}
