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
package org.springsource.loaded.test;

import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.springsource.loaded.ClassRenamer;
import org.springsource.loaded.Constants;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.ISMgr;
import org.springsource.loaded.MethodMember;
import org.springsource.loaded.NameRegistry;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.SSMgr;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils;
import org.springsource.loaded.agent.SpringLoadedPreProcessor;
import org.springsource.loaded.test.infra.ClassPrinter;
import org.springsource.loaded.test.infra.MethodPrinter;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.test.infra.TestClassLoader;


/**
 * Abstract root test class containing helper functions.
 * 
 * @author Andy Clement
 * @since 1.0
 */
public abstract class SpringLoadedTests implements Constants {

	/**
	 * Classloader that can be used to see things in the bin directory, it is initialised ready for each test to use.
	 */
	protected ClassLoader binLoader;

	protected String TestDataPath = TestUtils.getPathToClasses("../testdata");
	protected String GroovyTestDataPath = TestUtils.getPathToClasses("../testdata-groovy");
	protected String AspectjrtJar = "../testdata/aspectjrt.jar";
	protected String CodeJar = "../testdata/code.jar";
	// TODO [java8] replace this with project dependency when Java8 is out
	protected String Java8CodeJar = "../testdata-java8/build/libs/testdata-java8.jar";
	protected String GroovyrtJar = "../testdata-groovy/groovy-all-1.8.6.jar";
	protected Result result;
	protected TypeRegistry registry;

	@Before
	public void setup() throws Exception {
		SpringLoadedPreProcessor.disabled = true;
		NameRegistry.reset();
		binLoader = new TestClassLoader(toURLs(TestDataPath, AspectjrtJar, CodeJar, Java8CodeJar), this.getClass().getClassLoader());
	}

	@After
	public void teardown() throws Exception {
		SpringLoadedPreProcessor.disabled = false;
	}

	public void switchToGroovy() {
		binLoader = new TestClassLoader(toURLs(GroovyTestDataPath, GroovyrtJar), this.getClass().getClassLoader());
		//		Thread.currentThread().setContextClassLoader(binLoader);
	}

	/**
	 * Convert an array of string paths to an array of URLs
	 * 
	 * @param paths the string paths
	 * @return the converted URLs
	 */
	public URL[] toURLs(String... paths) {
		URL[] urls = new URL[paths.length];
		int i = 0;
		for (String path : paths) {
			try {
				urls[i++] = new File(path).toURI().toURL();
			} catch (MalformedURLException e) {
				Assert.fail(e.toString());
			}
		}
		return urls;
	}

	public Object run(Class<?> clazz, String methodname, Object... params) {
		try {
			//			System.out.println("Calling method " + methodname + " on " + clazz.getName());
			Object o = clazz.newInstance();
			Method m = null;
			Method[] ms = clazz.getMethods();
			for (Method mm : ms) {
				if (mm.getName().equals(methodname)) {
					m = mm;
					break;
				}
			}
			// Method m = clazz.getDeclaredMethod(methodname);
			return m.invoke(o, params);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
			return null;
		}
	}

	public Object run(Class<?> clazz, Object o, String methodname, Object... params) {
		try {
			System.out.println("Calling method " + methodname + " on " + clazz.getName());
			Method m = null;
			Method[] ms = clazz.getMethods();
			for (Method mm : ms) {
				if (mm.getName().equals(methodname)) {
					m = mm;
					break;
				}
			}
			// Method m = clazz.getDeclaredMethod(methodname);
			return m.invoke(o, params);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
			return null;
		}
	}

	public static boolean capture = true;

