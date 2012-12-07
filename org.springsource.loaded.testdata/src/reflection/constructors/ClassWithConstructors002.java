package reflection.constructors;

import java.io.IOException;

/**
 * A class with some constructors, for testing methods like Class.getConstructor, Class.getConstructors etc.
 * <p>
 * We need a few variations in this class, some different parameter lists and different visibility modifiers on
 * these constructors. 
 * 
 * @author kdvolder
 */
public class ClassWithConstructors002 {

	//////////////////////////////////////////////////
	// Constructors that will not be changed (one with each kind of scope)
	
	private ClassWithConstructors002() {
	}
	
	protected ClassWithConstructors002(int x) {
		this();
	}
	
	public ClassWithConstructors002(boolean z) {
	}

	ClassWithConstructors002(double z) {
	}
	
	/////////////////////////////////////////////////////////////////////////
	// Some constructors that change in different ways in the reloaded class

	// modifier will change
	@SuppressWarnings("unused")
	private ClassWithConstructors002(int i, String s) {
	}

	// will be deleted
//	public ClassWithConstructors002(boolean i, String s) {
//	}
	
	// will get exceptions
	public ClassWithConstructors002(String i, String s) throws IOException, InterruptedException {
	}
	
	// will remove exceptions
	public ClassWithConstructors002(double i, String s) {
	}
	
	///////////////////////////////////////////////////////////////////////////
	// Some constructors are added 
	
	@SuppressWarnings("unused")
	private ClassWithConstructors002(double x, ClassWithConstructors002 copy) {
	}

	public ClassWithConstructors002(long x, ClassWithConstructors002 copy) {
	}
	
	protected ClassWithConstructors002(short x, ClassWithConstructors002 copy) {
	}
	
	ClassWithConstructors002(char x, ClassWithConstructors002 copy) {
	}
}
