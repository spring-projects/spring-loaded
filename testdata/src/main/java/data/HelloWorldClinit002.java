package data;

public class HelloWorldClinit002 {
	public void greet() {
		System.out.println("Greet from HelloWorldClinit 2");
	}

	static {
		int i = 1;
		int j = 2;
		for (int k = 0; k < 5; k++) {
			i += j;
		}
	}

	public String getValue() {
		return "message from HelloWorld";
	}

	public String getValueWithParams(String a, String b) {
		return "message with inserts " + a + " and " + b;
	}

	public static String getStaticValueWithParams(String a, String b) {
		return "message with inserts " + a + " and " + b;
	}

	public static String getStaticValueWithPrimitiveParams(String a, int i, char ch) {
		return "message with inserts " + a + " and " + i + " and " + ch;
	}

	public static String getStaticValueWithPrimitiveDSParams(long l, String a, double d, boolean b) {
		return "message with inserts " + l + " and " + a + " and " + d + " and " + b;
	}
}