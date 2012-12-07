package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLRFSetTheRest extends FormattingHelper {

	public boolean z;
	public byte b;
	public char c;
	public double d;
	public float f;
	public int i;
	public long j;
	public short s;

	public String run() throws Exception {
		StringBuilder s = new StringBuilder();
		s.append(setBoolean()).append(" ");
		s.append(setByte()).append(" ");
		s.append(setChar()).append(" ");
		s.append(setDouble()).append(" ");
		s.append(setFloat()).append(" ");
		s.append(setInt()).append(" ");
		s.append(setLong()).append(" ");
		s.append(setShort()).append(" ");
		return s.toString().trim();
	}

	public String setBoolean() throws Exception {
		Method m = Field.class.getMethod("setBoolean", Object.class, Boolean.TYPE);
		Field f = JLRFSetTheRest.class.getDeclaredField("z");
		m.invoke(f, this, true);
		return Boolean.toString(z);
	}

	public String setByte() throws Exception {
		Method m = Field.class.getMethod("setByte", Object.class, Byte.TYPE);
		Field f = JLRFSetTheRest.class.getDeclaredField("b");
		m.invoke(f, this, (byte) 123);
		return Byte.toString(b);
	}

	public String setChar() throws Exception {
		Method m = Field.class.getMethod("setChar", Object.class, Character.TYPE);
		Field f = JLRFSetTheRest.class.getDeclaredField("c");
		m.invoke(f, this, 'a');
		return Character.toString(c);
	}

	public String setDouble() throws Exception {
		Method m = Field.class.getMethod("setDouble", Object.class, Double.TYPE);
		Field f = JLRFSetTheRest.class.getDeclaredField("d");
		m.invoke(f, this, 3.14d);
		return Double.toString(d);
	}

	public String setFloat() throws Exception {
		Method m = Field.class.getMethod("setFloat", Object.class, Float.TYPE);
		Field f = JLRFSetTheRest.class.getDeclaredField("f");
		m.invoke(f, this, 6.5f);
		return Float.toString(this.f);
	}

	public String setInt() throws Exception {
		Method m = Field.class.getMethod("setInt", Object.class, Integer.TYPE);
		Field f = JLRFSetTheRest.class.getDeclaredField("i");
		m.invoke(f, this, 32767);
		return Integer.toString(i);
	}

	public String setLong() throws Exception {
		Method m = Field.class.getMethod("setLong", Object.class, Long.TYPE);
		Field f = JLRFSetTheRest.class.getDeclaredField("j");
		m.invoke(f, this, 555L);
		return Long.toString(j);
	}

	public String setShort() throws Exception {
		Method m = Field.class.getMethod("setShort", Object.class, Short.TYPE);
		Field f = JLRFSetTheRest.class.getDeclaredField("s");
		m.invoke(f, this, (short) 333);
		return Short.toString(s);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFSetTheRest().run());
	}

}
