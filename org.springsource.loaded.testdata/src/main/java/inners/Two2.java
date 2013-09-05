package inners;

public class Two2 {

	public static void runner() {
		new Two2().run();
	}

	public void run() {
		m();
	}

	public void m() {
		new TwoDefault();
	}
}
