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
public class A002 {

	public String pubEarly() {
		return "A002.pubEarly()";
	}

	@SuppressWarnings("unused")
	private String privEarly() {
		return "A002.privEarly()";
	}

	static String staticEarly() {
		return "A002.staticEarly()";
	}

	public String pubLate() {
		return "A002.pubLate()";
	}

	@SuppressWarnings("unused")
	private String privLate() {
		return "A002.privLate()";
	}

	static String staticLate() {
		return "A002.staticLate()";
	}

}
