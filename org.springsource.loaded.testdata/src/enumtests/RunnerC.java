package enumtests;

public class RunnerC {

	public static void main(String[] args) {
		callGetEnumConstants();
	}

	public static void callGetEnumConstants() {
		ColoursC[] values = ColoursC.class.getEnumConstants();
		System.out.print("[");
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				System.out.print(" ");
			}
			System.out.print(values[i]);
		}
		System.out.println("]");
		System.out.println("value count = " + values.length);
	}

	public static void callValueOf1() {
		ColoursC[] values = ColoursC.class.getEnumConstants();
		System.out.print("valueOf(String)=[");
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				System.out.print(" ");
			}
			System.out.print(ColoursC.valueOf(values[i].toString()));
		}
		System.out.println("]");
		System.out.println("value count = " + values.length);
	}

	public static void callValueOf2() {
		ColoursC[] values = ColoursC.class.getEnumConstants();
		System.out.print("valueOf(String)=[");
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				System.out.print(" ");
			}
			System.out.print(Enum.valueOf(ColoursC.class, values[i].toString()));
		}
		System.out.println("]");
		System.out.println("value count = " + values.length);
	}
}
