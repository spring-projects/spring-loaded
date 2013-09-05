package system;

import java.lang.reflect.Field;

/**
 * This test class represents a class in the system set for the VM. These classes cannot have their reflective calls directly
 * intercepted because we cannot introduce dependencies on types in a lower classloader, so we have to call the reflective
 * interceptor reflectively!
 */
public class One {

	public String runIt() {
		StringBuilder data = new StringBuilder();
		Field[] fields = fs();
		data.append("fields:null?" + (fields == null) + " ");
		if (fields != null) {
			data.append("fields:size=" + fields.length + " ");
		}
		return "complete:" + data.toString().trim();
	}

	public Field[] fs() {
		return this.getClass().getDeclaredFields();
	}
}
