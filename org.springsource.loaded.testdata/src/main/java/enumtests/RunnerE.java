package enumtests;

public class RunnerE {

	public static void main(String[] args) {
		run1();
	}

	public static void run1() {
		ColoursD[] colours = ColoursD.values();
		System.out.print("[");
		for (int i = 0; i < colours.length; i++) {
			if (i > 0) {
				System.out.print(" ");
			}
			System.out.print(colours[i] + " " + colours[i].getIntValue() + " " + colours[i].ordinal());
		}
		System.out.println("]");
		System.out.println("value count = " + colours.length);
	}
}
