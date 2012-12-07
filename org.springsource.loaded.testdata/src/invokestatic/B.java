package invokestatic;

public class B {

	private static String somemethod() {
		return "hello";
	}

	public String run() {
		return somemethod();
	}
}
