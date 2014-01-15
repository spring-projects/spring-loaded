package ctors;

public class CallerB2 {

	public Object runA() {
		return new CalleeB();
	}

	public Object runB() {
		return new CalleeB2("abcde");
	}
}
