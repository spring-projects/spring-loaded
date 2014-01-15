package reflection.invocation;

/**
 * For invocation testing, we need a class hierarchy fo some complexity to see if dispatching works right.
 * 
 * Will be using a 3 deep hierarchy C extends B extends A.
 * 
 * Further we will be adding methods with different modifiers
 * 
 * @author kdvolder
 */
public class C002 extends B {

	public String pubEarly() {
		return "C002.pubEarly()";
	}

	@SuppressWarnings("unused")
	private String privEarly() {
		return "C002.privEarly()";
	}

	static String staticEarly() {
		return "C002.staticEarly()";
	}

	public String pubLate() {
		return "C002.pubLate()";
	}

	@SuppressWarnings("unused")
	private String privLate() {
		return "C002.privLate()";
	}

	static String staticLate() {
		return "C002.staticLate()";
	}

}
