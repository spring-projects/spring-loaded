package system;

import java.lang.reflect.Constructor;

/**
 * This test class represents a class in the system set for the VM. These classes cannot have their reflective calls directly
 * intercepted because we cannot introduce dependencies on types in a lower classloader, so we have to call the reflective
 * interceptor reflectively!
 */
public class Seven {

	public Seven() {
	}

	public Seven(String s) {
	}

	public String runIt() throws Exception {
		StringBuilder data = new StringBuilder();
		Constructor<?> ctor = m();
		data.append("defaultctor?" + ctor + " ");
		ctor = m(String.class);
		data.append("stringctor?" + ctor + " ");
		try {
			m(Integer.class);
			data.append("unexpectedly_didn't_fail");
		} catch (NoSuchMethodException nsme) {
			data.append("nsme");
		}
		return "complete:" + data.toString().trim();
	}

	public Constructor<?> m(Class<?>... params) throws NoSuchMethodException {
		return this.getClass().getDeclaredConstructor(params);
	}
}
