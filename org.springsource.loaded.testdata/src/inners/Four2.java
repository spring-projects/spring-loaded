package inners;

public class Four2 {

	public static void runner() {
		new Four2().run();
	}

	public void run() {
		new Inner();
	}

	protected static class Inner {
		private Inner() {
		}
	}
}
