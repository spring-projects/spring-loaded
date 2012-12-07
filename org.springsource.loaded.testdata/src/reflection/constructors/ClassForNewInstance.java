package reflection.constructors;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Test class with constructors, for the purpose of testing constructor invocation.
 * <p>
 * Constructors in this class have some behavior (which is changed in a reloaded class), so that we can see when they are
 * being called.
 * 
 * @author kdvolder
 */
public class ClassForNewInstance {
	
	private boolean b;
	private String s;
	private int i;
	private double d;

	public ClassForNewInstance() {
		System.out.println("no args");
	}
	
	ClassForNewInstance(String x) {
		this.s = x;
		System.out.println("string "+x);
	}
	
	protected ClassForNewInstance(int x) {
		this.i = x;
		System.out.println("int "+x);
	}

	@SuppressWarnings("unused")
	private ClassForNewInstance(boolean x) {
		this.b = x;
		System.out.println("bool "+x);
	}

	//Becomes public
	@SuppressWarnings("unused")
	private ClassForNewInstance(int x, String y) {
		this.i = x; this.s = y;
		System.out.println("int String "+x+" "+y);
	}
	
	//Becomes private
	public ClassForNewInstance(double x) {
		this.d = x;
		System.out.println("double "+x);
	}
	
	// Will be deleted
	public ClassForNewInstance(char c, char d) {
		s = c+","+d;
	}
	
	@Override
	public String toString() {
		// The value of toString is used by the test to check expected result... so
		return "001{ "+b+", "+s+","+i+","+d+"}";
	}
	
	/// We also use this class itself as an "invoker" for testing, so that we have some cases where it *is* allowed
	// to call private methods etc. without setAccessible!
	
    public static Object callNewInstance(Constructor<?> thiz, Object[] a0)
    throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        return thiz.newInstance(a0);
    }
	
	
}
