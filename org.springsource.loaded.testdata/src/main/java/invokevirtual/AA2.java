package invokevirtual;

public class AA2 {

	public static String callfoo() {
		BB2 b = new BB2();
		return b.foo();
	}

	public static String callbar() {
		BB2 b = new BB2();
		return b.bar();
	}
}
