package data;

public class Pear {

	public int intField;
	public static int staticIntField;
	public int[] intArray = new int[] { 4, 3, 2, 1 };
	public static int[] staticIntArray = new int[] { 44, 33, 22, 11 };

	public void run() {
		System.out.println("Apple.run() is running ");
	}

	public int[] getIntArray() {
		return intArray;
	}

	public String runWithReturn() {
		return "alphabeti spaghetti";
	}

	public void runWithParam(String string) {
		System.out.println("Apple.run(" + string + ") is running ");
	}

}
