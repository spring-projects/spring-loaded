package invokespecial;

public class Q extends P {
	public int foo() {
		return 2;
	}

	public int run() {
		return super.foo();
	}
}
