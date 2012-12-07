package enumtests;

public class RunnerB {

	public static void main(String[] args) {
		run1();
	}

	public static void run1() {
		System.out.println(ColoursB.Red.getIntValue());
		System.out.println(ColoursB.Green.getIntValue());
		System.out.println(ColoursB.Blue.getIntValue());
		ColoursB[] colours = ColoursB.values();
		System.out.print("[");
		for (int i = 0; i < colours.length; i++) {
			if (i > 0) {
				System.out.print(" ");
			}
			System.out.print(colours[i].getIntValue());
		}
		System.out.println("]");
		System.out.println("value count = " + colours.length);
	}
}
