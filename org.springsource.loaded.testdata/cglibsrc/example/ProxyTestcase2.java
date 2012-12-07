package example;

public class ProxyTestcase2 {

	static Simple2 proxy;// = ProxyBuilder.createProxyFor(Simple2.class);

	public static void main(String[] args) {
		run();
	}

	public static void run() {
		MyMethodInterceptor.clearLog();
		proxy.boo();
		System.out.println(MyMethodInterceptor.interceptionLog());
	}

	public static void runMoo() {
		MyMethodInterceptor.clearLog();
		proxy.moo();
		System.out.println(MyMethodInterceptor.interceptionLog());
	}

	public static void runBar() {
		MyMethodInterceptor.clearLog();
		proxy.bar(1, "abc", 3L);
		System.out.println(MyMethodInterceptor.interceptionLog());
	}

}
