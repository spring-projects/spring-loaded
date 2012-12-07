package data;

/**
 * Simple class that runs a method.  Can we update the method without restarting the JVM.
 */
public class Simple {

	public static void main(String[] argv) {
		Simple s = new Simple();
		s.run();
	}
	 
	public void run() {
		System.out.println("Hello World3");
	}

	public static void runStatic() {
		System.out.println("Hello World from static2");
	}
	
	public void newmethod() {
		System.out.println();
	}

	
}
