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
package org.springsource.loaded.testgen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is not actually part of the test infrastructure itself. It is a quick and dirty Java application that prints out Java
 * source code for a 'Invoker' class. That is, a class that contains code invoking the different reflection methods of a given
 * Target type. We use the reflection API to get all the methods defined on a given type and create a method that call that method.
 * 
 * @author kdvolder
 */
public class InvokerGenerator {

	public static void main(String[] args) {
		InvokerGenerator generator = new InvokerGenerator(Constructor.class);
		//		System.out.println(generator.getCode());
		System.out.println(generator.getMethodNameArray());
	}

	private static final String INDENT_STR = "    ";

	/**
	 * Set this to tell the generator what the target type is.
	 */
	private Class<?> targetClass;

	/**
	 * Set this to define the package in which the invoker lives.
	 */
	private String invokerPkg;

	/**
	 * Class name of the invokder class (without package)
	 */
	private String invokerClassName;

	private Set<String> imports = null;

	public InvokerGenerator(Class<?> targetClass) {
		this.targetClass = targetClass;
		this.invokerPkg = "reflection";
		this.invokerClassName = targetClass.getSimpleName() + "Invoker";
	}

	/**
	 * @return the generated code as a String
	 */
	public String getCode() {
		imports = new HashSet<String>();
		addImport(targetClass);
		String methods = getMethods();
		String header = getHeader();
		String footer = getFooter();
		return header + methods + footer;
	}

	private String getMethodNameArray() {
		StringBuffer code = new StringBuffer();
		Method[] methods = targetClass.getDeclaredMethods();
		for (Method method : methods) {
			if (Modifier.isPublic(method.getModifiers())) {
				code.append('"');
				code.append(method.getDeclaringClass().getSimpleName() + ".");
				code.append(method.getName());
				code.append("\",\n");
			}
		}
		return code.toString();
	}

	private void addImport(Class<?> clazz) {
		if (clazz.isPrimitive()) {
			return;
		}
		if (clazz.isArray()) {
			addImport(clazz.getComponentType());
			return;
		}
		imports.add(clazz.getName());
	}

	private String getFooter() {
		return "}\n";
	}

	private String getMethods() {
		StringBuffer code = new StringBuffer();
		Method[] methods = targetClass.getDeclaredMethods();
		for (Method method : methods) {
			if (Modifier.isPublic(method.getModifiers())) {
				generateCallerMethod(method, code);
			}
		}
		return code.toString();
	}

	private void generateCallerMethod(Method method, StringBuffer code) {
		code.append(INDENT_STR);
		code.append("public static " + method.getReturnType().getSimpleName() + " " + "call" + capitalize(method.getName()) + "(");
		addImport(method.getReturnType());
		generateFormalParams(method, code);
		code.append(")\n");
		Class<?>[] exceptions = method.getExceptionTypes();
		if (exceptions.length > 0) {
			code.append(INDENT_STR + "throws ");
			for (int i = 0; i < exceptions.length; i++) {
				addImport(exceptions[i]);
				if (i > 0) {
					code.append(", ");
				}
				code.append(exceptions[i].getSimpleName());
			}
			code.append("\n");
		}
		code.append(INDENT_STR + "{\n");
		generateBody(method, code);
		code.append(INDENT_STR + "}\n\n");
	}

	private void generateBody(Method method, StringBuffer code) {
		code.append(INDENT_STR + INDENT_STR);

		if (method.getReturnType() != void.class) {
			code.append("return ");
		}

		if (Modifier.isStatic(method.getModifiers())) {
			code.append(targetClass.getSimpleName() + ".");
		} else {
			code.append("thiz.");
		}

		code.append(method.getName() + "(");
		generateActualParams(method, code);
		code.append(");\n");
	}

	private void generateActualParams(Method method, StringBuffer code) {

		int i = 0;
		for (Class<?> param : method.getParameterTypes()) {
			addImport(param);
			if (i > 0) {
				code.append(", ");
			}
			code.append("a" + (i++));
		}
	}

	private void generateFormalParams(Method method, StringBuffer code) {
		Class<?>[] params = method.getParameterTypes();

		boolean addThisParam = !Modifier.isStatic(method.getModifiers());

		if (addThisParam) {
			code.append(targetClass.getSimpleName() + " " + "thiz");
		}

		int i = 0;
		for (Class<?> param : params) {
			if (i > 0 || addThisParam) {
				code.append(", ");
			}
			code.append(param.getSimpleName() + " a" + (i++));
		}
	}

	private String capitalize(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private String getHeader() {
		return "package " + invokerPkg + ";\n" + "\n" + getImports() + "\n" + "/**\n"
				+ " * Class containing one method for each method in the " + targetClass.getName() + "\n"
				+ " * containing code calling that method.\n" + " */\n" + "@SuppressWarnings({\"unchecked\",\"rawtypes\"})"
				+ "public class " + invokerClassName + "{\n" + "\n";
	}

	private String getImports() {
		StringBuffer code = new StringBuffer();
		for (String imp : imports) {
			code.append("import " + imp + ";\n");
		}
		return code.toString();
	}

}
