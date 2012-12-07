package reflection.invocation;

/**
 * For invocation testing, we need a class hierarchy of some complexity to see if dispatching works right.
 * 
 * Will be using a 3 deep hierarchy C extends B extends B.
 * 
 * Further we will be adding methods with different modifiers
 * 
 * @author kdvolder
 */
public class B extends A {

	public String pubEarly() {
		return "B.pubEarly()";
	}

	@SuppressWarnings("unused")
	private String privEarly() {
		return "B.privEarly()";
	}

	static String staticEarly() {
		return "B.staticEarly()";
	}

	public String pubDeleted() {
		return "B.pubDeleted()";
	}

	@SuppressWarnings("unused")
	private String privDeleted() {
		return "B.privDeleted()";
	}

	static String staticDeleted() {
		return "B.staticDeleted()";
	}

}
