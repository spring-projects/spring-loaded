package enumtests;

public class RunnerD2 {

	public static void main(String[] args) {
		run1();
	}

	public static void run1() {
		ColoursD2[] colours = ColoursD2.values();
		System.out.print("[");
		for (int i = 0; i < colours.length; i++) {
			if (i > 0) {
				System.out.print(" ");
			}
			System.out.print(colours[i] + " " + colours[i].getCharValue() + " " + colours[i].ordinal());
		}
		System.out.println("]");
		System.out.println("value count = " + colours.length);
	}
}
