package system;

import java.lang.reflect.Method;

/*
 * Method.invoke(instance, arguments)
 * 
 * This test class represents a class in the system set for the VM. These classes cannot have their reflective calls directly
 * intercepted because we cannot introduce dependencies on types in a lower classloader, so we have to call the reflective
 * interceptor reflectively!
 */
public class Eleven {
	
	public String runIt() throws Exception {
		StringBuilder data = new StringBuilder();
		Object obj = invoke(new Eleven(),12,"abc");
		data.append("obj="+obj);
		return "complete:" + data.toString().trim();
	}

	public static Object invoke(Object instance, Object... args) throws Exception {
		Method m = Eleven.class.getDeclaredMethod("foo", Integer.TYPE,String.class);
		return m.invoke(instance,args);
	}
	
	public String foo(int i, String s) {
		return "i="+i+":s="+s;
	}
}
