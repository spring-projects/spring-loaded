package reflect;

import java.lang.reflect.Field;

public class FieldAccessing2 {

	public static int i = 4;

	public int j = 5;

	Field fi = null;
	Field fj = null;

	public int geti() throws Exception {
		if (fi == null) {
			fi = FieldAccessing.class.getDeclaredField("i");
		}
		return fi.getInt(this);
	}

	public int getj() throws Exception {
		if (fj == null) {
			fj = FieldAccessing.class.getDeclaredField("j");
		}
		return fj.getInt(this);
	}
}
