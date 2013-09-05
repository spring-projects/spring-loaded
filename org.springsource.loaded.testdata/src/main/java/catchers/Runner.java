package catchers;

public class Runner {

	B b = new B();

	public String runToString() {
		return b.toString();
	}

	public Object runPublicMethod() {
		return b.publicMethod();
	}

	public Object runProtectedMethod() {
		return b.callProtectedMethod();
	}
}
