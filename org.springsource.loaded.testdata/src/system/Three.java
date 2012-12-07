package system;

import java.lang.reflect.Field;

/**
 * This test class represents a class in the system set for the VM. These classes cannot have their reflective calls directly
 * intercepted because we cannot introduce dependencies on types in a lower classloader, so we have to call the reflective
 * interceptor reflectively!
 */
public class Three {

	public String s;

	public String runIt() throws Exception {
		StringBuilder data = new StringBuilder();
		Field field = f("s");
		data.append("field?" + field + " ");
		try {
			f("foo");
			data.append("unexpectedly_didn't_fail");
		} catch (NoSuchFieldException nsfe) {
			data.append("nsfe");
		}
		return "complete:" + data.toString().trim();
	}

	public Field f(String name) throws NoSuchFieldException {
		return this.getClass().getField(name);
	}
}
