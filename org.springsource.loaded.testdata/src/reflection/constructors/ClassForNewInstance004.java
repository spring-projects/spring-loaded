package reflection.constructors;

/**
 * This version (004) is only used for testing "Class.newInstance". Testing case where there is
 * no default constructor
 */
public class ClassForNewInstance004 {
	
	public ClassForNewInstance004(String noDefaultConstructor) {
		System.out.println("004 blah");
	}
	
	
	@Override
	public String toString() {
		// The value of toString is used by the test to check expected result... so
		return "004";
	}
	
}
