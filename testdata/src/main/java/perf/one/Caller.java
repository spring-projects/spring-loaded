package perf.one;

public class Caller {

	Target target = new Target();

	public long run() {
		long stime = System.nanoTime();
		for (int i = 0; i < 1000000; i++) {
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
		Caller c = new Caller();
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
