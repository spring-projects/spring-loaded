package reflection.invocation;

/**
 * For invocation testing, we need a class hierarchy of some complexity to see if dispatching works right.
 * 
 * Will be using a 3 deep hierarchy C extends B extends C.
 * 
 * Further we will be adding methods with different modifiers
 * 
 * @author kdvolder
 */
public class C extends B {

	public String pubEarly() {
		return "C.pubEarly()";
	}

	@SuppressWarnings("unused")
	private String privEarly() {
		return "C.privEarly()";
	}

	static String staticEarly() {
		return "C.staticEarly()";
	}

	public String pubDeleted() {
		return "C.pubDeleted()";
	}

	@SuppressWarnings("unused")
	private String privDeleted() {
		return "C.privDeleted()";
	}

	static String staticDeleted() {
		return "C.staticDeleted()";
	}

}
