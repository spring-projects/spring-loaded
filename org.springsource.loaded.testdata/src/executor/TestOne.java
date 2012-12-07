package executor;

public class TestOne {

	public int i = 101;

	// Regular method
	public long foo(String s) {
		return Long.parseLong(s);
	}

	// Overriding an inherited method
	public int hashCode() {
		return 37;
	}
}
