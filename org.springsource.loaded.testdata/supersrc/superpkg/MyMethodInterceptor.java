package superpkg;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class MyMethodInterceptor implements MethodInterceptor {

	static List<String> interceptedMethods = new ArrayList<String>();
	public static boolean callSupers = true;

	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
		//		System.out.println("intercepted:" + method);
		interceptedMethods.add(method.toString());
		if (callSupers) {
			try {
				proxy.invokeSuper(obj, args);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return null;
	}

	public static String interceptionLog() {
		return "Interception list: " + interceptedMethods.toString();
	}

	public static void clearLog() {
		interceptedMethods.clear();
	}

	public static void setCallSupers(boolean b) {
		if (b) {
			System.out.println("interceptorConfiguration: turning on super calls");
			callSupers = true;
		} else {
			System.out.println("interceptorConfiguration: turning off super calls");
			callSupers = false;
		}
	}

}