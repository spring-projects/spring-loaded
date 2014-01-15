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
public class A {

	public String pubEarly() {
		return "A.pubEarly()";
	}

	@SuppressWarnings("unused")
	private String privEarly() {
		return "A.privEarly()";
	}

	static String staticEarly() {
		return "A.staticEarly()";
	}

	public String pubDeleted() {
		return "A.pubDeleted()";
	}

	@SuppressWarnings("unused")
	private String privDeleted() {
		return "A.privDeleted()";
	}

	static String staticDeleted() {
		return "A.staticDeleted()";
	}

}
