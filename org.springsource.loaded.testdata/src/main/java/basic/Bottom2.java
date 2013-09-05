package basic;

public class Bottom2 extends Top2 {

	public void method() {
		System.out.println("abc");
	}

	public void run() {
		new Bottom2().method();
	}

}
