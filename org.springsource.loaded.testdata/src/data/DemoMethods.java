package data;

public class DemoMethods {

	public static void main(String[] args) throws Exception {
		print("Hello World");
		print("Hello World");
		print("Hello World");
		print("Hello World");
		print("Hello World");
		print("Hello World");
		print("Hello World");
		print("Hello World");
		print("Hello World");
		print("Hello World");
		print("Hello World");
		print("Hello World");
	}

	public static void print(String message) {
		System.out.println(upperize(message));
	}

	private static String upperize(String message) {
		return message.toUpperCase();
	}

}