package system;

import java.lang.reflect.Field;

/*
 * Field.get()
 * 
 * This test class represents a class in the system set for the VM. These classes cannot have their reflective calls directly
 * intercepted because we cannot introduce dependencies on types in a lower classloader, so we have to call the reflective
 * interceptor reflectively!
 */
public class Twelve {
	
	public String foo = "abc";

	public String runIt() throws Exception {
		StringBuilder data = new StringBuilder();
		Object value  = gf();
		data.append("value="+value);
		return "complete:" + data.toString().trim();
	}

	public Object gf() throws Exception {
		Field f = Twelve.class.getField("foo");
		return f.get(this);
	}
}
