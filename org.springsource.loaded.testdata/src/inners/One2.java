package inners;

public class One2 {

	public static void runner() {
		new One2().run();
	}

	public void run() {
		m();
	}

	public void m() {
		new Inner();
	}

	static class Inner {
		Inner() {
		}
	}
}
