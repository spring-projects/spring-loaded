package catchers;

@SuppressWarnings("unused")
public class Z extends Y {

	public int publicMethod() {
		return 3;
	}

	private void privateMethod() {
	}

	char defaultMethod() {
		return 'c';
	}

	protected long protectedMethod() {
		return 300;
	}
}
