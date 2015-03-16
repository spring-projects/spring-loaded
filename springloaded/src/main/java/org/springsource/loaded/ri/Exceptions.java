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

package org.springsource.loaded.ri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectweb.asm.Type;

/**
 * Utility class to create correctly formatted Exceptions and Errors for different kinds of error conditions.
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class Exceptions {

	static IllegalAccessException illegalSetFinalFieldException(Field field, Class<?> valueType, Object value) {
		// Example of error when setting a primitive type final field:		
		//		Can not set final short field reflection.nonrelfields.NonReloadableClassWithFields.nrlShort to (short)2		

		String fieldType = field.getType().getName();
		String fieldQName = field.getDeclaringClass().getName() + "." + field.getName();
		String valueString;
		if (value == null) {
			valueString = "null value";
		}
		else if (valueType.isPrimitive()) {
			valueString = "(" + valueType.getName() + ")" + value;
		}
		else {
			valueString = value == null ? "null value" : value.getClass().getName();
		}
		return new IllegalAccessException("Can not set final " + fieldType + " field " + fieldQName + " to "
				+ valueString);
	}

	static IllegalArgumentException illegalSetFieldTypeException(Field field, Class<?> valueType, Object value) {
		int mods = field.getModifiers() & (Modifier.FINAL | Modifier.STATIC);
		String modStr = Modifier.toString(mods);
		if (!modStr.equals("")) {
			modStr = modStr + " ";
		}

		String fieldType = field.getType().getName();
		String fieldQName = field.getDeclaringClass().getName() + "." + field.getName();
		String valueStr;
		if (valueType == null) {
			valueStr = "null value";
		}
		else if (valueType.isPrimitive()) {
			valueStr = "(" + valueType.getName() + ")" + value;
		}
		else {
			valueStr = valueType.getName();
		}
		return new IllegalArgumentException("Can not set " + modStr + fieldType + " field " + fieldQName + " to "
				+ valueStr);
	}

	public static NoSuchFieldError noSuchFieldError(Field field) {
		return new NoSuchFieldError(field.getName());
	}

	public static NoSuchMethodError noSuchMethodError(Method method) {
		return Exceptions.noSuchMethodError(method.getDeclaringClass().getName(), method.getName(),
				Type.getMethodDescriptor(method));
	}

	public static NoSuchMethodError noSuchMethodError(String dottedClassName, String methodName, String methodDescriptor) {
		return new NoSuchMethodError(dottedClassName + "." + methodName + methodDescriptor);
	}

	static NoSuchMethodException noSuchMethodException(Class<?> clazz, String name, Class<?>... params) {
		return new NoSuchMethodException(clazz.getName() + "." + name + ReflectiveInterceptor.toParamString(params));
	}

	static NoSuchFieldException noSuchFieldException(String name) {
		return new NoSuchFieldException(name);
	}

	public static IllegalArgumentException illegalGetFieldType(Field field, Class<?> returnType) {
		String fieldQName = field.getDeclaringClass().getName() + "." + field.getName();
		String returnTypeName = returnType.getName();
		String fieldType = field.getType().getName();
		return new IllegalArgumentException("Attempt to get " + fieldType + " field \"" + fieldQName
				+ "\" with illegal data type conversion to " + returnTypeName);
	}

	public static NoSuchMethodException noSuchConstructorException(Class<?> clazz, Class<?>[] params) {
		return noSuchMethodException(clazz, "<init>", params);
	}

	public static NoSuchMethodError noSuchConstructorError(Constructor<?> c) {
		//Example error message from Sun JVM:
		//			Exception in thread "main" java.lang.NoSuchMethodError: blah.Target.<init>(CC)V
		//			at Main.main(Main.java:10)
		return new NoSuchMethodError(c.getDeclaringClass().getName() + ".<init>" + Type.getConstructorDescriptor(c));
	}

	public static InstantiationException instantiation(Class<?> clazz) {
		return new InstantiationException(clazz.getName());
	}

}
