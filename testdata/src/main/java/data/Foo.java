package data;


public class Foo {

	public static void main(String[] args) throws Exception {
		print("Hello");
		print("World");

		System.out.println("Annotations: " + Foo.class.getDeclaredField("i"));
		System.out.println("Annotations: " + Foo.class.getDeclaredField("i"));
		System.out.println("Annotations: " + Foo.class.getDeclaredField("i"));
		System.out.println("Annotations: " + Foo.class.getDeclaredField("i"));
		System.out.println("Annotations: " + Foo.class.getDeclaredField("i").getAnnotation(Wiggle.class));
		System.out.println("Annotations: " + Foo.class.getDeclaredField("i").getAnnotation(Wiggle.class));
		System.out.println("Annotations: " + Foo.class.getDeclaredField("i").getAnnotation(Wiggle.class));
		System.out.println("Annotations: " + Foo.class.getDeclaredField("i").getAnnotation(Wiggle.class));
		System.out.println("Annotations: " + Foo.class.getDeclaredField("i").getAnnotation(Wiggle.class));
		System.out.println("Annotations: " + Foo.class.getDeclaredField("i").getAnnotation(Wiggle.class));
	}

	public static void print(String message) {
		System.out.println();// dosomethingwithit(message));
	}

	// private static String dosomethingwithit(String message) {
	// return message.toUpperCase();
	// }

	@Wiggle
	int i;
}
