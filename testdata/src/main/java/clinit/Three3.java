package clinit;

public class Three3 {

	static int i;
	{
		i = 4;
	}

	public static String run() {
		return Integer.toString(i);
	}
}
