package reflection.generics;

import java.util.Iterator;

public class GenericClass002<K extends Comparable<K>> implements GenericInterface<K>, Iterable<K> {
	
	//what we need in this v002 class...

	// Same as in original class, but also with methods added (fore these cases)

	public Iterator<K> iterator() {
		return null;
	}
	
	public static Iterator<String> iterateStrings(Iterator<? extends Object> objs) {
		return null;
	}
	
	public void processThem(String... strings) {
	}
	
	public static <T extends Comparable<T>> GenericClass002<T> create(T ini) {
		return null;
	}
	
    public <E extends RuntimeException> void genericThrow() throws E {}
    
    public void checkMe() throws SecurityException, NoSuchFieldException {}

	public Iterator<K> iterator2() {
		return null;
	}
	
	public static Iterator<String> iterateStrings2(Iterator<? extends Object> objs) {
		return null;
	}
	
	public void processThem2(String... strings) {
	}
	
	public static <T extends Comparable<T>> GenericClass002<T> create2(T ini) {
		return null;
	}
	
    <E extends RuntimeException> void genericThrow2() throws E {}
    
    void checkMe2() throws SecurityException, NoSuchFieldException {}
    
}
