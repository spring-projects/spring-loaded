package accessors;

@SuppressWarnings("unused")
public class ProtectedFields {

	protected static boolean b = false;

	protected static String someStaticString = "def";

	protected int i;

	protected String someString;

	protected long lng;

	public ProtectedFields() {
		i = 23;
		someString = "abc";
		lng = 32768L;
	}

	public static String run() {
		new ProtectedFields().print();
		return "success";
	}

	public void print() {
		System.out.println(b);
		System.out.println(someStaticString);
		System.out.println(i);
		System.out.println(someString);
		System.out.println(lng);
	}
}
