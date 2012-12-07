package fields;

public class TypeChanging {

	int i = 1;
	boolean z = true;
	long j = 34L;
	float f = 2.0f;
	char c = 'a';
	byte b = (byte) 0xff;
	short s = (short) 32;
	double d = 3.141d;
	Super superinstance = new Sub();
	int[] wasArray = new int[] { 1, 2, 3 };
	String wasNotArray = "abc";

	public Super getSuper() {
		return superinstance;
	}

	public Integer getI() {
		return i;
	}

	public Boolean getBoolean() {
		return z;
	}

	public Long getLong() {
		return j;
	}

	public Float getFloat() {
		return f;
	}

	public Character getChar() {
		return c;
	}

	public Byte getByte() {
		return b;
	}

	public Short getShort() {
		return s;
	}

	public Double getDouble() {
		return d;
	}

	public Object getWasArray() {
		return wasArray;
	}

	public Object getWasNotArray() {
		return wasNotArray;
	}
}

class Super {
	public String toString() {
		return "SuperInstance";
	}
}

class Middle extends Super {
	public String toString() {
		return "MiddleInstance";
	}
}

class Sub extends Middle {
	public String toString() {
		return "SubInstance";
	}
}
