package catchers;

@SuppressWarnings("unused")
public class X {
	public int publicMethod() {
		return 1;
	}

	private void privateMethod() {
	}

	char defaultMethod() {
		return 'a';
	}

	protected long protectedMethod() {
		return 100;
	}

	public long callProtectedMethod() {
		return protectedMethod();
	}
}
