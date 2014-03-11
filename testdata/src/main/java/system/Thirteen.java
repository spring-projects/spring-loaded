package system;

import java.lang.reflect.Field;

/*
 * Field.getLong()
 * 
 * This test class represents a class in the system set for the VM. These classes cannot have their reflective calls directly
 * intercepted because we cannot introduce dependencies on types in a lower classloader, so we have to call the reflective
 * interceptor reflectively!
 */
public class Thirteen {
	
	public long foo = 42L;

	public String runIt() throws Exception {
		StringBuilder data = new StringBuilder();
		Object value  = gf();
		data.append("value="+value);
		return "complete:" + data.toString().trim();
	}

	public Long gf() throws Exception {
		Field f = Thirteen.class.getField("foo");
		return f.getLong(this);
	}
}
