package executor;

public class TestOne2 {

	public int i;

	// Regular method
	public long foo(String s) {
		return Long.parseLong(s);
	}

	// Overriding an inherited method
	public int hashCode() {
		return i * 2;
	}
}
