package data;

public class HelloWorld002 {
	public void greet() {
		System.out.println("Greet from HelloWorld 2");
	}

	public String getValue() {
		return "message from HelloWorld002";
	}

	public String getValueWithParams(String a, String b) {
		return "message with inserts " + b + " and " + a;
	}

	public static String getStaticValueWithParams(String a, String b) {
		return "static message with inserts " + b + " and " + a;
	}

	public static String getStaticValueWithPrimitiveParams(String a, int i, char ch) {
		return "message with inserts " + ch + " and " + i + " and " + a;
	}

	public static String getStaticValueWithPrimitiveDSParams(long l, String a, double d, boolean b) {
		return "message with inserts " + b + " and " + d + " and " + a + " and " + l;
	}

}