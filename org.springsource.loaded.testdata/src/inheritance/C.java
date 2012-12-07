package inheritance;

public class C {
public static void main(String[]argv) {
  System.out.println(run());
}
	public static int run() {
		A a = new B();
		return a.foo();
	}
}
