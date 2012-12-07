package enumtests;

public class RunnerA {

	public static void main(String[] args) {
		run1();
	}

	public static void run1() {
		System.out.println(Colours.Red);
		System.out.println(Colours.Green);
		System.out.println(Colours.Blue);
		Colours[] colours = Colours.values();
		System.out.print("[");
		for (int i = 0; i < colours.length; i++) {
			if (i > 0) {
				System.out.print(" ");
			}
			System.out.print(colours[i]);
		}
		System.out.println("]");
		System.out.println("value count = " + colours.length);
	}
}
