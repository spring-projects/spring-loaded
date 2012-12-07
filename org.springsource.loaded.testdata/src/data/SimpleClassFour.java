package data;

class SimpleClassFour {
	{
		System.out.println("clinit!");
	}

	public int boo;

	public static String s;

	public SimpleClassFour(int i) {
	}

	public SimpleClassFour(String d) {
	}

	void boo() {
	}

	@SuppressWarnings("unused")
	private static void foo() {
	}

	public String goo(int i, double d, String p) {
		return p;
	}

	public static int hoo(long l) {
		return new Long(l).intValue();
	}

	public static void woo() throws RuntimeException, IllegalStateException {
	}
}