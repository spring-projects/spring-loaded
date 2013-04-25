package invokestatic.issue4;

public class B extends A {
	
	public static String getMessage() {
		return getString();
	}

}
