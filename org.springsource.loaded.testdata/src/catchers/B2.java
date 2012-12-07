package catchers;

public class B2 extends A {
	public String toString() {
		return "hey!";
	}

	public int publicMethod() {
		return 66;
	}

	@SuppressWarnings("unused")
	private int privateMethod() {
		return 4444;
	}

	public Object callProtectedMethod() {
		return protectedMethod();
	}

	protected int protectedMethod() {
		return 32;
	}
}
