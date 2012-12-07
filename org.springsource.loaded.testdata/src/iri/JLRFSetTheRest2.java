package iri;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JLRFSetTheRest2 extends FormattingHelper {

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
		Field f = JLRFSetTheRest2.class.getDeclaredField("z2");
		m.invoke(f, this, true);
		return Boolean.toString(z2);
	}

	public String setByte() throws Exception {
		Method m = Field.class.getMethod("setByte", Object.class, Byte.TYPE);
		Field f = JLRFSetTheRest2.class.getDeclaredField("b2");
		m.invoke(f, this, (byte) 111);
		return Byte.toString(b2);
	}

	public String setChar() throws Exception {
		Method m = Field.class.getMethod("setChar", Object.class, Character.TYPE);
		Field f = JLRFSetTheRest2.class.getDeclaredField("c2");
		m.invoke(f, this, 'b');
		return Character.toString(c2);
	}

	public String setDouble() throws Exception {
		Method m = Field.class.getMethod("setDouble", Object.class, Double.TYPE);
		Field f = JLRFSetTheRest2.class.getDeclaredField("d2");
		m.invoke(f, this, 6.28d);
		return Double.toString(d2);
	}

	public String setFloat() throws Exception {
		Method m = Field.class.getMethod("setFloat", Object.class, Float.TYPE);
		Field f = JLRFSetTheRest2.class.getDeclaredField("f2");
		m.invoke(f, this, 13.0f);
		return Float.toString(this.f2);
	}

	public String setInt() throws Exception {
		Method m = Field.class.getMethod("setInt", Object.class, Integer.TYPE);
		Field f = JLRFSetTheRest2.class.getDeclaredField("i2");
		m.invoke(f, this, 11122);
		return Integer.toString(i2);
	}

	public String setLong() throws Exception {
		Method m = Field.class.getMethod("setLong", Object.class, Long.TYPE);
		Field f = JLRFSetTheRest2.class.getDeclaredField("j2");
		m.invoke(f, this, 222L);
		return Long.toString(j2);
	}

	public String setShort() throws Exception {
		Method m = Field.class.getMethod("setShort", Object.class, Short.TYPE);
		Field f = JLRFSetTheRest2.class.getDeclaredField("s2");
		m.invoke(f, this, (short) 777);
		return Short.toString(s2);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFSetTheRest2().run());
	}

}
