package reflection.constructors;

import javax.crypto.IllegalBlockSizeException;

/**
 * A class with some constructors, for testing methods like Class.getConstructor, Class.getConstructors etc.
 * <p>
 * We need a few variations in this class, some different parameter lists and different visibility modifiers on
 * these constructors. 
 * 
 * @author kdvolder
 */
public class ClassWithConstructors {

	//////////////////////////////////////////////////
	// Constructors that will not be changed (one with each kind of scope)
	
	private ClassWithConstructors() {
	}
	
	protected ClassWithConstructors(int x) {
		this();
	}
	
	public ClassWithConstructors(boolean z) {
	}

	ClassWithConstructors(double z) {
	}
	
	/////////////////////////////////////////////////////////////////////////
	// Some constructors that change in different ways in the reloaded class

	// modifier will change
	public ClassWithConstructors(int i, String s) {
	}

	// will be deleted
	public ClassWithConstructors(boolean i, String s) {
	}
	
	// will get exceptions
	public ClassWithConstructors(String i, String s) {
	}
	
	// will remove exceptions
	public ClassWithConstructors(double i, String s) throws IllegalBlockSizeException {
	}
	
}
