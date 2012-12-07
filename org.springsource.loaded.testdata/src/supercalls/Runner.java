package supercalls;

public class Runner {

	public static Super top = new Super();
	public static Sub bottom = new Sub();

	public int runSubMethod() {
		return bottom.method();
	}

	public String runSubToString() {
		return bottom.toString();
	}
}
