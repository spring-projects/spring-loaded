package proxy.two;

public class TestA2 {

	static TestIntfaceA2 instance;

	public static void createProxy() {
		Object o = TestInvocationHandlerA1.newInstance(TestIntfaceA2.class);
		System.out.println("first interface is " + o.getClass().getInterfaces()[0]);
		instance = (TestIntfaceA2) o;
	}

	public static void runM() {
		System.out.println("instance type is " + instance.getClass());
		instance.m();
	}

	public static void runN() {
		System.out.println("instance type is " + instance.getClass());
		instance.n();
	}

	public static void main(String[] argv) {
		createProxy();
		runM();
	}
}
