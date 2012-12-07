package data;

public class HelloWorldPrimitive002 {

	public int getValue() {
		return 37;
	}

	public boolean getValueBoolean() {
		return false;
	}

	public int getValueWithParams(String a, String b) {
		return Integer.valueOf(a) + Integer.valueOf(b);
	}

	public short getValueShort() {
		return 6;
	}

	public float getValueFloat() {
		return 6.0f;
	}

	public double getValueDouble() {
		return 6.0d;
	}

	public long getValueLong() {
		return 6L;
	}

	public byte getValueByte() {
		return 6;
	}

	public char getValueChar() {
		return 'f';
	}

	public int[] getArrayInt() {
		return new int[] { 5 };
	}

	public String[] getArrayString() {
		return new String[] { "DEF" };
	}
}