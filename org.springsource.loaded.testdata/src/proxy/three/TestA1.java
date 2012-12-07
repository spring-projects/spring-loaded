package proxy.three;

public class TestA1 {

	static TestIntfaceA1 instanceA;
	static TestIntfaceB1 instanceB;

	public static void createProxy() {
		Object o = TestInvocationHandlerA1.newInstance(TestIntfaceA1.class, TestIntfaceB1.class);
		System.out.println("o =" + o);
		System.out.println(o.toString());
		System.out.println("first interface is " + o.getClass().getInterfaces()[0]);
		System.out.println("second interface is " + o.getClass().getInterfaces()[1]);
		instanceA = (TestIntfaceA1) o;
		instanceB = (TestIntfaceB1) o;
		System.out.println("instanceA = " + instanceA);
		System.out.println("instanceB = " + instanceB);
	}

	public static void runMA() {
		System.out.println("instance type is " + instanceA.getClass());
		instanceA.ma();
	}

	public static void runNA() {
		// filled in later
	}

	public static void runMB() {
		System.out.println("instance type is " + instanceB.getClass());
		instanceB.mb();
	}

	public static void runNB() {
		// filled in later
	}

}
