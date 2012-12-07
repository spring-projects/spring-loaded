package reflect;

import java.lang.reflect.Field;

public class FieldWriting {

	public int i = 4;

	public static int j = 5;

	Field fi = null;
	Field fj = null;

	public void seti(int newi) throws Exception {
		if (fi == null) {
			fi = FieldWriting.class.getDeclaredField("i");
		}
		fi.setInt(this, newi);
	}

	public void setj(int newj) throws Exception {
		if (fj == null) {
			fj = FieldWriting.class.getDeclaredField("j");
		}
		fj.setInt(this, newj);
	}
}
