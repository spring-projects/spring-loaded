package reflection;

/**
 * A class for testing, used in conjuntion with "SubTestVal", for tests requring
 * instances of sub/supertypes (e.g. when testing the type checking contraints 
 * that should be imposed by reflective field set operations.
 * 
 * @author kdvolder
 */
public class TestVal {
	
	public static final TestVal it = new TestVal();
	
	@Override
	public String toString() {
		return "TestVal";
	}

}
