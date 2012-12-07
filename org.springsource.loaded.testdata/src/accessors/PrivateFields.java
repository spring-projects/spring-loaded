package accessors;

@SuppressWarnings("unused")
public class PrivateFields {

	private static boolean b = false;

	private static String someStaticString = "def";

	private int i;

	private String someString;

	private long lng;

	public PrivateFields() {
		i = 23;
		someString = "abc";
		lng = 32768L;
	}
}
