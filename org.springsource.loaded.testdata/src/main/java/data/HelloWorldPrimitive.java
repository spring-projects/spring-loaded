package data;

public class HelloWorldPrimitive {

	public int getValue() {
		return 42;
	}

	public int getValueWithParams(String a, String b) {
		return Integer.valueOf(a) + Integer.valueOf(b);
	}

	public float getValueFloat() {
		return 3.0f;
	}

	public boolean getValueBoolean() {
		return true;
	}

	public short getValueShort() {
		return 3;
	}

	public long getValueLong() {
		return 3L;
	}

	public double getValueDouble() {
		return 3.0d;
	}

	public char getValueChar() {
		return 'c';
	}

	public byte getValueByte() {
		return 3;
	}

	public int[] getArrayInt() {
		return new int[] { 3 };
	}

	public String[] getArrayString() {
		return new String[] { "ABC" };
	}

}