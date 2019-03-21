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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Enables us to check things quickly in the constant pool. This version accumulates the class references and the method
 * references, for classes that start with 'j' (we want to catch: java/lang). It skips everything it can and the end
 * result is a list of class references and a list of method references. The former look like this 'a/b/C' whilst the
 * latter look like this 'java/lang/Foo.bar' (the descriptor for the method is not included). Interface methods are
 * skipped.
 *
 * Useful reference: https://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html
 *
 * @author Andy Clement
 * @since 0.7.3
 */
public class ConstantPoolScanner {

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

	private final static byte CONSTANT_MethodHandle = 15;

	private final static byte CONSTANT_MethodType = 16;

	private final static byte CONSTANT_InvokeDynamic = 18;

	private byte[] classbytes;

	// Used during the parse step
	private int ptr;

	// Filled with strings and int[]
	private Object[] cpdata;

	private int cpsize;

	private int[] type;

	// Does not need to be a set as there are no dups in the ConstantPool (for a class from a decent compiler...)
	private List<String> referencedClasses = new ArrayList<String>();

	private List<String> referencedMethods = new ArrayList<String>();

	private String slashedclassname;


	public static References getReferences(byte[] classbytes) {
		ConstantPoolScanner cpScanner = new ConstantPoolScanner(classbytes);
		return new References(cpScanner.slashedclassname, cpScanner.referencedClasses, cpScanner.referencedMethods);
	}

	private ConstantPoolScanner(byte[] bytes) {
		parseClass(bytes);
		computeReferences();
	}

	// Format of a classfile:
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
	private void parseClass(byte[] bytes) {
		try {
			this.classbytes = bytes;
			this.ptr = 0;
			int magic = readInt(); // magic 0xCAFEBABE
			if (magic != 0xCAFEBABE) {
				throw new IllegalStateException("not bytecode, magic was 0x" + Integer.toString(magic, 16));
			}
			ptr += 4; // skip minor and major versions
			cpsize = readUnsignedShort();
			if (DEBUG) {
				System.out.println("Constant Pool Size =" + cpsize);
			}
			cpdata = new Object[cpsize];
			type = new int[cpsize];
			for (int cpentry = 1; cpentry < cpsize; cpentry++) {
				boolean wasDoubleSlotItem = processConstantPoolEntry(cpentry);
				if (wasDoubleSlotItem) {
					cpentry++;
				}
			}
			ptr += 2; // access flags
			int thisclassname = readUnsignedShort();
			int classindex = ((Integer) cpdata[thisclassname]);
			slashedclassname = accessUtf8(classindex);
		}
		catch (Exception e) {
			throw new IllegalStateException("Unexpected problem processing bytes for class", e);
		}
	}

