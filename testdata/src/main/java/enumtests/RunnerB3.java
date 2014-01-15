package enumtests;

public class RunnerB3 {

	public static void main(String[] args) {
		run1();
	}

	public static void run1() {
		System.out.println(ColoursB3.Red.getIntValue());
		System.out.println(ColoursB3.Green.getIntValue());
		System.out.println(ColoursB3.Blue.getIntValue());
		ColoursB3[] colours = ColoursB3.values();
		System.out.print("[");
		for (int i = 0; i < colours.length; i++) {
			if (i > 0) {
				System.out.print(" ");
			}
			System.out.print(colours[i].getDoubleIntValue());
		}
		System.out.println("]");
		System.out.println("value count = " + colours.length);
	}
}
