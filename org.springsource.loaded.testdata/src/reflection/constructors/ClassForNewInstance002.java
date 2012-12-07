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
public class ClassForNewInstance002 {
	
	private boolean b;
	private String s;
	private int i;
	private double d;
	
	public ClassForNewInstance002() {
		System.out.println("002 no args");
	}
	
	ClassForNewInstance002(String x) {
		this.s = "002"+x;
		System.out.println("002 string "+x);
	}
	
	protected ClassForNewInstance002(int x) {
		this.i = x+200;
		System.out.println("002 int "+x);
	}

	@SuppressWarnings("unused")
	private ClassForNewInstance002(boolean x) {
		this.b = !x;
		System.out.println("002 bool "+x);
	}
	
	//Becomes public
	public ClassForNewInstance002(int x, String y) {
		this.i = 20+x; this.s = "222"+y;
		System.out.println("002 int String "+x+" "+y);
	}
	
	//Becomes private
	@SuppressWarnings("unused")
	private ClassForNewInstance002(double x) {
		System.out.println("002 double "+x);
	}
	
	//New public one
	public ClassForNewInstance002(float x) {
		System.out.println("002 float "+x);
	}
	
	//New private one
	@SuppressWarnings("unused")
	private ClassForNewInstance002(char x) {
		System.out.println("002 char "+x);
	}
	
	@Override
	public String toString() {
		// The value of toString is used by the test to check expected result... so
		return "002{ "+b+", "+s+","+i+","+d+"}";
	}

	/// We also use this class itself as an "invoker" for testing, so that we have some cases where it *is* allowed
	// to call private methods etc. without setAccessible!
	
    public static Object callNewInstance(Constructor<?> thiz, Object[] a0)
    throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        return thiz.newInstance(a0);
    }
	
}
