package invokestatic.issue4;

public class BB extends AA {
	
	public static String getMessage() {
		return BB.getString();
	}

}
