package proxy.two;

public class TestA1 {

	static TestIntfaceA1 instance;

	public static void createProxy() {
		Object o = TestInvocationHandlerA1.newInstance(TestIntfaceA1.class);
		System.out.println("first interface is " + o.getClass().getInterfaces()[0]);
		instance = (TestIntfaceA1) o;

	}

	public static void runM() {
		System.out.println("instance type is " + instance.getClass());
		instance.m();
	}

	public static void runN() {
		// filled in later
	}

	public static void main(String[] argv) {
		createProxy();
		runM();
	}
}
