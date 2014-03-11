package system;

import java.lang.reflect.Constructor;

/*
 * Class.getDeclaredConstructors()
 * 
 * This test class represents a class in the system set for the VM. These classes cannot have their reflective calls directly
 * intercepted because we cannot introduce dependencies on types in a lower classloader, so we have to call the reflective
 * interceptor reflectively!
 */
@SuppressWarnings("rawtypes")
public class Ten {

	public String runIt() {
		StringBuilder data = new StringBuilder();
		Constructor[] constructors = cs();
		data.append("constructors:null?" + (constructors == null) + " ");
		if (constructors != null) {
			data.append("constructors:size=" + constructors.length + " ");
		}
		return "complete:" + data.toString().trim();
	}

	public Constructor[] cs() {
		return this.getClass().getDeclaredConstructors();
	}
}
