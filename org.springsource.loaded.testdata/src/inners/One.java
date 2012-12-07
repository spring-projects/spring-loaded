package inners;

public class One {

	public static void runner() {
		new One().run();
	}

	public void run() {
		new Inner();
	}

	static class Inner {
		Inner() {
		}
	}
}