	public Result runUnguardedWithCCL(Class<?> clazz, ClassLoader ccl, String methodname, Object... params)
			throws InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException,
			IllegalArgumentException, InvocationTargetException {
		ClassLoader oldCCL = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(ccl);
			return runUnguarded(clazz, methodname, params);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCCL);
		}
	}

	public Result runUnguarded(Class<?> clazz, String methodname, Object... params) throws InstantiationException,
			IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {

		PrintStream oldo = System.out;
		PrintStream olde = System.err;
		Object result = null;
		ByteArrayOutputStream oso = new ByteArrayOutputStream();
		ByteArrayOutputStream ose = new ByteArrayOutputStream();
		try {
			if (capture) {
				System.setOut(new PrintStream(oso));
				System.setErr(new PrintStream(ose));
			}

			Object o = clazz.newInstance();
			Method m = null;
			Method[] ms = clazz.getMethods();
			for (Method mm : ms) {
				if (mm.getName().equals(methodname)) {
					m = mm;
					break;
				}
			}
			if (m == null) {
				Assert.fail("Invocation failure: could not find method '" + methodname + "' on type '" + clazz.getName());
			}
			m.setAccessible(true);
			result = m.invoke(o, params);
		} finally {
			if (capture) {
				System.setOut(oldo);
				System.setErr(olde);
			}
		}
		return new Result(result, oso.toString().replace("\r", ""), ose.toString().replace("\r", ""));
	}

	public Result runStaticUnguarded(Class<?> clazz, String methodname, Object... params) throws InstantiationException,
			IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {

		PrintStream oldo = System.out;
		PrintStream olde = System.err;
		Object result = null;
		ByteArrayOutputStream oso = new ByteArrayOutputStream();
		ByteArrayOutputStream ose = new ByteArrayOutputStream();
		try {
			if (capture) {
				System.setOut(new PrintStream(oso));
				System.setErr(new PrintStream(ose));
			}

			Method m = null;
			Method[] ms = clazz.getMethods();
			for (Method mm : ms) {
				if (mm.getName().equals(methodname)) {
					m = mm;
					break;
				}
			}
			if (m == null) {
				Assert.fail("Invocation failure: could not find method '" + methodname + "' on type '" + clazz.getName());
			}
			m.setAccessible(true);
			result = m.invoke(null, params);
		} finally {
			if (capture) {
				System.setOut(oldo);
				System.setErr(olde);
			}
		}
		return new Result(result, oso.toString().replace("\r", ""), ose.toString().replace("\r", ""));
	}

	public Result runConstructor(Class<?> clazz, int whichConstructor, Object... params) throws InstantiationException,
			IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {

		PrintStream oldo = System.out;
		PrintStream olde = System.err;
		Object result = null;
		ByteArrayOutputStream oso = new ByteArrayOutputStream();
		ByteArrayOutputStream ose = new ByteArrayOutputStream();
		Constructor<?> c = null;
		try {
			if (capture) {
				System.setOut(new PrintStream(oso));
				System.setErr(new PrintStream(ose));
			}

			Constructor<?>[] cs = clazz.getConstructors();
			c = cs[whichConstructor];
			System.out.println(c);
			if (c == null) {
				Assert.fail("Invocation failure: could not find constructor " + whichConstructor + " on type '" + clazz.getName());
			}
			c.setAccessible(true);
			result = c.newInstance(params);
		} finally {
			if (capture) {
				System.setOut(oldo);
				System.setErr(olde);
			}
		}
		return new Result(result, oso.toString().replace("\r", ""), ose.toString().replace("\r", ""));
	}

	public Result runConstructor(Class<?> clazz, String paramDescriptor, Object... params) throws InstantiationException,
			IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {

		PrintStream oldo = System.out;
		PrintStream olde = System.err;
		Object result = null;
		ByteArrayOutputStream oso = new ByteArrayOutputStream();
		ByteArrayOutputStream ose = new ByteArrayOutputStream();
		try {
			if (capture) {
				System.setOut(new PrintStream(oso));
				System.setErr(new PrintStream(ose));
			}

			Constructor<?>[] cs = clazz.getConstructors();
			Constructor<?> c = null;
			for (Constructor<?> ctor : cs) {
				Class<?>[] paramClazzes = ctor.getParameterTypes();
				String toParamDescriptorString = toParamDescriptorString(paramClazzes);
				//				System.out.println(toParamDescriptorString + "<<");
				if (paramDescriptor.equals(toParamDescriptorString)) {
					c = ctor;
					break;
				}
			}

			if (c == null) {
				Assert.fail("Invocation failure: could not find constructor with param descriptor " + paramDescriptor
						+ " on type '" + clazz.getName());
			}
			c.setAccessible(true);
			result = c.newInstance(params);
		} finally {
			if (capture) {
				System.setOut(oldo);
				System.setErr(olde);
			}
		}
		return new Result(result, oso.toString().replace("\r", ""), ose.toString().replace("\r", ""));
	}

	private String toParamDescriptorString(Class<?>[] paramClazzes) {
		if (paramClazzes == null) {
			return "";
		}
		StringBuilder s = new StringBuilder();
		for (Class<?> c : paramClazzes) {
			s.append(c.getName());
			s.append(" ");
		}
		return s.toString().trim();
	}

	public void runExpectNoSuchMethodException(Class<?> clazz, String methodname, Object... params) throws InstantiationException,
			IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		try {
			runUnguarded(clazz, methodname, params);
			Assert.fail("should not work, NSME should occur for " + methodname);
		} catch (InvocationTargetException ite) {
			String cause = ite.getCause().toString();
			if (!cause.startsWith("java.lang.NoSuchMethodError")) {
				ite.printStackTrace();
				Assert.fail("Should be a NoSuchMethodError, but got " + ite);
			}
		}
	}

	public static boolean printOutput = false;

	/**
	 * Proposed alternate version of runOnInstance that produces a wrapper Exception object similar to the Result object, but where
	 * the "result" is an exception. This is done so as not to lose the grabbed output when exception is raised by the test case.
	 */
	public static Result runOnInstance(Class<?> clazz, Object instance, String methodname, Object... params) throws ResultException {

		PrintStream oldo = System.out;
		PrintStream olde = System.err;
		Object result = null;
		Throwable exception = null;
		ByteArrayOutputStream oso = new ByteArrayOutputStream();
		ByteArrayOutputStream ose = new ByteArrayOutputStream();
		try {
			if (capture) {
				System.setOut(new PrintStream(oso));
				System.setErr(new PrintStream(ose));
			}

			Method m = null;
			Method[] ms = clazz.getMethods();
			for (Method mm : ms) {
				if (mm.getName().equals(methodname)) {
					m = mm;
					break;
				}
			}
			if (m == null) {
				throw new IllegalStateException("No method called " + methodname + " to call on type "
						+ (instance == null ? "null" : instance.getClass().getName()));
			}
			try {
				result = m.invoke(instance, params);
			} catch (Throwable e) {
				exception = e;
			}

		} finally {
			System.setOut(oldo);
			System.setErr(olde);
		}
		if (printOutput) {
			System.out.println("Collected output running: " + methodname);
			System.out.println(oso.toString());
			System.out.println(ose.toString());
		}
		if (exception != null) {
			throw new ResultException(exception, oso.toString().replace("\r", ""), ose.toString().replace("\r", ""));
		} else {
			return new Result(result, oso.toString().replace("\r", ""), ose.toString().replace("\r", ""));
		}
	}

	// attempt definition - kind of a lightweight way to see if it is OK
	public Class<?> loadit(String name, byte[] bytes) {
		try {
			return ((TestClassLoader) binLoader).defineTheClass(name, bytes);
		} catch (RuntimeException t) {
			ClassPrinter.print(bytes);
			t.printStackTrace();
			throw t;
		}
	}

	public Class<?> loadClass(String name) {
		return loadit(name, loadBytesForClass(name));
	}

	protected byte[] retrieveRename(String newName, String name) {
		return ClassRenamer.rename(newName, loadBytesForClass(name));
	}

	/**
	 * retargets are "from.this.thing:to.this.thing"
	 * 
	 * @param newName
	 * @param name
	 * @param retargets of the form "this.from:this.to"
	 * @return
	 */
	protected byte[] retrieveRename(String newName, String name, String... retargets) {
		return ClassRenamer.rename(newName, loadBytesForClass(name), retargets);
	}

	protected byte[] loadBytesForClass(String dottedClassName) {
		byte[] data = Utils.loadDottedClassAsBytes(binLoader, dottedClassName);
		Assert.assertNotNull(data);
		Assert.assertNotSame(0, data.length);
		return data;
	}

	protected String getStamp(String classname) {
		return getStamp(binLoader, classname);
	}

	public static byte[] retrieveClass(ClassLoader loader, String classname) {
		byte[] data = Utils.loadDottedClassAsBytes(loader, classname);
		Assert.assertNotNull(data);
		Assert.assertNotSame(0, data.length);
		return data;
	}

	protected void print(byte[] classdata) {
		ClassReader reader = new ClassReader(classdata);
		reader.accept(new ClassPrinter(System.out), 0);
	}

	protected String printItAndReturnIt(byte[] classdata) {
		return printItAndReturnIt(classdata, false);
	}

	protected String printItAndReturnIt(byte[] classdata, boolean quoted) {
		OutputStream os = new SimpleOutputStream();
		ClassReader reader = new ClassReader(classdata);
		reader.accept(new ClassPrinter(new PrintStream(os)), 0);
		StringBuffer sb = new StringBuffer(os.toString().replace("\r", ""));
		if (!quoted) {
			return sb.toString();
		}
		for (int i = 0; i < sb.length(); i++) {
			if (sb.charAt(i) == '\n') {
				sb.insert(i + 1, "\"");
				sb.insert(i, "\\n\"+");
				i += 4;
			}
		}
		sb.delete(sb.length() - 3, sb.length());
		sb.insert(0, "\"");
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private MethodNode getMethod(byte[] classbytes, String methodName) {
		ClassReader super_cr = new ClassReader(classbytes);
		ClassNode cn = new ClassNode();
		super_cr.accept(cn, 0);
		List<MethodNode> methods = cn.methods;
		if (methods != null) {
			for (MethodNode mn : methods) {
				if (mn.name.equals(methodName)) {
					return mn;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private FieldNode getField(byte[] classbytes, String fieldName) {
		ClassReader super_cr = new ClassReader(classbytes);
		ClassNode cn = new ClassNode();
		super_cr.accept(cn, 0);
		List<FieldNode> fields = cn.fields;
		if (fields != null) {
			for (FieldNode fn : fields) {
				if (fn.name.equals(fieldName)) {
					return fn;
				}
			}
		}
		return null;
	}

	protected ClassNode getClassNode(byte[] classdata) {
		ClassNode cn = new ClassNode();
		ClassReader cr = new ClassReader(classdata);
		cr.accept(cn, 0);	
		return cn;
	}

	@SuppressWarnings("unchecked")
	protected List<MethodNode> getMethods(byte[] classdata) {
		return getClassNode(classdata).methods;
	}
	
	protected int countMethods(byte[] classdata) {
		ClassNode cn = getClassNode(classdata);
		return cn.methods==null?0:cn.methods.size();
	}	

	protected List<MethodNode> filter(List<MethodNode> methods, String nameSubstring) {
		if (methods == null) { return Collections.<MethodNode>emptyList(); }
		List<MethodNode> subset = new ArrayList<MethodNode>();
		for (MethodNode methodNode: methods) {
			if (methodNode.name.contains(nameSubstring)) {
				subset.add(methodNode);
			}
		}
		return subset;
	}
	
	protected String toStringClass(byte[] classdata) {
		return toStringClass(classdata, false, false);
	}

	protected String toStringClass(byte[] classdata, boolean includeBytecode) {
		return toStringClass(classdata, includeBytecode, false);
	}

	protected String toStringClass(byte[] classdata, boolean includeBytecode, boolean quoted) {
		OutputStream os = new SimpleOutputStream();
		ClassPrinter.print(new PrintStream(os), classdata, includeBytecode);
		String s = os.toString();
		StringBuffer sb = new StringBuffer(s.replaceAll("\r", ""));
		if (!quoted) {
			return sb.toString();
		}
		for (int i = 0; i < sb.length(); i++) {
			if (sb.charAt(i) == '\n') {
				sb.insert(i + 1, "\"");
				sb.insert(i, "\\n\"+");
				i += 4;
			}
		}
		sb.insert(0, "\"");
		sb.delete(sb.length() - 3, sb.length());
		return sb.toString();
	}

	protected String toStringMethod(byte[] classdata, String methodname, boolean quoted) {
		OutputStream os = new SimpleOutputStream();
		// ClassReader reader = new ClassReader(classdata);
		MethodNode one = getMethod(classdata, methodname);
		one.instructions.accept(new MethodPrinter(new PrintStream(os)));
		String s = os.toString();
		StringBuffer sb = new StringBuffer(s.replaceAll("\r", ""));
		if (!quoted) {
			return sb.toString();
		}
		for (int i = 0; i < sb.length(); i++) {
			if (sb.charAt(i) == '\n') {
				sb.insert(i + 1, "\"");
				sb.insert(i, "\\n\"+");
				i += 4;
			}
		}
		sb.insert(0, "Method is " + methodname + "\n\"");
		sb.delete(sb.length() - 3, sb.length());
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	protected String toStringField(byte[] classdata, String fieldname) {
		StringBuilder sb = new StringBuilder();
		FieldNode fieldNode = getField(classdata, fieldname);
		if (fieldNode == null) {
			return null;
		}
		List<AnnotationNode> annos = fieldNode.visibleAnnotations;
		if (annos != null) {
			sb.append("vis(").append(toStringAnnotations(annos)).append(") ");
		}
		annos = fieldNode.invisibleAnnotations;
		if (annos != null) {
			sb.append("invis(").append(toStringAnnotations(annos)).append(") ");
		}
		// will need implementing at some point:
		//		List<Attribute> attrs = fieldNode.attrs;
		//		if (attrs = !null) {
		//			sb.append("attrs(").append(toStringAttributes(attrs)).append(") ");
		//		}
		sb.append("0x").append(Integer.toHexString(fieldNode.access)).append("(")
				.append(ClassPrinter.toAccessForMember(fieldNode.access)).append(") ");
		sb.append(fieldNode.name).append(" ");
		sb.append(fieldNode.desc).append(" ");
		if (fieldNode.signature != null) {
			sb.append(fieldNode.signature).append(" ");
		}
		if (fieldNode.value != null) {
			sb.append(fieldNode.value).append(" ");
		}
		return sb.toString().trim();
	}

	private String toStringAnnotations(List<AnnotationNode> annos) {
		StringBuilder sb = new StringBuilder();
		for (AnnotationNode anno : annos) {
			sb.append(toStringAnnotation(anno)).append(" ");
		}
		return sb.toString().trim();
	}

	private String toStringAnnotation(AnnotationNode anno) {
		StringBuilder sb = new StringBuilder();
		sb.append(anno.desc);
		if (anno.values != null) {
			for (int i = 0; i < anno.values.size(); i = i + 2) {
				if (i > 0) {
					sb.append(" ");
				}
				sb.append(toStringAnnotationValue((String) anno.values.get(i), anno.values.get(i + 1)));
			}
		}

		return sb.toString().trim();
	}

	/**
	 * From asm:
	 * 
	 * The name value pairs of this annotation. Each name value pair is stored as two consecutive elements in the list. The name is
	 * a {@link String}, and the value may be a {@link Byte}, {@link Boolean}, {@link Character}, {@link Short}, {@link Integer},
	 * {@link Long}, {@link Float}, {@link Double}, {@link String} or {@link org.objectweb.asm.Type}, or an two elements String
	 * array (for enumeration values), a {@link AnnotationNode}, or a {@link List} of values of one of the preceding types. The list
	 * may be <tt>null</tt> if there is no name value pair.
	 */

	private String toStringAnnotationValue(String name, Object value) {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("=");
		if (value instanceof Byte) {
			sb.append(((Byte) value).byteValue());
		} else if (value instanceof Boolean) {
			sb.append(((Boolean) value).booleanValue());
		} else if (value instanceof Character) {
			sb.append(((Character) value).charValue());
		} else if (value instanceof Short) {
			sb.append(((Short) value).shortValue());
		} else if (value instanceof Integer) {
			sb.append(((Integer) value).intValue());
		} else if (value instanceof Long) {
			sb.append(((Long) value).longValue());
		} else if (value instanceof Float) {
			sb.append(((Float) value).floatValue());
		} else if (value instanceof Double) {
			sb.append(((Double) value).doubleValue());
		} else if (value instanceof String) {
			sb.append(((String) value));
		} else if (value instanceof Type) {
			sb.append(((Type) value).getClassName());
		} else if (value instanceof String[]) {
			String[] ss = (String[]) value;
			sb.append(ss[0]).append(ss[1]);
		} else if (value instanceof AnnotationNode) {
			sb.append(toStringAnnotation((AnnotationNode) value));
		} else if (value instanceof List) {
			throw new IllegalStateException("nyi");
		}
		return sb.toString().trim();
	}

	private static class SimpleOutputStream extends OutputStream {

		StringBuilder sb = new StringBuilder();

		@Override
		public void write(int b) throws IOException {
			sb.append((char) b);
		}

		@Override
		public String toString() {
			return sb.toString();
		}

	}

	protected static String getStamp(ClassLoader loader, String classname) {
		Assert.assertFalse(classname.endsWith(".class"));
		Assert.assertEquals(-1, classname.indexOf('/'));
		URL resourceURL = loader.getResource(classname.replace('.', '/') + ".class");
		System.out.println(resourceURL.getFile());
		return null;
	}

	/**
	 * Look for a <name>.print file and check the printout of the bytes matches it, unless regenerate is true in which case the
	 * print out is recorded in that file.
	 */
	protected void checkIt(String name, byte[] bytes) {
		checkIt(name, bytes, shouldRegenerate());
	}

	/**
	 * Look for a <name>.print file and check the printout of the bytes matches it, unless regenerate is true in which case the
	 * print out is recorded in that file.
	 */
	protected void checkIt(String name, byte[] bytes, boolean regenerate) {
		String filename = "src/test/java/" + name.replace('.', '/') + ".print";
		try {
			if (regenerate) {
				// create the file
				System.out.println("creating " + filename);
				File f = new File(filename);
				FileWriter fos = new FileWriter(f);
				BufferedWriter dos = new BufferedWriter(fos);
				dos.write(printItAndReturnIt(bytes));
				dos.flush();
				fos.close();
			} else {
				// compare the files
				List<String> expectedLines = new ArrayList<String>();
				File f = new File(filename);
				if (!f.exists()) {
					Assert.fail("Must run with renegerate on once to create the expected output for '" + name + "'");
				}
				FileInputStream fis = new FileInputStream(f);
				BufferedReader dis = new BufferedReader(new FileReader(new File(filename)));
				String line = null;
				while ((line = dis.readLine()) != null) {
					if (line.length() != 0) {
						expectedLines.add(line);
					}
				}
				dis.close();
				fis.close();
				List<String> actualLines = toLines(printItAndReturnIt(bytes));
				if (actualLines.size() != expectedLines.size()) {
					System.out.println("actual lines=" + actualLines.size());
					System.out.println("   exp lines=" + expectedLines.size());
					Assert.assertEquals(expectedLines, actualLines);
				}
				for (int ln = 0; ln < expectedLines.size(); ln++) {
					if (!expectedLines.get(ln).equals(actualLines.get(ln))) {
						String expLine = (ln + 1) + " " + expectedLines.get(ln);
						String actLine = (ln + 1) + " " + actualLines.get(ln);
						Assert.assertEquals(expLine, actLine);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected List<String> toLines(String input) {
		StringTokenizer tokenizer = new StringTokenizer(input, "\n\r");
		List<String> output = new ArrayList<String>();
		while (tokenizer.hasMoreElements()) {
			output.add(tokenizer.nextToken());
		}
		return output;
	}

	protected boolean shouldRegenerate() {
		return true;
	}

	protected void copyFile(File from, File to) {
		try {
			FileInputStream fis = new FileInputStream(from);
			FileOutputStream fos = new FileOutputStream(to);
			BufferedInputStream bis = new BufferedInputStream(fis);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			byte[] buffer = new byte[4096];
			int len;
			while ((len = bis.read(buffer, 0, 4096)) != -1) {
				bos.write(buffer, 0, len);
			}
			bis.close();
			bos.close();
		} catch (IOException ioe) {
			throw new RuntimeException("Copy file failed", ioe);
		}
	}

	protected void checkMethod(byte[] bytes, String name, String expected) {
		String actual = toStringMethod(bytes, name, false);
		if (!actual.equals(expected)) {
			// print it out for inclusion in the testcode
			System.out.println(toStringMethod(bytes, name, true));
		}
		Assert.assertEquals(expected, actual);
	}

	protected void checkType(byte[] bytes, String expected) {
		String actual = printItAndReturnIt(bytes, false);
		if (!actual.equals(expected)) {
			// print it out for inclusion in the testcode
			System.out.println(printItAndReturnIt(bytes, true));
		}
		Assert.assertEquals(expected, actual);
	}

	// ---

	// private void loadNewVersion(ReloadableType rtype, String version) throws InstantiationException,
	// IllegalAccessException {
	// loadNewVersion(rtype, version, true);
	// }
	//
	// /**
	// * Loads a new version of a type, given the name of the type, the version (suffix) and the name.
	// */
	// private void loadNewVersion(ReloadableType rtype, String version, boolean log) throws InstantiationException,
	// IllegalAccessException {
	// String name = rtype.getTypeName();
	// byte[] newclassbytes = retrieveClass(name + version);
	// newclassbytes = ClassRenamer.rename(name.replace('.', '/'), newclassbytes);
	// byte[] newdispatcher = DispatcherCreator.createFor(rtype, version);
	// if (log) {
	// System.out.println("DISPATCHER:");
	// print(newdispatcher);
	// }
	// Class<?> newdispatcherclass = loadit(name + "$D$" + version, newdispatcher);
	// byte[] newexecutorbytes = ExecutorCreator.createFor(rtype, version, newclassbytes);
	// if (log) {
	// System.out.println("EXECUTOR:");
	// print(newexecutorbytes);
	// }
	// loadit(name + "$E$" + version, newexecutorbytes);
	// TypeRegistry.poke(name.replace('.', '/'), newdispatcherclass.newInstance());
	// }
	//
	// private void checkvalue(Class<?> clazz, String methodname, Object expectedOutput) {
	// Object value = run(clazz, methodname);
	// if (!value.equals(expectedOutput)) {
	// Assert.fail("Expected " + expectedOutput + ", not " + value);
	// }
	// }

	protected void configureForTesting(TypeRegistry typeRegistry, String... includePatterns) {
		if (includePatterns != null) {
			Properties p = new Properties();
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < includePatterns.length; i++) {
				if (i > 0) {
					s.append(',');
				}
				s.append(includePatterns[i]);
			}
			p.setProperty(TypeRegistry.Key_Inclusions, s.toString());
			typeRegistry.configure(p);
		}
	}

	protected void checkCause(Exception e, Class<? extends Throwable> expectedCause) {
		Throwable t = e.getCause();
		if (t == null || !t.getClass().equals(expectedCause)) {
			e.printStackTrace();
			Assert.fail("Expected cause of " + expectedCause + " but it was " + (t == null ? "null" : t.getClass()));
		}
	}

	public MethodMember grabFrom(List<MethodMember> ms, String name) {
		for (MethodMember m : ms) {
			if (m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}

	public Method grabFrom(Method[] ms, String name) {
		for (Method m : ms) {
			if (m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}

	public Field grabFrom(Field[] fs, String name) {
		for (Field f : fs) {
			if (f.getName().equals(name)) {
				return f;
			}
		}
		return null;
	}

	protected MethodMember findMethod(String toSearchFor, TypeDescriptor typeDescriptor) {
		for (MethodMember method : typeDescriptor.getMethods()) {
			if (method.toString().equals(toSearchFor)) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Create a type registry, configure it with the specified reloadable type/packages and return it.
	 * 
	 * @return new TypeRegistry
	 */
	protected TypeRegistry getTypeRegistry(String includePatterns) {
		TypeRegistry.reinitialize();
		TypeRegistry tr = TypeRegistry.getTypeRegistryFor(binLoader);
		Properties p = new Properties();
		if (includePatterns != null) {
			p.setProperty(TypeRegistry.Key_Inclusions, includePatterns);
		}
		if (tr == null) {
			throw new IllegalStateException(
					"maybe you need to run with: -Dspringloaded=limit=false -Xmx512M -XX:MaxPermSize=256m -noverify");
		}
		tr.configure(p);
		return tr;
	}

	//	protected TypeRegistry getTypeRegistry(String... includePatterns) {
	//		StringBuilder s = new StringBuilder();
	//		for (int i = 0; i < includePatterns.length; i++) {
	//			if (i > 0) {
	//				s.append(',');
	//			}
	//			s.append(includePatterns[i]);
	//		}
	//		return getTypeRegistry(s.toString());
	//	}

	protected TypeRegistry getTypeRegistry() {
		return getTypeRegistry(null);
	}

	/**
	 * Make a type reload itself - this does trigger creation of the dispatcher/executor.
	 */
	protected void reload(ReloadableType reloadableType, String versionstamp) {
		reloadableType.loadNewVersion(versionstamp, reloadableType.bytesInitial);
	}

	@SuppressWarnings("unchecked")
	protected void checkLocalVariables(byte[] bytes, String methodNameAndDescriptor, String... expected) {
		ClassNode cn = new ClassNode();
		ClassReader cr = new ClassReader(bytes);
		cr.accept(cn, 0);

		boolean checked = false;
		List<MethodNode> methods = cn.methods;
		for (MethodNode mn : methods) {
			if (methodNameAndDescriptor.equals(mn.name + mn.desc)) {
				List<LocalVariableNode> localVariables = mn.localVariables;
				Assert.assertEquals(expected.length, localVariables.size());
				for (int i = 0; i < expected.length; i++) {
					StringTokenizer tokenizer = new StringTokenizer(expected[i], ":");
					String expectedName = tokenizer.nextToken();
					String expectedDesc = tokenizer.nextToken();
					LocalVariableNode localVariable = localVariables.get(i);
					Assert.assertEquals(i, localVariable.index);
					Assert.assertEquals(expectedName, localVariable.name);
					Assert.assertEquals(expectedDesc, localVariable.desc);
				}
				checked = true;
			}
		}
		if (!checked) {
			for (MethodNode mn : methods) {
				System.out.println(mn.name + mn.desc);
			}
			Assert.fail("Unable to find method " + methodNameAndDescriptor);
		}
	}

	@SuppressWarnings("unchecked")
	protected void checkAnnotations(byte[] bytes, String methodNameAndDescriptor, String... expected) {
		ClassNode cn = new ClassNode();
		ClassReader cr = new ClassReader(bytes);
		cr.accept(cn, 0);
		if (expected == null) {
			expected = new String[0];
		}

		boolean checked = false;
		List<MethodNode> methods = cn.methods;
		for (MethodNode mn : methods) {
			if (methodNameAndDescriptor.equals(mn.name + mn.desc)) {
				List<AnnotationNode> annotations = mn.visibleAnnotations;
				if (annotations == null) {
					annotations = Collections.emptyList();
				}
				Assert.assertEquals(expected.length, annotations.size());
				for (int i = 0; i < expected.length; i++) {
					//					StringTokenizer tokenizer = new StringTokenizer(expected[i], ":");
					//					String expectedName = tokenizer.nextToken();
					//					String expectedDesc = tokenizer.nextToken();
					AnnotationNode annotation = annotations.get(i);
					Assert.assertEquals(expected[i], toString(annotation));
				}
				checked = true;
			}
		}
		if (!checked) {
			for (MethodNode mn : methods) {
				System.out.println(mn.name + mn.desc);
			}
			Assert.fail("Unable to find method " + methodNameAndDescriptor);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	protected Object toString(AnnotationNode annotation) {
		StringBuilder s = new StringBuilder();
		s.append("@");
		String s2 = annotation.desc.substring(1, annotation.desc.length() - 1);
		s.append(s2.replace('/', '.'));
		s.append("(");
		List values = annotation.values;
		if (values != null) {
			int i = 0;
			while (i < values.size()) {
				String name = (String) values.get(i++);
				Object value = values.get(i++);
				s.append(name).append("=").append(value);
			}
		}
		s.append(")");
		return s.toString();
	}

	protected void assertStartsWith(String prefix, Object value) {
		String stringValue = (String) value;
		if (!stringValue.startsWith(prefix)) {
			Assert.fail("Expected 'regular' toString() but was " + result.returnValue);
		}
	}

	protected void checkDoesNotContain(TypeDescriptor typeDescriptor, String string) {
		if (typeDescriptor.toString().indexOf(string) != -1) {
			Assert.fail("Did not expect to find '" + string + "' in\n" + typeDescriptor);
		}
	}

	protected void checkDoesContain(TypeDescriptor typeDescriptor, String string) {
		if (typeDescriptor.toString().indexOf(string) == -1) {
			Assert.fail("Expected to find '" + string + "' in\n" + typeDescriptor);
		}
	}

	protected ReloadableType loadType(TypeRegistry typeRegistry, String dottedTypeName) {
		return typeRegistry.addType(dottedTypeName, loadBytesForClass(dottedTypeName));
	}

	public Class<?> classForName(String typeName) throws ClassNotFoundException {
		if (typeName.endsWith("[]")) {
			Class<?> element = classForName(typeName.substring(0, typeName.length() - 2));
			return Array.newInstance(element, 0).getClass();
		} else if (typeName.equals("int")) {
			return int.class;
		} else if (typeName.equals("void")) {
			return void.class;
		} else if (typeName.equals("boolean")) {
			return boolean.class;
		} else if (typeName.equals("byte")) {
			return byte.class;
		} else if (typeName.equals("char")) {
			return char.class;
		} else if (typeName.equals("short")) {
			return short.class;
		} else if (typeName.equals("double")) {
			return double.class;
		} else if (typeName.equals("float")) {
			return float.class;
		} else if (typeName.equals("long")) {
			return long.class;
		}
		return Class.forName(typeName, false, binLoader);
	}

	protected Object intArrayToString(Object value) {
		int[] intArray = (int[]) value;
		StringBuilder s = new StringBuilder();
		s.append("{");
		if (intArray != null) {
			for (int j = 0; j < intArray.length; j++) {
				if (j > 0) {
					s.append(",");
				}
				s.append(intArray[j]);
			}
		}
		s.append("}");
		return s.toString();
	}

	protected String objectArrayToString(Object value) {
		Object[] array = (Object[]) value;
		StringBuilder s = new StringBuilder();
		s.append("{");
		if (array != null) {
			for (int j = 0; j < array.length; j++) {
				if (j > 0) {
					s.append(",");
				}
				s.append(array[j]);
			}
		}
		s.append("}");
		return s.toString();
	}

	protected void assertContains(String expectedToBeContained, String actual) {
		if (actual.indexOf(expectedToBeContained) == -1) {
			fail("\nCould not find expected data:\n" + expectedToBeContained + "\n in actual output:\n" + actual);
		}
	}

	protected void assertUniqueContains(String expectedToBeContained, String actual) {
		if (actual.indexOf(expectedToBeContained) == -1
				|| actual.indexOf(expectedToBeContained) != actual.lastIndexOf(expectedToBeContained)) {
			fail("Expected a unique occurrence of:\n" + expectedToBeContained + "\n in actual output:\n" + actual);
		}
	}

	protected void assertDoesNotContain(String expectedToBeContained, String actual) {
		if (actual.indexOf(expectedToBeContained) != -1) {
			fail("Did not expect to find data:\n" + expectedToBeContained + "\n in actual output:\n" + actual);
		}
	}

	protected ISMgr getFieldAccessor(Object o) {
		Class<?> clazz = o.getClass();
		try {
			Field f = clazz.getDeclaredField(Constants.fInstanceFieldsName);
			f.setAccessible(true);
			return (ISMgr) f.get(o);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected String getStaticFieldsMap(Class<?> clazz) {
		try {
			Field f = clazz.getDeclaredField(Constants.fStaticFieldsName);
			f.setAccessible(true);
			SSMgr m = (SSMgr) f.get(null);
			return m.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	PrintStream oldo, olde;
	ByteArrayOutputStream oso, ose;

	/**
	 * Start intercepting the System.out/System.err streams
	 */
	protected void captureOn() {
		oldo = System.out;
		olde = System.err;
		oso = new ByteArrayOutputStream();
		ose = new ByteArrayOutputStream();
		System.setOut(new PrintStream(oso));
		System.setErr(new PrintStream(ose));
	}

	protected static String toSlash(String dottedName) {
		return dottedName.replace('.', '/');
	}

	protected static String toDotted(String slashedName) {
		return slashedName.replace('/', '.');
	}

	/**
	 * Stop intercepting the System.out/System.err streams and return any accumulated output since the captureOn.
	 */
	protected String captureOff() {
		if (oldo == null) {
			throw new IllegalStateException("Turning capture off without having turned it on");
		}
		System.setOut(oldo);
		System.setErr(olde);
		oldo = null;
		olde = null;
		return new String("SYSOUT\n" + oso.toString().replace("\r", "") + "\nSYSERR\n" + ose.toString().replace("\r", "") + "\n");
	}

	protected String captureOffReturnStdout() {
		if (oldo == null) {
			throw new IllegalStateException("Turning capture off without having turned it on");
		}
		System.setOut(oldo);
		System.setErr(olde);
		oldo = null;
		olde = null;
		return new String(oso.toString());
	}

	/**
	 * Called at the end of a test to tidy up in case a test crashed and failed to stop capturing.
	 */
	protected void ensureCaptureOff() {
		if (oldo != null) {
			System.setOut(oldo);
			System.setErr(olde);
			oldo = null;
		}
	}

	/**
	 * Execute a specific method, returning all output that occurred during the run to the caller.
	 */
	public String runMethodAndCollectOutput(Class<?> clazz, String methodname) throws Exception {
		captureOn();
		Method m = clazz.getDeclaredMethod(methodname);
		if (!Modifier.isStatic(m.getModifiers())) {
			fail("Method should be static: " + m);
		}
		m.invoke(null);
		return captureOff();
	}

	protected final static void pause(int seconds) {
		try {
			Thread.sleep(seconds*1000);
		} catch (Exception e) {}
	}
	
	
}
