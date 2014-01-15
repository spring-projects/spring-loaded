package catchers;

public class Runner2 {

	X x = new X();
	Y y = new Y();
	Z z = new Z();

	public int runPublicX() {
		return x.publicMethod();
	}

	public int runPublicY() {
		return y.publicMethod();
	}

	public int runPublicZ() {
		return z.publicMethod();
	}

	public char runDefaultX() {
		return x.defaultMethod();
	}

	public char runDefaultY() {
		return y.defaultMethod();
	}

	public char runDefaultZ() {
		return z.defaultMethod();
	}

	public long runProtectedX() {
		return x.callProtectedMethod();
	}

	public long runProtectedY() {
		return y.callProtectedMethod();
	}

	public long runProtectedZ() {
		return z.callProtectedMethod();
	}
}
