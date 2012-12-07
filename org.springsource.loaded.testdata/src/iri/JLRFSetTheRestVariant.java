package iri;

import java.lang.reflect.Field;

/**
 * This variant is using set() for primitive fields but passing in wrapper values.
 * 
 * @author Andy Clement
 * @since 1.0.4
 */
public class JLRFSetTheRestVariant extends FormattingHelper {

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
		Field f = JLRFSetTheRestVariant.class.getDeclaredField("z");
		f.set(this, Boolean.TRUE);
		return Boolean.toString(z);
	}

	public String setByte() throws Exception {
		Field f = JLRFSetTheRestVariant.class.getDeclaredField("b");
		f.set(this, new Byte("123"));
		return Byte.toString(b);
	}

	public String setChar() throws Exception {
		Field f = JLRFSetTheRestVariant.class.getDeclaredField("c");
		f.set(this, Character.valueOf('a'));
		return Character.toString(c);
	}

	public String setDouble() throws Exception {
		Field f = JLRFSetTheRestVariant.class.getDeclaredField("d");
		f.set(this, Double.valueOf(3.14d));
		return Double.toString(d);
	}

	public String setFloat() throws Exception {
		Field f = JLRFSetTheRestVariant.class.getDeclaredField("f");
		f.set(this, Float.valueOf(6.5f));
		return Float.toString(this.f);
	}

	public String setInt() throws Exception {
		Field f = JLRFSetTheRestVariant.class.getDeclaredField("i");
		f.set(this, Integer.valueOf(32767));
		return Integer.toString(i);
	}

	public String setLong() throws Exception {
		Field f = JLRFSetTheRestVariant.class.getDeclaredField("j");
		f.set(this, Long.valueOf(555L));
		return Long.toString(j);
	}

	public String setShort() throws Exception {
		Field f = JLRFSetTheRestVariant.class.getDeclaredField("s");
		f.set(this, Short.valueOf((short) 333));
		return Short.toString(s);
	}

	public static void main(String[] argv) throws Exception {
		System.out.println(new JLRFSetTheRestVariant().run());
	}

}
