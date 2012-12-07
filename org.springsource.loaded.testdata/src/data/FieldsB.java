package data;

public class FieldsB {

	int[] is = new int[] { 1, 2, 3 };
	int i = 23;
	boolean b = false;
	char c = 'a';
	short s = 123;
	long l = 32768 * 327;
	double d = 2.0d;
	float f = 1.4f;
	String theMessage = "Hello Andy";

	public boolean isB() {
		return b;
	}

	public void setB(boolean b) {
		this.b = b;
	}

	public int[] getIs() {
		return is;
	}

	public void setIs(int[] newis) {
		is = newis;
	}

	public char getC() {
		return c;
	}

	public void setC(char c) {
		this.c = c;
	}

	public short getS() {
		return s;
	}

	public void setS(short s) {
		this.s = s;
	}

	public long getL() {
		return l;
	}

	public void setL(long l) {
		this.l = l;
	}

	public double getD() {
		return d;
	}

	public void setD(double d) {
		this.d = d;
	}

	public float getF() {
		return f;
	}

	public void setF(float f) {
		this.f = f;
	}

	public String getTheMessage() {
		return theMessage;
	}

	public void setI(int i) {
		this.i = i;
	}

	public int getI() {
		return i;
	}

	public void setTheMessage(String newValue) {
		theMessage = newValue;
	}

}