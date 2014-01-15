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
public class B002 extends A {

	public String pubEarly() {
		return "B002.pubEarly()";
	}

	@SuppressWarnings("unused")
	private String privEarly() {
		return "B002.privEarly()";
	}

	static String staticEarly() {
		return "B002.staticEarly()";
	}

	public String pubLate() {
		return "B002.pubLate()";
	}

	@SuppressWarnings("unused")
	private String privLate() {
		return "B002.privLate()";
	}

	static String staticLate() {
		return "B002.staticLate()";
	}

}
