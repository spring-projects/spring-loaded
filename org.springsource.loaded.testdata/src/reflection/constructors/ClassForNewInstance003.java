package reflection.constructors;

/**
 * This version (003) is only used for testing "Class.newInstance" so we only need a default constructor
 */
public class ClassForNewInstance003 {
	
	private ClassForNewInstance003() {
		System.out.println("003 no args");
	}
	
	
	@Override
	public String toString() {
		// The value of toString is used by the test to check expected result... so
		return "003";
	}
	
}
