package proxy.three;

public class TestA2 {

	static TestIntfaceA2 instanceA;
	static TestIntfaceB2 instanceB;

	public static void createProxy() {
		Object o = TestInvocationHandlerA1.newInstance(TestIntfaceA2.class, TestIntfaceB2.class);
		System.out.println("first interface is " + o.getClass().getInterfaces()[0]);
		System.out.println("second interface is " + o.getClass().getInterfaces()[1]);
		instanceB = (TestIntfaceB2) o;
		instanceA = (TestIntfaceA2) o;
	}

	public static void runMA() {
		System.out.println("instance type is " + instanceA.getClass());
		instanceA.ma();
	}

	public static void runNA() {
		System.out.println("instance type is " + instanceA.getClass());
		instanceA.na();
	}

	public static void runMB() {
		System.out.println("instance type is " + instanceB.getClass());
		instanceB.mb();
	}

	public static void runNB() {
		if (instanceB == null) {
			throw new IllegalStateException();
		}
		System.out.println("instance type is " + instanceB.getClass());
		instanceB.nb();
	}
}
