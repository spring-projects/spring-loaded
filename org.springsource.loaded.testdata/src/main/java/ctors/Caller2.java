package ctors;

public class Caller2 {

	public Object runA() {
		return new Callee();
	}

	public Object runB() {
		return new Callee2("abcde");
	}
}
