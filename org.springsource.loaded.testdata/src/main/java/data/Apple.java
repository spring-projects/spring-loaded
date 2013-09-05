package data;

public class Apple {

	public int intField;
	public static int staticIntField;

	public void run() {
		System.out.println("Apple.run() is running ");
	}

	public String runWithReturn() {
		return "alphabeti spaghetti";
	}

	public void runWithParam(String string) {
		System.out.println("Apple.run(" + string + ") is running ");
	}

}
