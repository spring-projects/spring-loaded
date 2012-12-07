package subpkg;

import superpkg.MyMethodInterceptor;
import superpkg.Simple;

public class ProxyTestcase {

	static Simple proxy = ProxyBuilder.createProxyFor(Simple.class, new MyMethodInterceptor());

	public static void main(String[] args) {
		run();
	}

	public static void run() {
		MyMethodInterceptor.clearLog();
		proxy.moo();
		System.out.println(MyMethodInterceptor.interceptionLog());
	}

	public static void runMoo() {
		MyMethodInterceptor.clearLog();
		proxy.moo();
		System.out.println(MyMethodInterceptor.interceptionLog());
	}

	public static void runBar() {
		MyMethodInterceptor.clearLog();
		// proxy.bar(1, "abc", 3L); active in ProxyTestcase2
		System.out.println(MyMethodInterceptor.interceptionLog());
	}

	public String getProxyLoader() {
		return proxy.getClass().getClassLoader().toString();
	}

	public String getSimpleLoader() {
		return Simple.class.getClassLoader().toString();
	}

	public static void configureTest1() {
		MyMethodInterceptor.setCallSupers(false);
	}

	public static void configureTest2() {
		MyMethodInterceptor.setCallSupers(true);
	}

}
