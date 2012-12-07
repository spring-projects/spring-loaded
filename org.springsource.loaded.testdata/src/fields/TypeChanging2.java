package fields;

public class TypeChanging2 {

	Integer i = 1;
	Boolean z = true;
	Long j = 34L;
	Float f = 2.0f;
	Character c = 'a';
	Byte b = (byte) 0x255;
	Short s = (short) 32;
	Double d = 3.141d;
	Middle superinstance = new Sub();

	int wasArray;
	String[] wasNotArray;

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
