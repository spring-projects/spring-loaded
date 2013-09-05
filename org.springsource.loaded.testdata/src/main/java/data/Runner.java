package data;

/**
 * Long running program for checking watching and class replacement.
 * 
 * @author Andy Clement
 */
public class Runner {

	public static void main(String[] args) {
		new Runner().run();
	}

	static Orange orange = new Orange();

	/**
	 * Every 10 seconds, call one()
	 */
	public void run() {
		while (true) {
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
			}
			try {
				orange.one();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

}
