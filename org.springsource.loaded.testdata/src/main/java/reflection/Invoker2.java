package reflection;

import java.lang.reflect.Field;

public class Invoker2 {

	static Field f_zarray; // boolean[]
	static Field f_f; // float
	static Field f_d; // double
	static Field f_l; // reference (String)
	static Field f_annotated; // annotated field
	static Target2 t = new Target2();

	{
		try {
			f_zarray = Target2.class.getDeclaredField("zs");
			f_f = Target2.class.getDeclaredField("f");
			f_d = Target2.class.getDeclaredField("d");
			f_l = Target2.class.getDeclaredField("l");
			f_annotated = Target2.class.getDeclaredField("annotated");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String setString() throws Exception {
		f_l.setAccessible(true);
		f_l.set(t, "wibble");
		return (String) f_l.get(t);
	}

}
