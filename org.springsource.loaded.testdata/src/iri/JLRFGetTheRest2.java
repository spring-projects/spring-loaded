package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLRFGetTheRest2 extends FormattingHelper {

	public boolean z;
	public byte b;
	public char c;
	public double d;
	public float f;
	public int i;
	public long j;
	public short s;

	public boolean z2;
	public byte b2;
	public char c2;
	public double d2;
	public float f2;
	public int i2;
	public long j2;
	public short s2;

	public String run() throws Exception {
		z2 = true;
		b2 = (byte) 23;
		c2 = 'b';
		d2 = 4.141d;
		f2 = 43f;
		i2 = 22345;
		j2 = 544L;
		s2 = (short) 999;
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
		Field f = JLRFGetTheRest2.class.getDeclaredField("z2");
		return ((Boolean) m.invoke(f, this)).toString();
	}

	public String getByte() throws Exception {
		Method m = Field.class.getMethod("getByte", Object.class);
		Field f = JLRFGetTheRest2.class.getDeclaredField("b2");
		return ((Byte) m.invoke(f, this)).toString();
	}

	public String getChar() throws Exception {
		Method m = Field.class.getMethod("getChar", Object.class);
		Field f = JLRFGetTheRest2.class.getDeclaredField("c2");
		return ((Character) m.invoke(f, this)).toString();
	}

	public String getDouble() throws Exception {
		Method m = Field.class.getMethod("getDouble", Object.class);
		Field f = JLRFGetTheRest2.class.getDeclaredField("d2");
		return ((Double) m.invoke(f, this)).toString();
	}

	public String getFloat() throws Exception {
		Method m = Field.class.getMethod("getFloat", Object.class);
		Field f = JLRFGetTheRest2.class.getDeclaredField("f2");
		return ((Float) m.invoke(f, this)).toString();
	}

	public String getInt() throws Exception {
		Method m = Field.class.getMethod("getInt", Object.class);
		Field f = JLRFGetTheRest2.class.getDeclaredField("i2");
		return ((Integer) m.invoke(f, this)).toString();
	}

	public String getLong() throws Exception {
		Method m = Field.class.getMethod("getLong", Object.class);
		Field f = JLRFGetTheRest2.class.getDeclaredField("j2");
		return ((Long) m.invoke(f, this)).toString();
	}

	public String getShort() throws Exception {
		Method m = Field.class.getMethod("getShort", Object.class);
		Field f = JLRFGetTheRest2.class.getDeclaredField("s2");
		return ((Short) m.invoke(f, this)).toString();
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFGetTheRest2().run());
	}

}
