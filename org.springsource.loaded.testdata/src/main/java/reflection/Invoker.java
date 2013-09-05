package reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class Invoker {

	static Field f_i; // int
	static Field f_z; // boolean
	static Field f_zarray; // boolean[]
	static Field f_is; // static int
	static Field f_b; // byte
	static Field f_c; // char
	static Field f_s; // short
	static Field f_j; // long
	static Field f_f; // float
	static Field f_d; // double
	static Field f_l; // reference (String)
	static Field f_annotated; // annotated field
	static Target t = new Target();

	{
		try {
			f_i = Target.class.getDeclaredField("i");
			f_z = Target.class.getDeclaredField("z");
			f_zarray = Target.class.getDeclaredField("zs");
			f_is = Target.class.getDeclaredField("is");
			f_b = Target.class.getDeclaredField("b");
			f_c = Target.class.getDeclaredField("c");
			f_s = Target.class.getDeclaredField("s");
			f_j = Target.class.getDeclaredField("j");
			f_f = Target.class.getDeclaredField("f");
			f_d = Target.class.getDeclaredField("d");
			f_l = Target.class.getDeclaredField("l");
			f_annotated = Target.class.getDeclaredField("annotated");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// int
	public void setI() throws Exception {
		f_i.set(t, 42);
	}

	public void setIntI() throws Exception {
		f_i.setInt(t, 45);
	}

	public int getI() {
		return t.i;
	}

	public int getReflectI() throws Exception {
		return f_i.getInt(t);
	}

	public int getReflectObjectI() throws Exception {
		return (Integer) f_i.get(t);
	}

	// --- boolean
	public void setZ() throws Exception {
		f_z.setAccessible(true);
		f_z.set(t, true);
	}

	public void setIntZ() throws Exception {
		f_z.setAccessible(true);
		f_z.setBoolean(t, false);
	}

	public boolean getZ() {
		return t.z;
	}

	public boolean getReflectZ() throws Exception {
		return f_z.getBoolean(t);
	}

	public boolean getReflectObjectZ() throws Exception {
		return (Boolean) f_z.get(t);
	}

	// --- byte
	public void setB() throws Exception {
		f_b.setAccessible(true);
		f_b.set(t, (byte) 65);
	}

	public void setIllegalB() throws Exception {
		f_b.setAccessible(true);
		f_b.set(t, 32); // cannot supply int
	}

	public void setByteB() throws Exception {
		f_b.setAccessible(true);
		f_b.setByte(t, (byte) 70);
	}

	public byte getB() {
		return t.b;
	}

	public byte getReflectB() throws Exception {
		return f_b.getByte(t);
	}

	public byte getReflectObjectB() throws Exception {
		return (Byte) f_b.get(t);
	}

	// --- char
	public void setC() throws Exception {
		f_c.setAccessible(true);
		f_c.set(t, (char) 66);
	}

	public void setIllegalC() throws Exception {
		f_c.setAccessible(true);
		f_c.set(t, 32); // cannot supply int
	}

	public void setCharC() throws Exception {
		f_c.setAccessible(true);
		f_c.setChar(t, (char) 77);
	}

	public char getC() {
		return t.c;
	}

	public char getReflectC() throws Exception {
		return f_c.getChar(t);
	}

	public char getReflectObjectC() throws Exception {
		return (Character) f_c.get(t);
	}

	// --- short
	public void setS() throws Exception {
		f_s.setAccessible(true);
		f_s.set(t, (short) 660);
	}

	public void setIllegalS() throws Exception {
		f_s.setAccessible(true);
		f_s.set(t, 32); // cannot supply int
	}

	public void setShortS() throws Exception {
		f_s.setAccessible(true);
		f_s.setShort(t, (short) 77);
	}

	public short getS() {
		return t.s;
	}

	public short getReflectS() throws Exception {
		return f_s.getShort(t);
	}

	public short getReflectObjectS() throws Exception {
		return (Short) f_s.get(t);
	}

	// --- long
	public void setJ() throws Exception {
		f_j.setAccessible(true);
		f_j.set(t, (long) 660);
	}

	public void setIllegalJ() throws Exception {
		f_j.setAccessible(true);
		f_j.set(t, 32); // cannot supply int
	}

	public void setLongJ() throws Exception {
		f_j.setAccessible(true);
		f_j.setLong(t, (long) 77);
	}

	public long getJ() {
		return t.j;
	}

	public long getReflectJ() throws Exception {
		return f_j.getLong(t);
	}

	public long getReflectObjectJ() throws Exception {
		return (Long) f_j.get(t);
	}

	// --- float
	public void setF() throws Exception {
		f_f.setAccessible(true);
		f_f.set(t, (float) 660);
	}

	public void setIllegalF() throws Exception {
		f_f.setAccessible(true);
		f_f.set(t, 32); // cannot supply int
	}

	public void setFloatF() throws Exception {
		f_f.setAccessible(true);
		f_f.setFloat(t, (float) 77);
	}

	public float getF() {
		return t.f;
	}

	public float getReflectF() throws Exception {
		return f_f.getFloat(t);
	}

	public float getReflectObjectF() throws Exception {
		return (Float) f_f.get(t);
	}

	// --- static int field
	public void setIS() throws Exception {
		f_is.setAccessible(true);
		f_is.set(t, (int) 660);
	}

	public void setIllegalIS() throws Exception {
		f_is.setAccessible(true);
		f_is.set(t, "abc"); // cannot supply int
	}

	public void setintIS() throws Exception {
		f_is.setAccessible(true);
		f_is.setInt(t, (int) 77);
	}

	public int getIS() {
		return Target.is;
	}

	public Integer getISInteger() {
		return Target.is;
	}

	public Integer getReflectIS() throws Exception {
		return f_is.getInt(t);
	}

	public Integer getReflectObjectIS() throws Exception {
		return (Integer) f_is.get(t);
	}

	// --- double
	public void setD() throws Exception {
		f_d.setAccessible(true);
		f_d.set(t, (double) 660);
	}

	public void setIllegalD() throws Exception {
		f_d.setAccessible(true);
		f_d.set(t, 32); // cannot supply int
	}

	public void setDoubleD() throws Exception {
		f_d.setAccessible(true);
		f_d.setDouble(t, (double) 77);
	}

	public double getD() {
		return t.d;
	}

	public double getReflectD() throws Exception {
		return f_d.getDouble(t);
	}

	public double getReflectObjectD() throws Exception {
		return (Double) f_d.get(t);
	}

	// --- boolean array
	public void setZArray() throws Exception {
		f_zarray.setAccessible(true);
		boolean[] bs = new boolean[] { true, false, true };
		f_zarray.set(t, bs);
	}

	public void setIllegalZArray() throws Exception {
		f_zarray.setAccessible(true);
		f_zarray.set(t, 32); // cannot supply int
	}

	public boolean[] getZArray() {
		return t.zs;
	}

	public boolean[] getReflectObjectZArray() throws Exception {
		return (boolean[]) f_zarray.get(t);
	}

	// --- reference
	public void setReference() throws Exception {
		f_l.setAccessible(true);
		f_l.set(t, "abcde");
	}

	public void setIllegalReference() throws Exception {
		f_l.setAccessible(true);
		f_l.set(t, 32); // cannot supply int
	}

	public String getReference() {
		return t.l;
	}

	public String getReflectObjectReference() throws Exception {
		return (String) f_l.get(t);
	}

	// ---

	public Annotation getAnnotation(Class<? extends Annotation> clazz) {
		return (Annotation) f_annotated.getAnnotation(clazz);
	}

	public Annotation[] getDeclaredAnnotations() {
		return (Annotation[]) f_annotated.getDeclaredAnnotations();
	}

}
