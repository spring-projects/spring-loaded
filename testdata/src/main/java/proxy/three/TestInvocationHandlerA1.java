package proxy.three;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class TestInvocationHandlerA1 implements InvocationHandler {
	//		Object obj;

	public TestInvocationHandlerA1() {
		//			this.obj = obj;
	}

	public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
		System.out.println("TestInvocationHandler1.invoke() for " + m.getName());
		return null;
		//			return m.invoke(obj, args);
		//			try {
		//				System.out.println("before");
		//			} catch (Exception e) {
		//				e.printStackTrace();
		//			}
		//			return null;
	}

	static public Object newInstance(Class<?>... interfaces) {
		return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, new TestInvocationHandlerA1());
	}
}
