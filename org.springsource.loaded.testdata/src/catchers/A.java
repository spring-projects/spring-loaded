package catchers;

@SuppressWarnings("unused")
public class A {
	public int publicMethod() {
		return 65;
	}

	private int privateMethod() {
		return 2222;
	}

	void defaultMethod() {
	}

	protected int protectedMethod() {
		return 23;
	}
}
