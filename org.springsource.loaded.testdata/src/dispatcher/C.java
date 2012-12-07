package dispatcher;

public class C {

	public void foo(String s) {
		System.out.println("instance foo running");
	}

	public static void foo(C c, String s) {
		System.out.println("static foo running");
	}

}
