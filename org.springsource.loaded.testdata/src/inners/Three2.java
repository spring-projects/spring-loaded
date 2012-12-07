package inners;

public class Three2 {

	public static void runner() {
		new Three2().run();
	}

	public void run() {
		new Inner();
	}

	private static class Inner {
		private Inner() {
		}
	}
}
