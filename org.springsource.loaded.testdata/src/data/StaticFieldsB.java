package data;

public class StaticFieldsB {

	static int i = 23;
	static boolean b = false;
	static char c = 'a';
	static short s = 123;
	static long l = 32768 * 327;
	static double d = 2.0d;
	static float f = 1.4f;
	static Boolean[] bs = new Boolean[] { true, false, true };
	static String theMessage = "Hello Andy";

	public static boolean isB() {
		return b;
	}

	public Boolean[] getBs() {
		return bs;
	}

	public void setBs(Boolean[] newbs) {
		bs = newbs;
	}

	public static void setB(boolean bb) {
		b = bb;
	}

	public static char getC() {
		return c;
	}

	public static void setC(char cc) {
		c = cc;
	}

	public static short getS() {
		return s;
	}

	public static void setS(short ss) {
		s = ss;
	}

	public static long getL() {
		return l;
	}

	public static void setL(long ll) {
		l = ll;
	}

	public static double getD() {
		return d;
	}

	public static void setD(double dd) {
		d = dd;
	}

	public static float getF() {
		return f;
	}

	public static void setF(float ff) {
		f = ff;
	}

	public static String getTheMessage() {
		return theMessage;
	}

	public static void setTheMessage(String newvalue) {
		theMessage = newvalue;
	}

	public static void setI(int ii) {
		i = ii;
	}

	public static int getI() {
		return i;
	}

}