package remote;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.CRC32;

public class SerializeG {

	// This is a serialized FakeClosure( field=abc)
	private static final String serString = 
			"aced00057372001272656d6f74652e46616b65436c6f73757265cebbffd721e98fea0200014c00056669656c647400124c6a6176612f6c616e672f537472696e673b7872001367726f6f76792e6c616e672e436c6f737572653ca0c76616126c5a0200074900096469726563746976654900196d6178696d756d4e756d6265724f66506172616d657465727349000f7265736f6c766553747261746567794c000864656c65676174657400124c6a6176612f6c616e672f4f626a6563743b4c00056f776e657271007e00035b000e706172616d6574657254797065737400125b4c6a6176612f6c616e672f436c6173733b4c000a746869734f626a65637471007e0003787000000000000000000000000070707070740003616263";

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
			checkPredeserializedData();
		}
		else {
			run();
			checkPredeserializedData();			
		}
	}
	
	public static void checkPredeserializedData() {
		FakeClosure p = (FakeClosure)read(serBytes);
		if (p==null) {
			throw new IllegalStateException("Unable to deserialize FakeClosure!");
		}
		if (!p.field.equals("abc")) {
			throw new IllegalStateException("Unable to deserialized pre-serialized data: "+p.field);
		}
		System.out.println("Pre-serialized groovy form checked ok");
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
		new SerializeG().run2();
	}

	public void run2() {
		FakeClosure p = new FakeClosure(null);
		p.field = "abc";
		byte[] bs = write(p);
		dumpinfo(bs);
		FakeClosure p2 = (FakeClosure)read(bs);
		check(p,p2);
	}
	
	public static void run3() {
		String p = new String("abc");
		byte[] bs = write(p);
		dumpinfo(bs);
		String p2 = (String)read(bs);
		if (!p2.equals("abc")) {
			throw new IllegalStateException();
		}
	}
	
	private static void dumpinfo(byte[] bytes) {
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

	public static void check(FakeClosure before, FakeClosure after) {
		if (before==null && after!=null) {
			throw new IllegalStateException("Missing deserialized object for comparison");
		}
		if (!before.field.equals(after.field)) {
			IllegalStateException ise =  new IllegalStateException("Not the same "+before.field+" and "+after.field);
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
