package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLRFGetTheRest extends FormattingHelper {

	public boolean z;
	public byte b;
	public char c;
	public double d;
	public float f;
	public int i;
	public long j;
	public short s;

	public String run() throws Exception {
		z = true;
		b = (byte) 123;
		c = 'a';
		d = 3.141d;
		f = 33f;
		i = 12345;
		j = 444L;
		s = (short) 99;
		StringBuilder s = new StringBuilder();
		s.append(getBoolean()).append(" ");
		s.append(getByte()).append(" ");
		s.append(getChar()).append(" ");
		s.append(getDouble()).append(" ");
		s.append(getFloat()).append(" ");
		s.append(getInt()).append(" ");
		s.append(getLong()).append(" ");
		s.append(getShort()).append(" ");
		return s.toString().trim();
	}

	public String getBoolean() throws Exception {
		Method m = Field.class.getMethod("getBoolean", Object.class);
		Field f = JLRFGetTheRest.class.getDeclaredField("z");
		return ((Boolean) m.invoke(f, this)).toString();
	}

	public String getByte() throws Exception {
		Method m = Field.class.getMethod("getByte", Object.class);
		Field f = JLRFGetTheRest.class.getDeclaredField("b");
		return ((Byte) m.invoke(f, this)).toString();
	}

	public String getChar() throws Exception {
		Method m = Field.class.getMethod("getChar", Object.class);
		Field f = JLRFGetTheRest.class.getDeclaredField("c");
		m.invoke(f, this);
		return Character.toString(c);
	}

	public String getDouble() throws Exception {
		Method m = Field.class.getMethod("getDouble", Object.class);
		Field f = JLRFGetTheRest.class.getDeclaredField("d");
		return ((Double) m.invoke(f, this)).toString();
	}

	public String getFloat() throws Exception {
		Method m = Field.class.getMethod("getFloat", Object.class);
		Field f = JLRFGetTheRest.class.getDeclaredField("f");
		return ((Float) m.invoke(f, this)).toString();
	}

	public String getInt() throws Exception {
		Method m = Field.class.getMethod("getInt", Object.class);
		Field f = JLRFGetTheRest.class.getDeclaredField("i");
		return ((Integer) m.invoke(f, this)).toString();
	}

	public String getLong() throws Exception {
		Method m = Field.class.getMethod("getLong", Object.class);
		Field f = JLRFGetTheRest.class.getDeclaredField("j");
		return ((Long) m.invoke(f, this)).toString();
	}

	public String getShort() throws Exception {
		Method m = Field.class.getMethod("getShort", Object.class);
		Field f = JLRFGetTheRest.class.getDeclaredField("s");
		return ((Short) m.invoke(f, this)).toString();
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFGetTheRest().run());
	}

}
