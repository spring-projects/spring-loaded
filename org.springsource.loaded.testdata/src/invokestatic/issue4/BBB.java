package invokestatic.issue4;

public class BBB extends AAA {
	
	public static String getMessage() {
		return AAA.getString();
	}

}
