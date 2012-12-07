package invokevirtual;

public class X {

	public int foo() {
		return 1111;
	}

	public int run() {
		Y y = new Y();
		return y.foo();
	}
}
