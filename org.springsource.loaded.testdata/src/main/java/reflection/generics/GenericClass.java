package reflection.generics;

import java.util.Iterator;

public class GenericClass<K extends Comparable<K>> implements GenericInterface<K>, Iterable<K> {
	
	//what we need in this class...
	
	// Following cases:
	//   - generic return type
	//   - generic method (i.e. with a generic parameter different from the class's type parameters
	//   - generically typed parameters
	//   - generically typed exception(s)
	//   - varargs method
	
	// static and non-static versions of most cases

	/**
	 * Method with Generic return type
	 */
	public Iterator<K> iterator() {
		return null;
	}
	
	/**
	 * Static method with Generic return type
	 */
	public static Iterator<String> iterateStrings(Iterator<? extends Object> objs) {
		return null;
	}
	
	public void processThem(String... strings) {
	}
	
	/**
	 * Generic method
	 */
	public static <T extends Comparable<T>> GenericClass<T> create(T ini) {
		return null;
	}
	
    public <E extends RuntimeException> void genericThrow() throws E {}
    
    public void checkMe() throws SecurityException, NoSuchFieldException {}
	
}
