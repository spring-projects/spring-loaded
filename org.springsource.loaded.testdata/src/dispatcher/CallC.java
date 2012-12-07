package dispatcher;

public class CallC {

	public void runInstance() {
		C c = new C();
		c.foo("abc");
	}

	public void runStatic() {
		C c = new C();
		C.foo(c, "abc");
	}

}
