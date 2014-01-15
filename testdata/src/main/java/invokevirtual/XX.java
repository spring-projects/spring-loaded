package invokevirtual;

public class XX {

	public int foo() {
		return 1111;
	}

	public int run1() {
		XX zz = new ZZ();
		return zz.foo();
	}

	public int run2() {
		YY zz = new ZZ();
		return zz.foo();
	}

	public int run3() {
		ZZ zz = new ZZ();
		return zz.foo();
	}
}
