package ctors;

public class JR {
	
	public JR(int i) {
		
	}
	
	public static String printMessage() {
		return "hello";
	}
	
	public static JR getInstance() {
		return new JR(42);
	}

	public Object getFieldReflectively() throws Exception {
		return null;
	}

	public void setFieldReflectively(int value) throws Exception {
	}
}
