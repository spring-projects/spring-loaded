/*
 * Copyright 2010-2012 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springsource.loaded;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// TODO does not yet support the new constant pool entry types that come with Java 7

// http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html
/**
 * Quickly checks the constant pool for class references, it skips everything else as fast as it can. The class references are then
 * available for checking.
 * 
 * @author Andy Clement
 * @since 0.7.3
 */
public class ConstantPoolChecker {

	private static final boolean DEBUG = false;

	private final static byte CONSTANT_Utf8 = 1;
	private final static byte CONSTANT_Integer = 3;
	private final static byte CONSTANT_Float = 4;
	private final static byte CONSTANT_Long = 5;
	private final static byte CONSTANT_Double = 6;
	private final static byte CONSTANT_Class = 7;
	private final static byte CONSTANT_String = 8;
	private final static byte CONSTANT_Fieldref = 9;
	private final static byte CONSTANT_Methodref = 10;
	private final static byte CONSTANT_InterfaceMethodref = 11;
	private final static byte CONSTANT_NameAndType = 12;

	// Test entry point just goes through all the code in the bin folder
	public static void main(String[] args) throws Exception {
		//		File[] fs = new File("./bin").listFiles();
		//		File[] fs = new File("../testdata-groovy/bin").listFiles();
		//		checkThemAll(fs);
		//		System.out.println("total=" + total / 1000000d);
	}

	//	static long total = 0;

	//	private static void checkThemAll(File[] fs) throws Exception {
	//		for (File f : fs) {
	//			if (f.isDirectory()) {
	//				checkThemAll(f.listFiles());
	//			} else if (f.getName().endsWith(".class")) {
	//				System.out.println(f);
	//				byte[] data = Utils.loadFromStream(new FileInputStream(f));
	//				long stime = System.nanoTime();
	//				List<String> ls = getReferencedClasses(data);
	//				//				total += (System.nanoTime() - stime);
	//				System.out.println(ls);
	//			}
	//		}
	//	}

	//	ClassFile {
	//    	u4 magic;
	//    	u2 minor_version;
	//    	u2 major_version;
	//    	u2 constant_pool_count;
	//    	cp_info constant_pool[constant_pool_count-1];
	//    	u2 access_flags;
	//    	u2 this_class;
	//    	u2 super_class;
	//    	u2 interfaces_count;
	//    	u2 interfaces[interfaces_count];
	//    	u2 fields_count;
	//    	field_info fields[fields_count];
	//    	u2 methods_count;
	//    	method_info methods[methods_count];
	//    	u2 attributes_count;
	//    	attribute_info attributes[attributes_count];
	//    }

	static List<String> getReferencedClasses(byte[] bytes) {
		return new ConstantPoolChecker(bytes).referencedClasses;
	}

	// Filled with strings and int[]
	private Object[] cpdata;
	private int cpsize;
	private int[] type;
	// Does not need to be a set as there are no dups in the ConstantPool (for a class from a decent compiler...)
	private List<String> referencedClasses = new ArrayList<String>();

	private ConstantPoolChecker(byte[] bytes) {
		readConstantPool(bytes);
		computeReferences();
	}

	public void computeReferences() {
		for (int i = 0; i < cpsize; i++) {
			switch (type[i]) {
			case CONSTANT_Class:
				int classindex = ((Integer) cpdata[i]);
				String classname = (String) cpdata[classindex];
				if (classname == null) {
					throw new IllegalStateException();
				}
				referencedClasses.add(classname);
				break;
			//			private final static byte CONSTANT_Utf8 = 1;
			//			private final static byte CONSTANT_Integer = 3;
			//			private final static byte CONSTANT_Float = 4;
			//			private final static byte CONSTANT_Long = 5;
			//			private final static byte CONSTANT_Double = 6;
			//			private final static byte CONSTANT_String = 8;
			//			private final static byte CONSTANT_Fieldref = 9;
			//			private final static byte CONSTANT_Methodref = 10;
			//			private final static byte CONSTANT_InterfaceMethodref = 11;
			//			private final static byte CONSTANT_NameAndType = 12;
			}
		}
	}

	public void readConstantPool(byte[] bytes) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			DataInputStream dis = new DataInputStream(bais);

			int magic = dis.readInt(); // magic 0xCAFEBABE
			if (magic != 0xCAFEBABE) {
				throw new IllegalStateException("not bytecode, magic was 0x" + Integer.toString(magic, 16));
			}
			dis.skip(4);
			//			dis.readShort(); // minor
			//			dis.readShort(); // major
			cpsize = dis.readShort();
			if (DEBUG) {
				System.out.println("Constant Pool Size =" + cpsize);
			}
			cpdata = new Object[cpsize];
			type = new int[cpsize];
			//			int max = cpsize - 1;
			for (int cpentry = 1; cpentry < cpsize; cpentry++) {
				boolean doubleSlot = processConstantPoolEntry(cpentry, dis);
				if (doubleSlot) {
					cpentry++;
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Unexpected problem processing bytes for class", e);
		}
	}

