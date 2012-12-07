package data;

public class Orange002 {

	Apple002 apple = new Apple002();

	public String callApple1(String a, Integer b, String c, Integer d) {
		return apple.run(a, b, c, d);
	}

	public String callApple2(int i) {
		return apple.run2(i);
	}

	public Integer callApple3(int i) {
		return apple.run3(i);
	}

	public String callApple3x(String s, int i, double d, String t, int[] is) {
		return apple.run(s, i, d, t, is);
	}

	public static Integer callApple4(int i) {
		return Apple002.run4(i);
	}

	public Float callAppleRetFloat(float i) {
		return apple.runGetFloat(i);
	}

	public Boolean callAppleRetBoolean(boolean i) {
		return apple.runGetBoolean(i);
	}

	public Short callAppleRetShort(short i) {
		return apple.runGetShort(i);
	}

	public Long callAppleRetLong(long i) {
		return apple.runGetLong(i);
	}

	public Double callAppleRetDouble(double i) {
		return apple.runGetDouble(i);
	}

	public Character callAppleRetChar(char i) {
		return apple.runGetChar(i);
	}

	public Byte callAppleRetByte(byte b) {
		return apple.runGetByte(b);
	}

	public int[] callAppleRetArrayInt(int[] b) {
		return apple.runGetArrayInt(b);
	}

	public String[] callAppleRetArrayString(String[] b) {
		return apple.runGetArrayString(b);
	}

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
