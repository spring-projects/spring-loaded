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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;

/**
 * Common constants used throughout Spring Loaded.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public interface Constants extends Opcodes {

	public static final Integer DEFAULT_INT = Integer.valueOf(0);

	public static final Byte DEFAULT_BYTE = Byte.valueOf((byte) 0);

	public static final Character DEFAULT_CHAR = Character.valueOf((char) 0);

	public static final Short DEFAULT_SHORT = Short.valueOf((short) 0);

	public static final Long DEFAULT_LONG = Long.valueOf(0);

	public static final Float DEFAULT_FLOAT = Float.valueOf(0);

	public static final Double DEFAULT_DOUBLE = Double.valueOf(0);

	public static final Boolean DEFAULT_BOOLEAN = Boolean.FALSE;

	static String magicDescriptorForGeneratedCtors = "org.springsource.loaded.C";

	// TODO change r$ to _sl or sl throughout?
	static String PREFIX = "r$";

	static String tRegistryType = "org/springsource/loaded/TypeRegistry";

	static String lRegistryType = "L" + tRegistryType + ";";

	static String tDynamicallyDispatchable = "org/springsource/loaded/__DynamicallyDispatchable";

	static String lDynamicallyDispatchable = "L" + tDynamicallyDispatchable + ";";

	static String tReloadableType = "org/springsource/loaded/ReloadableType";

	static String lReloadableType = "L" + tReloadableType + ";";

	static String tInstanceStateManager = "org/springsource/loaded/ISMgr";

	static String lInstanceStateManager = "L" + tInstanceStateManager + ";";

	static String tStaticStateManager = "org/springsource/loaded/SSMgr";

	static String lStaticStateManager = "L" + tStaticStateManager + ";";

	static String fReloadableTypeFieldName = PREFIX + "type";

	// Static field holding map and accessors
	static String fStaticFieldsName = PREFIX + "sfields";

	static String mStaticFieldSetterName = PREFIX + "sets";

	static String mStaticFieldSetterDescriptor = "(Ljava/lang/Object;Ljava/lang/String;)V";

	static String mStaticFieldGetterName = PREFIX + "gets";

	// Instance field holding map and accessors
	static String fInstanceFieldsName = PREFIX + "fields";

	static String mInstanceFieldSetterName = PREFIX + "set";

	static String mInstanceFieldSetterDescriptor = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V";

	static String mInstanceFieldGetterName = PREFIX + "get";

	static String mInstanceFieldGetterDescriptor = "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;";

	static String mStaticFieldInterceptionRequired = "staticFieldInterceptionRequired";

	static String mInstanceFieldInterceptionRequired = "instanceFieldInterceptionRequired";

	// method called to see if the target of what is about to be called has changed
	static String mChangedForInvocationName = "anyChanges";

	static String mChangedForInvokeStaticName = "istcheck";

	static String mChangedForInvokeInterfaceName = "iincheck";

	static String mChangedForInvokeDynamicName = "idycheck";

	static String mChangedForInvokeVirtualName = "ivicheck";

	static String mChangedForInvokeSpecialName = "ispcheck";

	static String mPerformInvokeDynamicName = "idyrun";

	static String descriptorChangedForInvokeSpecialName = "(ILjava/lang/String;)Lorg/springsource/loaded/__DynamicallyDispatchable;";

	static String mChangedForConstructorName = "ccheck";

	static int WAS_INVOKESTATIC = 0x0001;

	static int WAS_INVOKEVIRTUAL = 0x0002;

	// Dynamic dispatch method
	static String mDynamicDispatchName = "__execute";

	static String mDynamicDispatchDescriptor = "([Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;";

	static String mInitializerName = "___init___";

	static String mStaticInitializerName = "___clinit___";

	static int ACC_PUBLIC_ABSTRACT = Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT;

	static int ACC_PRIVATE_STATIC = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC;

	static int ACC_PUBLIC_STATIC = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;

	static int ACC_PUBLIC_STATIC_FINAL = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;

	static int ACC_PUBLIC_INTERFACE = Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;

	static int ACC_PUBLIC_STATIC_SYNTHETIC = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;

	static int ACC_PUBLIC_SYNTHETIC = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;

	static int ACC_PUBLIC_PROTECTED = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED;

	static int ACC_PUBLIC_PRIVATE_PROTECTED = Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED;

	static int ACC_PRIVATE_STATIC_SYNTHETIC = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;

	static int ACC_PRIVATE_PROTECTED = Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED;

	static int ACC_PRIVATE_STATIC_FINAL = ACC_FINAL | ACC_STATIC | ACC_PRIVATE;

	static String[] NO_STRINGS = new String[0];

	static Method[] NO_METHODS = new Method[0];

	static Field[] NO_FIELDS = new Field[0];

	//Name pattern used to recognise names of Executor classes.
	static Pattern executorClassNamePattern = Pattern.compile("\\$\\$E[0-9,a-z,A-Z]+$");

	static final String jlObject = "java/lang/Object";

	//
	public static int JLC_GETDECLAREDFIELDS = 0x0001;

	public static int JLC_GETDECLAREDFIELD = 0x0002;

	public static int JLC_GETFIELD = 0x0004;

	public static int JLC_GETDECLAREDMETHODS = 0x0008;

	public static int JLC_GETDECLAREDMETHOD = 0x0010;

	public static int JLC_GETMETHOD = 0x0020;

	public static int JLC_GETDECLAREDCONSTRUCTOR = 0x0040;

	public static int JLC_GETMODIFIERS = 0x0080;

	public static int JLC_GETMETHODS = 0x0100;

	public static int JLC_GETCONSTRUCTOR = 0x0200;

	public static int JLC_GETDECLAREDCONSTRUCTORS = 0x0400;

	public static int JLRM_INVOKE = 0x0800;

	public static int JLRF_GET = 0x1000;

	public static int JLRF_GETLONG = 0x2000;

	public static int JLOS_HASSTATICINITIALIZER = 0x4000;

	// For rewritten reflection in system classes, these are used:
	// The member names are used for fields *and* methods
	static final String jlcgdfs = "__sljlcgdfs";

	static final String jlcgdfsDescriptor = "(Ljava/lang/Class;)[Ljava/lang/reflect/Field;";

	static final String jlcgdf = "__sljlcgdf";

	static final String jlcgdfDescriptor = "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;";

	static final String jlcgf = "__sljlcgf";

	static final String jlcgfDescriptor = "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;";

	static final String jlcgdms = "__sljlcgdms";

	static final String jlcgdmsDescriptor = "(Ljava/lang/Class;)[Ljava/lang/reflect/Method;";

	static final String jlcgdm = "__sljlcgdm";

	static final String jlcgdmDescriptor = "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;";

	static final String jlcgm = "__sljlcgm";

	static final String jlcgmDescriptor = "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;";

	static final String jlcgdc = "__sljlcgdc";

	static final String jlcgdcDescriptor = "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/reflect/Constructor;";

	static final String jlcgc = "__sljlcgc";

	static final String jlcgcDescriptor = "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/reflect/Constructor;";

	static final String jlcgmods = "__sljlcgmods";

	static final String jlcgmodsDescriptor = "(Ljava/lang/Class;)I";

	static final String jlcgms = "__sljlcgms";

	static final String jlcgmsDescriptor = "(Ljava/lang/Class;)[Ljava/lang/reflect/Method;";

	// TODO migrate those above to this slightly more comprehensible format
	static final String jlcGetDeclaredConstructorsMember = "__sljlcgdcs";

	static final String jlcGetDeclaredConstructorsDescriptor = "(Ljava/lang/Class;)[Ljava/lang/reflect/Constructor;";

	static final String jlrmInvokeMember = "__sljlrmi";

	static final String jlrmInvokeDescriptor = "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";

	static final String jlrfGetMember = "__sljlrfg";

	static final String jlrfGetDescriptor = "(Ljava/lang/reflect/Field;Ljava/lang/Object;)Ljava/lang/Object;";

	static final String jlrfGetLongMember = "__sljlrfgl";

	static final String jlrfGetLongDescriptor = "(Ljava/lang/reflect/Field;Ljava/lang/Object;)J";

	static final String jloObjectStream_hasInitializerMethod = "__sljlos_him";

	static final String methodSuffixSuperDispatcher = "_$superdispatcher$";
}
