package system;

import java.lang.reflect.Method;

/**
 * This test class represents a class in the system set for the VM. These classes cannot have their reflective calls directly
 * intercepted because we cannot introduce dependencies on types in a lower classloader, so we have to call the reflective
 * interceptor reflectively!
 */
public class Nine {

	public String runIt() {
		StringBuilder data = new StringBuilder();
		Method[] methods = ms();
		data.append("methods:null?" + (methods == null) + " ");
		if (methods != null) {
			data.append("methods:size=" + methods.length + " ");
		}
		return "complete:" + data.toString().trim();
	}

	public Method[] ms() {
		return this.getClass().getMethods();
	}
}
