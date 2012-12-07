package system;

import java.lang.reflect.Method;

/**
 * This test class represents a class in the system set for the VM. These classes cannot have their reflective calls directly
 * intercepted because we cannot introduce dependencies on types in a lower classloader, so we have to call the reflective
 * interceptor reflectively!
 */
public class Five {

	String s;

	public String runIt() throws Exception {
		StringBuilder data = new StringBuilder();
		Method method = m("runIt");
		data.append("method?" + method + " ");
		try {
			m("foobar");
			data.append("unexpectedly_didn't_fail");
		} catch (NoSuchMethodException nsme) {
			data.append("nsme");
		}
		return "complete:" + data.toString().trim();
	}

	public Method m(String name) throws NoSuchMethodException {
		return this.getClass().getDeclaredMethod(name);
	}
}
