package data;

public class Orange {

	Apple apple = new Apple();

	public void one() {
		apple.run(); // simple case, no parameters and void return
	}

	public void oneWithParam(String string) {
		apple.runWithParam(string);
	}

	public String oneWithReturn() {
		return apple.runWithReturn();
	}

	public int accessFieldOnApple() {
		int i = apple.intField;
		return i;
	}

	public void setFieldOnApple() {
		apple.intField = 35;
	}

	@SuppressWarnings("static-access")
	public int getStaticFieldOnApple() {
		return apple.staticIntField;
	}

	@SuppressWarnings("static-access")
	public void setStaticFieldOnApple() {
		apple.staticIntField = 35;
	}

	// public String callApple1(String a, Integer b, String c, Integer d) {
	// return apple.run(a, b, c, d);
	// }

	// public void oneCodeAfter() {
	// apple.run(); // simple case, no parameters and void return
	// int j = 3;
	// System.out.println(j);
	// }
	//	
	// public void oneCodeBefore() {
	// int k = 3;
	// System.out.println(k);
	// apple.run(); // simple case, no parameters and void return
	// }
	//	
	// public void oneCodeBeforeAndAfter() {
	// int k = 3;
	// System.out.println(k);
	// apple.run(); // simple case, no parameters and void return
	// int j = 3;
	// System.out.println(j);
	// }

	// public String oneWithReturn() {
	// return apple.runWithReturn();
	// }
}
