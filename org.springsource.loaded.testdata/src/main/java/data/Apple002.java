package data;

public class Apple002 {

	public int intField;
	public static int staticIntField;

	public void run() {
		System.out.println("Apple002.run() is running ");
	}

	public String run(String a, Integer b, String c, Integer d) {
		return a + " " + b + " " + c + " " + d;
	}

	public String run2(int i) {
		return "run2 " + i;
	}

	public int run3(int i) {
		return i * 2;
	}

	public static int run4(int i) {
		return i * 2;
	}

	public Boolean runGetBoolean(boolean i) {
		return !i;
	}

	public short runGetShort(short i) {
		return (short) (i * 2);
	}

	public float runGetFloat(float i) {
		return i * 2;
	}

	public long runGetLong(long i) {
		return i * 2;
	}

	public double runGetDouble(double i) {
		return i * 2;
	}

	public char runGetChar(char i) {
		return (char) (i + 1);
	}

	public byte runGetByte(byte i) {
		return (byte) (i * 2);
	}

	public int[] runGetArrayInt(int[] is) {
		return is;
	}

	public String[] runGetArrayString(String[] is) {
		return is;
	}

	public String run(String s, int i, double d, String t, int[] is) {
		return s + i + d + t + is[0];
	}
}
