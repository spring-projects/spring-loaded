package ctors;

import java.lang.reflect.Field;

public class JR2 {

	int field = 34;
	
	public JR2(int i) {
		
	}
	
	public JR2() {
		
	}
	
	public static String printMessage() {
		return "goodbye";
	}
	
	public static JR2 getInstance() {
		return new JR2();
	}
	
	public Object getFieldReflectively() throws Exception {
		Field f = this.getClass().getDeclaredField("field");
		return f.get(this);
	}

	public void setFieldReflectively(int value) throws Exception {
		Field f = this.getClass().getDeclaredField("field");
		f.setAccessible(true);
		f.set(this,value);
	}

}
