package remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.CRC32;

public class SerializeB {
	
	private static byte[] storedBytes;

	public static void main(String[] args) {
		run();
	}
	
	public static void run() {
//		new Serialize().run1();
		new SerializeB().run2();
	}
	
	public void writePerson() {
		PersonB p = new PersonB("Wilf","Smith");
		storedBytes = write(p);
		System.out.println("Person stored ok");
	}
	
	public PersonB readPerson() {
		PersonB person = (PersonB)read(storedBytes);
		if (!person.toString().equals("Wilf Smith")) {
			throw new IllegalStateException("Expected 'Wilf Smith' but was '"+person.toString()+"'");
		}
		System.out.println("Person read ok");
		return person;
	}
	
	public void printSecret() throws Exception {
		PersonB p = readPerson();
		Field f = p.getClass().getDeclaredField("newSecretField");
		f.setAccessible(true);
		Object value = f.get(p);
		System.out.println(value);
	}
	
	public void printInitials() throws Exception {
		PersonB person = readPerson();
		Method m = person.getClass().getDeclaredMethod("getInitials");
		m.setAccessible(true);
		String value = (String)m.invoke(person);
		System.out.println(value);
	}
	
	public void run1() {
		String s = "abc";
		byte[] bs = write(s);
		String s2 = (String)read(bs);
		check(s,s2);
	}

	public void run2() {
		PersonB p = new PersonB("Wilf","Smith");
		byte[] bs = write(p);
		dumpinfo(bs);
		PersonB p2 = (PersonB)read(bs);
		check(p,p2);
	}
	
	private void dumpinfo(byte[] bytes) {
		CRC32 crc = new CRC32();
		crc.update(bytes,0,bytes.length);
		System.out.println("byteinfo:len="+bytes.length+":crc="+Long.toHexString(crc.getValue()));
	}

	public static void check(Object before, Object after) {
		if (before==null && after!=null) {
			throw new IllegalStateException("Missing deserialized object for comparison");
		}
		if (!before.toString().equals(after.toString())) {
			IllegalStateException ise =  new IllegalStateException("Not the same "+before+" and "+after);
			ise.printStackTrace();
			throw ise;
		}
		System.out.println("check ok");
	}
	
	public static byte[] write(Object o) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(o);
			oos.close();
			baos.flush();
			return baos.toByteArray();
		} catch (Exception e) {
			return null;
		}
	}
	
	public static Object read(byte[] bs) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bs);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Object o = ois.readObject();
			return o;
		} catch (Exception e) {
			return null;
		}
		
	}
}
