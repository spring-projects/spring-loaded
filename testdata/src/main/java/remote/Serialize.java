package remote;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.zip.CRC32;

public class Serialize {

	private static byte[] storedBytes;

	// This is a serialized Person(Wilf,Smith)
	private static final String serString = 
			"aced00057372000d72656d6f74652e506572736f6e176b418850fd97d50200024c000966697273746e616d657400124c6a6176612f6c616e672f537472696e673b4c00086c6173746e616d6571007e0001787074000457696c66740005536d697468";

	private static final byte[] serBytes;
	
	static {
		serBytes = new byte[serString.length()/2];
		int p = 0;
		while (p < serString.length()) {
			String oneByte = serString.substring(p,p+2);
			int b = Integer.decode("0x"+oneByte);
			serBytes[p/2]=(byte)b;
			p+=2;
			
		}
		System.out.println();
	}
	
	
	public static void main(String[] args) {
		if (args!=null && args.length!=0 && args[0].equals("ds")) {
			new Serialize().checkPredeserializedData();
			
		}
		else {
			run();
			new Serialize().checkPredeserializedData();
		}
	}
	
	public void checkPredeserializedData() {
		Person p = (Person)read(serBytes);
		if (p==null) {
			throw new IllegalStateException("Unable to deserialize Person!");
		}
		if (!p.toString().equals("Wilf Smith")) {
			throw new IllegalStateException("Unable to deserialized pre-serialized data: "+p.toString());
		}
		System.out.println("Pre-serialized form checked ok");
	}
	
	public static byte[] loadBytesFromStream(InputStream stream) {
		try {
			BufferedInputStream bis = new BufferedInputStream(stream);
			byte[] theData = new byte[10000000];
			int dataReadSoFar = 0;
			byte[] buffer = new byte[1024];
			int read = 0;
			while ((read = bis.read(buffer)) != -1) {
				System.arraycopy(buffer, 0, theData, dataReadSoFar, read);
				dataReadSoFar += read;
			}
			bis.close();
			// Resize to actual data read
			byte[] returnData = new byte[dataReadSoFar];
			System.arraycopy(theData, 0, returnData, 0, dataReadSoFar);
			return returnData;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	

	public static void run() {
//		new Serialize().run1();
		new Serialize().run2();
	}
	
	public void writePerson() {
		Person p = new Person("Wilf","Smith");
		storedBytes = write(p);
		System.out.println("Person stored ok");
	}
	
	public Person readPerson() {
		Person person = (Person)read(storedBytes);
		if (!person.toString().equals("Wilf Smith")) {
			throw new IllegalStateException("Expected 'Wilf Smith' but was '"+person.toString()+"'");
		}
		System.out.println("Person read ok");
		return person;
	}
	
	public void printSecret() throws Exception {
		Person p = readPerson();
		Field f = p.getClass().getDeclaredField("newSecretField");
		f.setAccessible(true);
		Object value = f.get(p);
		System.out.println(value);
	}
	
	public void run1() {
		String s = "abc";
		byte[] bs = write(s);
		String s2 = (String)read(bs);
		check(s,s2);
	}

	public void run2() {
		Person p = new Person("Wilf","Smith");
		byte[] bs = write(p);
		dumpinfo(bs);
		Person p2 = (Person)read(bs);
		check(p,p2);
	}
	
	private void dumpinfo(byte[] bytes) {
		CRC32 crc = new CRC32();
		crc.update(bytes,0,bytes.length);
		StringBuilder data = new StringBuilder();
		for (int i=0;i<bytes.length;i++) {
			int val = bytes[i];
			String s = "00"+Integer.toHexString(val);
			data.append(s.substring(s.length()-2));
		}
		System.out.println("Bytedata:"+data.toString());
		System.out.println("byteinfo:len="+bytes.length+":crc="+Long.toHexString(crc.getValue()));
		// when run directly, this will print: byteinfo:len=98:crc=c1047cf6
	}

	public static void check(Object before, Object after) {
		if (before==null && after!=null) {
			throw new IllegalStateException("Missing deserialized object for comparison");
		}
		if (!before.toString().equals(after.toString())) {
			IllegalStateException ise =  new IllegalStateException("Not the same "+before+" and "+after);
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