	/**
	 * Return the UTF8 at the specified index in the constant pool. The data found at the constant pool for that index
	 * may not have been unpacked yet if this is the first access of the string. If not unpacked the constant pool entry
	 * is a pair of ints in an array representing the offset and length within the classbytes where the UTF8 string is
	 * encoded. Once decoded the constant pool entry is flipped from an int array to a String for future fast access.
	 *
	 * @param cpIndex constant pool index
	 * @return UTF8 string at that constant pool index
	 */
	private String accessUtf8(int cpIndex) {
		Object object = cpdata[cpIndex];
		if (object instanceof String) {
			return (String) object;
		}
		int[] ptrAndLen = (int[]) object;
		String value;
		try {
			value = new String(classbytes, ptrAndLen[0], ptrAndLen[1], "UTF8");
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Bad data found at constant pool position " + cpIndex + " offset="
					+ ptrAndLen[0] + " length=" + ptrAndLen[1], e);
		}
		cpdata[cpIndex] = value;
		return value;
	}

	/**
	 * @return an int constructed from the next four bytes to be processed
	 */
	private final int readInt() {
		return ((classbytes[ptr++] & 0xFF) << 24) + ((classbytes[ptr++] & 0xFF) << 16)
				+ ((classbytes[ptr++] & 0xFF) << 8)
				+ (classbytes[ptr++] & 0xFF);
	}

	/**
	 * @return an unsigned short constructed from the next two bytes to be processed
	 */
	private final int readUnsignedShort() {
		return ((classbytes[ptr++] & 0xff) << 8) + (classbytes[ptr++] & 0xff);
	}

	private boolean processConstantPoolEntry(int index) throws IOException {
		byte b = classbytes[ptr++];
		switch (b) {
			case CONSTANT_Integer: // CONSTANT_Integer_info { u1 tag; u4 bytes; }
			case CONSTANT_Float: // CONSTANT_Float_info { u1 tag; u4 bytes; }
			case CONSTANT_Fieldref: // CONSTANT_Fieldref_info { u1 tag; u2 class_index; u2 name_and_type_index; }
			case CONSTANT_InterfaceMethodref: // CONSTANT_InterfaceMethodref_info { u1 tag; u2 class_index; u2 name_and_type_index; }
			case CONSTANT_InvokeDynamic: // CONSTANT_InvokeDynamic_info { u1 tag; u2 bootstrap_method_attr_index; u2 name_and_type_index; }
				ptr += 4;
				break;
			case CONSTANT_Utf8:
				// CONSTANT_Utf8_info { u1 tag; u2 length; u1 bytes[length]; }
				// Cache just the index and length - do not unpack it now
				int len = readUnsignedShort();
				cpdata[index] = new int[] { ptr, len };
				ptr += len;
				break;
			case CONSTANT_Long: // CONSTANT_Long_info { u1 tag; u4 high_bytes; u4 low_bytes; }
			case CONSTANT_Double: // CONSTANT_Double_info { u1 tag; u4 high_bytes; u4 low_bytes; }
				ptr += 8;
				return true;
			case CONSTANT_Class: // CONSTANT_Class_info { u1 tag; u2 name_index; }
				type[index] = b;
				cpdata[index] = readUnsignedShort();
				break;
			case CONSTANT_Methodref:
				// CONSTANT_Methodref_info { u1 tag; u2 class_index; u2 name_and_type_index; }
				type[index] = b;
				cpdata[index] = new int[] { readUnsignedShort(), readUnsignedShort() };
				break;
			case CONSTANT_NameAndType:
				// The CONSTANT_NameAndType_info structure is used to represent a field or method, without indicating which class or interface type it belongs to:
				// CONSTANT_NameAndType_info { u1 tag; u2 name_index; u2 descriptor_index; }
				//			type[index] = b;
				cpdata[index] = readUnsignedShort();
				ptr += 2; // skip the descriptor for now
				break;
			case CONSTANT_MethodHandle:
				// CONSTANT_MethodHandle_info { u1 tag; u1 reference_kind; u2 reference_index; }
				ptr += 3;
				break;
			case CONSTANT_String: // CONSTANT_String_info { u1 tag; u2 string_index; }
			case CONSTANT_MethodType: // CONSTANT_MethodType_info { u1 tag; u2 descriptor_index; }
				ptr += 2;
				break;
			default:
				throw new IllegalStateException("Entry: " + index + " " + Byte.toString(b));
		}
		return false;
	}

	private void computeReferences() {
		for (int i = 0; i < cpsize; i++) {
			switch (type[i]) {
				case CONSTANT_Class:
					int classindex = ((Integer) cpdata[i]);
					String classname = accessUtf8(classindex);
					if (classname == null) {
						throw new IllegalStateException();
					}
					referencedClasses.add(classname);
					break;
				case CONSTANT_Methodref:
					int[] indexes = (int[]) cpdata[i];
					int classindex2 = indexes[0];
					int nameAndTypeIndex = indexes[1];
					StringBuilder s = new StringBuilder();
					String theClassName = accessUtf8((Integer) cpdata[classindex2]);
					if (theClassName.charAt(0) == 'j') {
						s.append(theClassName);
						s.append(".");
						s.append(accessUtf8((Integer) cpdata[nameAndTypeIndex]));
						referencedMethods.add(s.toString());
					}
					break;
				//			private final static byte CONSTANT_Utf8 = 1;
				//			private final static byte CONSTANT_Integer = 3;
				//			private final static byte CONSTANT_Float = 4;
				//			private final static byte CONSTANT_Long = 5;
				//			private final static byte CONSTANT_Double = 6;
				//			private final static byte CONSTANT_String = 8;
				//			private final static byte CONSTANT_Fieldref = 9;
				//			private final static byte CONSTANT_InterfaceMethodref = 11;
				//			private final static byte CONSTANT_NameAndType = 12;
			}
		}
	}


	public static class References {

		public final String slashedClassName;

		public final List<String> referencedClasses;

		public final List<String> referencedMethods;

		References(String slashedClassName, List<String> rc, List<String> rm) {
			this.slashedClassName = slashedClassName;
			this.referencedClasses = rc;
			this.referencedMethods = rm;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("Class=").append(slashedClassName).append("\n");
			s.append("ReferencedClasses=#").append(referencedClasses.size()).append("\n");
			s.append("ReferencedMethods=#").append(referencedMethods.size()).append("\n");
			return s.toString();
		}
	}

}
