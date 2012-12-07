package reflection.targets;

/**
 * A simple class with just a few methods and a simple a version that adds another method.
 * 
 * To test the SignatureFinder utility.
 * 
 * @author kdvolder
 */
public class SimpleClass002 {
	public void method(String m) {}
	public int method() {return 0;}
	void added(SimpleClass other) {} 
}