	private boolean processConstantPoolEntry(int index, DataInputStream dis) throws IOException {
		byte b = dis.readByte();
		type[index] = b;
		switch (b) {
		case CONSTANT_Utf8:
			//			CONSTANT_Utf8_info {
			//		    	u1 tag;
			//		    	u2 length;
			//		    	u1 bytes[length];
			//		    }
			cpdata[index] = dis.readUTF();
			if (DEBUG) {
				System.out.println(index + ":UTF8[" + cpdata[index] + "]");
			}
			break;
		case CONSTANT_Integer:
			//			 CONSTANT_Integer_info {
			//			    	u1 tag;
			//			    	u4 bytes;
			//			 }
			if (DEBUG) {
				int i = dis.readInt();
				if (DEBUG) {
					System.out.println(index + ":INTEGER[" + i + "]");
				}
			} else {
				dis.skip(4);
			}
			break;
		case CONSTANT_Float:
			//			 CONSTANT_Float_info {
			//			    	u1 tag;
			//			    	u4 bytes;
			//			 }
			if (DEBUG) {
				float f = dis.readFloat();
				if (DEBUG) {
					System.out.println(index + ":FLOAT[" + f + "]");
				}
			} else {
				dis.skip(4);
			}
			break;
		case CONSTANT_Long:
			//		CONSTANT_Long_info {
			//	    	u1 tag;
			//	    	u4 high_bytes;
			//	    	u4 low_bytes;
			//	    }
			if (DEBUG) {
				long l = dis.readLong();
				if (DEBUG) {
					System.out.println(index + ":LONG[" + l + "]");
				}
			} else {
				dis.skip(8);
			}
			return true;
		case CONSTANT_Double:
			//	    CONSTANT_Double_info {
			//	    	u1 tag;
			//	    	u4 high_bytes;
			//	    	u4 low_bytes;
			//	    }
			if (DEBUG) {
				double d = dis.readDouble();
				if (DEBUG) {
					System.out.println(index + ":DOUBLE[" + d + "]");
				}
			} else {
				dis.skip(8);
			}
			return true;
		case CONSTANT_Class:
			//			CONSTANT_Class_info {
			//		    	u1 tag;
			//		    	u2 name_index;
			//		    }
			cpdata[index] = (int) dis.readShort();
			if (DEBUG) {
				System.out.println(index + ":CLASS[name_index=" + cpdata[index] + "]");
			}
			break;
		case CONSTANT_String:
			//			CONSTANT_String_info {
			//		    	u1 tag;
			//		    	u2 string_index;
			//		    }
			if (DEBUG) {
				cpdata[index] = (int) dis.readShort();
				if (DEBUG) {
					System.out.println(index + ":STRING[string_index=" + cpdata[index] + "]");
				}
			} else {
				dis.skip(2);
			}
			break;
		case CONSTANT_Fieldref:
			//			CONSTANT_Fieldref_info {
			//		    	u1 tag;
			//		    	u2 class_index;
			//		    	u2 name_and_type_index;
			//		    }
			if (DEBUG) {
				cpdata[index] = new int[] { dis.readShort(), dis.readShort() };
				if (DEBUG) {
					System.out.println(index + ":FIELDREF[class_index=" + ((int[]) cpdata[index])[0] + ",name_and_type_index="
							+ ((int[]) cpdata[index])[1] + "]");
				}
			} else {
				dis.skip(4);
			}
			break;
		case CONSTANT_Methodref:
			//			 CONSTANT_Methodref_info {
			//			    	u1 tag;
			//			    	u2 class_index;
			//			    	u2 name_and_type_index;
			//			    }	
			if (DEBUG) {
				cpdata[index] = new int[] { dis.readShort(), dis.readShort() };
				if (DEBUG) {
					System.out.println(index + ":METHODREF[class_index=" + ((int[]) cpdata[index])[0] + ",name_and_type_index="
							+ ((int[]) cpdata[index])[1] + "]");
				}
			} else {
				dis.skip(4);
			}
			break;
		case CONSTANT_InterfaceMethodref:
			//			 CONSTANT_InterfaceMethodref_info {
			//			    	u1 tag;
			//			    	u2 class_index;
			//			    	u2 name_and_type_index;
			//			    }
			if (DEBUG) {
				cpdata[index] = new int[] { dis.readShort(), dis.readShort() };
				if (DEBUG) {
					System.out.println(index + ":INTERFACEMETHODREF[class_index=" + ((int[]) cpdata[index])[0]
							+ ",name_and_type_index=" + ((int[]) cpdata[index])[1] + "]");
				}
			} else {
				dis.skip(4);
			}
			break;
		case CONSTANT_NameAndType:
			// The CONSTANT_NameAndType_info structure is used to represent a field or method, without indicating which class or interface type it belongs to:
			//			    CONSTANT_NameAndType_info {
			//			    	u1 tag;
			//			    	u2 name_index;
			//			    	u2 descriptor_index;
			//			    }
			if (DEBUG) {
				cpdata[index] = new int[] { dis.readShort(), dis.readShort() };
				if (DEBUG) {
					System.out.println(index + ":NAMEANDTYPE[name_index=" + ((int[]) cpdata[index])[0] + ",descriptor_index="
							+ ((int[]) cpdata[index])[1] + "]");
				}
			} else {
				dis.skip(4);
			}
			break;
		default:
			throw new IllegalStateException("Entry: " + index + " " + Byte.toString(b));
		}
		return false;
	}
}
