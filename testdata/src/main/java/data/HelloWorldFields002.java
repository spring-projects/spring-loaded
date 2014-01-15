package data;

public class HelloWorldFields002 {

	String theMessage = "Hello Christian";

	public void greet() {
		System.out.println(theMessage);
	}

	public void setMessage(String newValue) {
		theMessage = newValue;
	}

	//	public String getValue() {
	//		return "message from HelloWorld";
	//	}
	//
	//	public String getValueWithParams(String a, String b) {
	//		return "message with inserts " + a + " and " + b;
	//	}
	//
	//	public static String getStaticValueWithParams(String a, String b) {
	//		return "static message with inserts " + a + " and " + b;
	//	}
	//
	//	public static String getStaticValueWithPrimitiveParams(String a, int i, char ch) {
	//		return "message with inserts " + a + " and " + i + " and " + ch;
	//	}
	//
	//	public static String getStaticValueWithPrimitiveDSParams(long l, String a, double d, boolean b) {
	//		return "message with inserts " + l + " and " + a + " and " + d + " and " + b;
	//	}
}