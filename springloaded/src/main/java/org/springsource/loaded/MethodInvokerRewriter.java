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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.springsource.loaded.ConstantPoolChecker2.References;
import org.springsource.loaded.Utils.ReturnType;
import org.springsource.loaded.ri.ReflectiveInterceptor;


/**
 * Rewrite method calls and field accesses. This is not only references to reloadable types but also calls to reflective APIs which
 * must be intercepted in case they refer to reloadable types at runtime.
 * <p>
 * The MethodInvokerRewriter actually manages a portion of the .slcache - it keeps track of two things:
 * <ul>
 * <li>There is an index file (.index) that records types and whether they were modified on a previous run. A later run can then
 * quickly determine it doesn't need to do anything, or if it should look for a cache file containing the modified form.
 * <li>There is a file 'per-modified-file' that captures the previously determined woven form of the class. The files are named
 * after the classname suffixed with the length of the bytecode (e.g. java_lang_String_3322.bytes). If one of these files exists, we
 * know it contains the appropriate modified bytecode created on a previous run.
 * </ul>
 * The cache is for types that are *only* getting reflection interception done, not for types touching anything reloadable.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class MethodInvokerRewriter {

	private static Logger log = Logger.getLogger(MethodInvokerRewriter.class.getName());

	private static boolean anyNecessaryCacheCleanupDone = false;

	/**
	 * Populated by the file in &lt;cacheDir&gt;/.index - this describes which types were not modified on a previous run and which
	 * files were. If a type is not mentioned then it hasn't been seen before. If the type is mentioned then there will likely be
	 * the bytecode for the modified form on disk in a file named something like 'com/foo/Bar_22233.bytes'- the number is the length
	 * of the unmodified form. The key into this map is the slashed form of the typename suffixed with '_' and the length of the
	 * bytes.
	 */
	private static Map<String, Boolean> cacheIndex = null;

	/**
	 * Rewrite regular operations on reloadable types and any reflective calls.
	 * <p>
	 * Note: no caching is done here (the cache is not read or written to)
	 * 
	 * @param typeRegistry the registry for which the rewriting is being done.
	 * @param bytes the bytes for the type to modify.
	 * @param skipReferencesCheck do we need to do a quick check to see if there is anything worth rewriting?
	 * @return the modified bytes.
	 */
	public static byte[] rewrite(TypeRegistry typeRegistry, byte[] bytes, boolean skipReferencesCheck) {
		ensureCleanupDone();
		return rewrite(false, typeRegistry, bytes, skipReferencesCheck);
	}

	public static byte[] rewrite(TypeRegistry typeRegistry, byte[] bytes) {
		ensureCleanupDone();
		return rewrite(false, typeRegistry, bytes, true);
	}

	private final static boolean DEBUG_CACHING;

	static {
		boolean b = false;
		try {
			b = System.getProperty("springloaded.debugcaching", "false").equalsIgnoreCase("true");
		} catch (Exception e) {
		}
		DEBUG_CACHING = b;
	}

	public static byte[] rewriteUsingCache(String slashedClassName, TypeRegistry typeRegistry, byte[] bytes) {
		ensureCacheIndexLoaded();
		if (DEBUG_CACHING) {
			System.out.println("cache check for " + slashedClassName);
		}
		if (slashedClassName != null) {
			// Construct cachekey, something like: java/lang/String_3343
			String cachekey = new StringBuilder(slashedClassName).append("_").append(bytes.length).toString();
			Boolean b = cacheIndex.get(cachekey);
			if (DEBUG_CACHING) {
				System.out.println("was in index? " + b);
			}
			if (b != null) {
				if (b.booleanValue()) { // the type was modified on an earlier run, there should be cached code around
					String cacheFileName = new StringBuilder(slashedClassName.replace('/', '_')).append("_").append(bytes.length)
							.append(".bytes").toString();
					File cacheFile = new File(GlobalConfiguration.cacheDir, ".slcache" + File.separator + cacheFileName);
					if (DEBUG_CACHING) {
						System.out.println("Checking for cache file " + cacheFile);
					}
					if (cacheFile.exists()) {
						// load the cached file
						if (DEBUG_CACHING) {
							System.out.println("loading and returning cached file contents");
						}
						try {
							FileInputStream fis = new FileInputStream(cacheFile);
							byte[] cachedBytes = Utils.loadBytesFromStream(fis);
							return cachedBytes;
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}
				} else {
					if (DEBUG_CACHING) {
						System.out.println("returning unmodified bytes, no need to change");
					}
					// wasn't modified before, assume it isn't modified now either!
					return bytes;
				}
			}
		}
		if (DEBUG_CACHING) {
			System.out.println("modifying " + slashedClassName);
		}
		// the type has not been seen before or there was no cached file
		return rewrite(true, typeRegistry, bytes, false);
	}

	private static void recursiveDelete(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File f : files) {
					recursiveDelete(f);
				}
			}
		}
		boolean d = file.delete();
		if (DEBUG_CACHING) {
			System.out.println("Deleting " + file + " " + d);
		}
	}

	private static void ensureCleanupDone() {
		if (anyNecessaryCacheCleanupDone) {
			return;
		}
		if (GlobalConfiguration.cleanCache) {
			deleteCacheFiles();
		}
		anyNecessaryCacheCleanupDone = true;
	}

	private static void deleteCacheFiles() {
		// Tidy up!
		File cacheDir = new File(GlobalConfiguration.cacheDir, ".slcache");
		if (cacheDir.exists()) {
			recursiveDelete(cacheDir);
			if (cacheIndex!=null) {
				cacheIndex.clear();
			}
		}
		versionInIndex=false;
	}
	
	private static final int CACHE_VERSION_1_1_0=1;
	private static final int CACHE_VERSION_1_1_1=2;
	private static final int CURRENT_CACHE_VERSION = CACHE_VERSION_1_1_1;
	
	private static boolean versionInIndex = false;

	/**
	 * Load the cache index from the file '&lt;cacheDir&gt;/.index'.
	 * 
	 */
	private static void ensureCacheIndexLoaded() {
		if (cacheIndex == null) {
			cacheIndex = new HashMap<String, Boolean>();
			if (GlobalConfiguration.cleanCache) {
				deleteCacheFiles();
				anyNecessaryCacheCleanupDone = true;
			} else {
				File cacheDir = new File(GlobalConfiguration.cacheDir, ".slcache");
				cacheDir.mkdir();
				File cacheIndexFile = new File(cacheDir, ".index");
				if (cacheIndexFile.exists()) {
					try {
						boolean clearCache = false;
						int cacheVersion = CACHE_VERSION_1_1_0; // if no version tag, assume 1.1.0
						try {
							FileReader fr = new FileReader(cacheIndexFile);
							BufferedReader br = new BufferedReader(fr);
							String line;
							boolean handledVersionString = false;
							while ((line = br.readLine()) != null) {
								if (!handledVersionString) {
									if (line.startsWith("Version")) {
										int colon = line.indexOf(":");
										if (colon!=-1) {
											cacheVersion = Integer.parseInt(line.substring(colon+1));
										}
										
										handledVersionString = true;
										continue;
									}
								}
								StringTokenizer st = new StringTokenizer(line, ":");
								boolean changed = st.nextToken().equals("y");
								String len = st.nextToken();
								String classname = st.nextToken();
								String key = new StringBuilder(classname).append("_").append(len).toString();
								// System.out.println("Populating cache index:" + key + "=" + changed);
								cacheIndex.put(key, changed);
							}
							fr.close();
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
						if (cacheVersion!=CURRENT_CACHE_VERSION) {
							clearCache=true;
							versionInIndex=false;
						} else {
							versionInIndex=true;
						}
						if (clearCache) {
							if (DEBUG_CACHING) {
								System.out.println("SpringLoaded: cache looks old (version "+cacheVersion+") - clearing it");
							}
							deleteCacheFiles();
							anyNecessaryCacheCleanupDone = true;							
						}
					} catch (NoSuchElementException nsme) {
						// rogue entry in the cache
						if (DEBUG_CACHING) {
							System.out.println("SpringLoaded: cache corrupt, clearing it");
						}
						deleteCacheFiles();
						anyNecessaryCacheCleanupDone = true;
					}
				}
			}
		}
	}

	private static byte[] rewrite(boolean canCache, TypeRegistry typeRegistry, byte[] bytes, boolean skipReferencesCheck) {

		// v1 - just looks at classes, if it sees jlClass or a jlr type it has to be cautious and assume a 
		// rewrite is necessary:
		//		List<String> classes = ConstantPoolChecker.getReferencedClasses(bytes);
		//		//		System.out.println(classes);
		//		long mtime = System.nanoTime();
		//		boolean needsRewriting = false;
		//		for (String clazz : classes) {
		//			if (typeRegistry.isReloadableTypeName(clazz)) {
		//				needsRewriting = true;
		//				break;
		//			} else if (clazz.length() > 10 && clazz.charAt(8) == 'g'
		//					&& (clazz.startsWith("java/lang/reflect/") || clazz.equals("java/lang/Class"))) {
		//				needsRewriting = true;
		//				break;
		//			}
		//		}
		//		if (!needsRewriting) {
		//			return bytes;
		//		}

		// v2 - using the CPC2, this also knows about methods that are being used so is much more precise
		// and never has to guess if something needs a rewrite
		if (!skipReferencesCheck) {
			References refs = ConstantPoolChecker2.getReferences(bytes);
			boolean needsRewriting = false;
			for (String clazz : refs.referencedClasses) {
				if (typeRegistry != null && typeRegistry.isReloadableTypeName(clazz)) {
					needsRewriting = true;
					break;
				} else if (clazz.length() > 10 && clazz.charAt(0) == 'j'
						&& (clazz.startsWith("java/lang/reflect/") || clazz.equals("java/lang/Class"))) {
					// Need a closer look
					boolean foundMethodCandidate = false;
					for (String classPlusMethod : refs.referencedMethods) {
						if (RewriteClassAdaptor.intercepted.contains(classPlusMethod)) {
							foundMethodCandidate = true;
							break;
						}
					}
					if (foundMethodCandidate) {
						needsRewriting = true;
						break;
					}
				}
			}
			if (!needsRewriting) {
				addToCacheIndex(refs.slashedClassName, bytes, false);
				return bytes;
			}
		}

		// Now we know the bytes contained something we need to rewrite:
		ClassReader fileReader = new ClassReader(bytes);
		RewriteClassAdaptor classAdaptor = new RewriteClassAdaptor(typeRegistry);
		try {// TODO always skip frames? or just for javassist things?
			fileReader.accept(classAdaptor, ClassReader.SKIP_FRAMES);
		} catch (DontRewriteException drex) {
			return bytes;
		}
		//		System.out.println((System.currentTimeMillis() - stime) + " rewrote " + classAdaptor.classname);
		byte[] bs = classAdaptor.getBytes();
		//		System.out.println(classAdaptor.slashedclassname + "  rewrite info:  rewroteReflection=" + classAdaptor.rewroteReflection
		//				+ " rewroteOtherKind=" + classAdaptor.rewroteOtherKindOfOperation);
		// checkNotTheSame(bs, bytes);
		if (canCache && classAdaptor.rewroteReflection && !classAdaptor.rewroteOtherKindOfOperation) {
			if (GlobalConfiguration.isCaching) {
				// Do not cache generated proxy classes
				if (!classAdaptor.slashedclassname.startsWith("com/sun/proxy/$Proxy")) {
					cacheOnDisk(classAdaptor.slashedclassname, bytes, bs);
				}
			}
		}
		return bs;
	}

	private static synchronized void cacheOnDisk(String slashedclassname, byte[] originalBytes, byte[] newbytes) {
		if (!GlobalConfiguration.isCaching) {
			return;
		}
		File cacheDir = new File(GlobalConfiguration.cacheDir, ".slcache");
		cacheDir.mkdir();
		File cacheFile = new File(cacheDir, slashedclassname.replace('/', '_') + "_" + originalBytes.length + ".bytes");
		if (DEBUG_CACHING) {
			System.out.println("Caching " + slashedclassname + " in " + cacheFile);
		}
		try {
			//			System.out.println("Creating cache file " + cacheFile);
			FileOutputStream fos = new FileOutputStream(cacheFile);
			fos.write(newbytes, 0, newbytes.length);
			fos.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		addToCacheIndex(slashedclassname, originalBytes, true);
	}

	private static void addToCacheIndex(String slashedclassname, byte[] bytes, boolean changed) {
		if (!GlobalConfiguration.isCaching) {
			return;
		}
		File cacheDir = new File(GlobalConfiguration.cacheDir, ".slcache");
		cacheDir.mkdir();
		File cacheIndexFile = new File(cacheDir, ".index");
		try {
			FileWriter fw = new FileWriter(cacheIndexFile, true);
			BufferedWriter bw = new BufferedWriter(fw);
			if (!versionInIndex) {
				versionInIndex = true;
				bw.write("Version:"+CURRENT_CACHE_VERSION+"\n");
			}
			bw.write((changed ? "y" : "n") + ":" + bytes.length + ":" + slashedclassname + "\n");
			bw.flush();
			fw.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	// method useful when debugging
	@SuppressWarnings("unused")
	private static void checkNotTheSame(byte[] bs, byte[] bytes) {
		if (bs.length == bytes.length) {
			System.out.println("same length!");
			boolean same = true;
			for (int i = 0; i < bs.length; i++) {
				if (bs[i] != bytes[i]) {
					same = false;
					break;
				}
			}
			if (same) {
				System.out.println("same data!!");
			} else {
				System.out.println("diff data");
			}
		} else {
			System.out.println("different");
		}
	}

	//	public static boolean summarize(RewriteClassAdaptor classAdaptor) {
	//		boolean somethingHappened = true;
	//		StringBuilder s = new StringBuilder();
	//		s.append((System.nanoTime() - stime) + " ns spent modifying " + classAdaptor.classname + " [");
	//		s.append("fields=").append(classAdaptor.visitingFieldOpOnNonReloadableType).append(":");
	//		s.append(classAdaptor.visitingFieldOpOnReloadableType).append(" methods=");
	//		s.append(classAdaptor.visitingMethodOpOnNonReloadableType).append(":")
	//				.append(classAdaptor.visitingMethodOpOnReloadableType);
	//		s.append(" reflect=").append(classAdaptor.lookingToInterceptReflection).append(":").append(classAdaptor.rewroteReflection)
	//				.append("]");
	//		if ((classAdaptor.visitingMethodOpOnReloadableType + classAdaptor.visitingFieldOpOnReloadableType + classAdaptor.rewroteReflection) == 0) {
	//			s.append(" NOTHING HAPPENED");
	//			somethingHappened = false;
	//		}
	//		System.out.println(s.toString());
	//		return somethingHappened;
	//	}

	@SuppressWarnings("serial")
	static class DontRewriteException extends RuntimeException {
	}

	static class RewriteClassAdaptor extends ClassVisitor implements Opcodes {

		private ClassVisitor cw;

		// List of reflective looking calls we don't have to intercept - either
		// because we intercept
		// whatever they were going to do with the return value or the object
		// which they are asking
		// is correct and we don't need to reply with something else (eg.
		// Method.getDeclaringType()
		// for a reloadable method). If calls to reflective looking methods are
		// not intercepted and
		// not listed here, there will be a log event produced to indicate a
		// decision needs to be
		// made about them.
		// @formatter:off
		static final String[] ignored = new String[] {
				// TODO: [...] the list below is dependent on assumptions about
				// what may / may not be changed
				// when reloading. We should make explicit precisely what the
				// underluing assumptions are
				// (e.g. by implementing checks when reloading types to see if
				// any of the assumptions are
				// being violated.)

				"Array.",
				"GenericArrayType.",
				"InvocationTargetException.",
				"MalformedParameterizedTypeException.",
				"Modifier.",
				"ParameterizedType.",
				"UndeclaredThrowableException.",
				"WildcardType.",
				"TypeVariable.",

				"AccessibleObject.isAccessible",
				"AccessibleObject.setAccessible",

				"Class.asSubclass",
				"Class.cast",
				"Class.forName",
				"Class.getCanonicalName",
				"Class.getClassLoader",
				"Class.getClasses",
				"Class.getComponentType",
				"Class.getDeclaredClasses",
				"Class.getDeclaringClass",
				"Class.getEnclosingClass",
				"Class.getEnclosingConstructor",
				"Class.getEnclosingMethod",
				"Class.getGenericInterfaces",
				"Class.getGenericSuperclass",
				"Class.desiredAssertionStatus",
				"Class.getEnumConstants",
				"Class.getInterfaces",
				"Class.getModifiers",
				"Class.getName",
				"Class.getPackage",
				"Class.getProtectionDomain",
				"Class.getResourceAsStream",
				"Class.getResource",
				"Class.getSuperclass",
				"Class.getSimpleName",
				"Class.getSigners",
				"Class.getTypeParameters",
				"Class.isArray",
				"Class.isAnonymousClass",
				"Class.isAnnotation",
				"Class.isAssignableFrom",
				"Class.isEnum",
				"Class.isInstance",
				"Class.isInterface",
				"Class.isLocalClass",
				"Class.isMemberClass",
				"Class.isPrimitive",
				"Class.isSynthetic",
				"Class.toString",

				"Constructor.equals",
				"Constructor.toString",
				"Constructor.hashCode",
				"Constructor.getModifiers",
				"Constructor.getName", // TODO test
				"Constructor.getDeclaringClass", // TODO test
				"Constructor.getParameterTypes", // TODO test
				"Constructor.getTypeParameters", // TODO test
				"Constructor.isSynthetic", // TODO test
				"Constructor.toGenericString", // TODO test
				"Constructor.getExceptionTypes", // TODO test
				"Constructor.getGenericExceptionTypes", // TODO test
				"Constructor.getGenericParameterTypes", // TODO test
				"Constructor.isVarArgs", // TODO test

				"Field.equals", "Field.getDeclaringClass",
				"Field.getGenericType", "Field.getName", "Field.getModifiers",
				"Field.getType", "Field.hashCode", "Field.isEnumConstant",
				"Field.isSynthetic", "Field.toGenericString", "Field.toString",

				"Member.getDeclaringClass", "Member.getModifiers",
				"Member.getName",

				"Method.equals", "Method.getDeclaringClass",
				"Method.getDefaultValue", "Method.getGenericExceptionTypes",
				"Method.getGenericParameterTypes",
				"Method.getGenericReturnType", "Method.getExceptionTypes",
				"Method.getModifiers", "Method.getName",
				"Method.getParameterTypes", "Method.getReturnType",
				"Method.getTypeParameters", "Method.hashCode",
				"Method.isAccessible", "Method.isBridge", "Method.isSynthetic",
				"Method isVarArgs", "Method.setAccessible",
				"Method toGenericString", "Method.toString",

		};

		private String slashedclassname;

		static final HashSet<String> intercepted = new HashSet<String>();
		static {
			interceptable("java/lang/reflect/AccessibleObject", "getAnnotation");
			interceptable("java/lang/reflect/AccessibleObject",
					"getAnnotations");
			interceptable("java/lang/reflect/AccessibleObject",
					"getDeclaredAnnotations");
			interceptable("java/lang/reflect/AccessibleObject",
					"isAnnotationPresent");

			interceptable("java/lang/reflect/AnnotatedElement", "getAnnotation");
			interceptable("java/lang/reflect/AnnotatedElement",
					"getAnnotations");
			interceptable("java/lang/reflect/AnnotatedElement",
					"getDeclaredAnnotations");
			interceptable("java/lang/reflect/AnnotatedElement",
					"isAnnotationPresent");

			interceptable("java/lang/reflect/Method", "getAnnotation");
			interceptable("java/lang/reflect/Method", "getAnnotations");
			interceptable("java/lang/reflect/Method", "getDeclaredAnnotations");
			interceptable("java/lang/reflect/Method", "getParameterAnnotations");
			interceptable("java/lang/reflect/Method", "invoke");
			interceptable("java/lang/reflect/Method", "isAnnotationPresent");

			interceptable("java/lang/reflect/Constructor", "getAnnotation");
			interceptable("java/lang/reflect/Constructor", "getAnnotations");
			interceptable("java/lang/reflect/Constructor",
					"getDeclaredAnnotations");
			interceptable("java/lang/reflect/Constructor",
					"getParameterAnnotations");
			interceptable("java/lang/reflect/Constructor",
					"isAnnotationPresent");
			interceptable("java/lang/reflect/Constructor", "newInstance");

			interceptable("java/lang/reflect/Field", "getAnnotation");
			interceptable("java/lang/reflect/Field", "getAnnotations");
			interceptable("java/lang/reflect/Field", "getDeclaredAnnotations");
			interceptable("java/lang/reflect/Field", "isAnnotationPresent");

			interceptable("java/lang/reflect/Field", "get");

			interceptable("java/lang/reflect/Field", "getBoolean");
			interceptable("java/lang/reflect/Field", "getByte");
			interceptable("java/lang/reflect/Field", "getShort");
			interceptable("java/lang/reflect/Field", "getChar");
			interceptable("java/lang/reflect/Field", "getInt");
			interceptable("java/lang/reflect/Field", "getLong");
			interceptable("java/lang/reflect/Field", "getFloat");
			interceptable("java/lang/reflect/Field", "getDouble");

			interceptable("java/lang/reflect/Field", "set");

			interceptable("java/lang/reflect/Field", "setBoolean");
			interceptable("java/lang/reflect/Field", "setByte");
			interceptable("java/lang/reflect/Field", "setChar");
			interceptable("java/lang/reflect/Field", "setDouble");
			interceptable("java/lang/reflect/Field", "setFloat");
			interceptable("java/lang/reflect/Field", "setInt");
			interceptable("java/lang/reflect/Field", "setLong");
			interceptable("java/lang/reflect/Field", "setShort");

			interceptable("java/lang/Class", "getAnnotation");
			interceptable("java/lang/Class", "getAnnotations");
			interceptable("java/lang/Class", "getField");
			interceptable("java/lang/Class", "getFields");
			interceptable("java/lang/Class", "getDeclaredAnnotations");
			interceptable("java/lang/Class", "getConstructors");
			interceptable("java/lang/Class", "getConstructor");
			interceptable("java/lang/Class", "getDeclaredConstructors");
			interceptable("java/lang/Class", "getDeclaredConstructor");
			interceptable("java/lang/Class", "getDeclaredField");
			interceptable("java/lang/Class", "getDeclaredFields");
			interceptable("java/lang/Class", "getDeclaredMethod");
			interceptable("java/lang/Class", "getDeclaredMethods");
			interceptable("java/lang/Class", "getMethod");
			interceptable("java/lang/Class", "getMethods");
			interceptable("java/lang/Class", "getModifiers");
			interceptable("java/lang/Class", "isAnnotationPresent");
			interceptable("java/lang/Class", "newInstance"); // TODO test
			// interceptable("java/lang/Class", "getEnumConstants"); // no need
			// to intercept - the enumConstants array it depends on is cleared
			// on enum reload

		}

		// @formatter:on

		/**
		 * Call this method to declare that a certain method is 'interceptable'. An interceptable method should have a corresponding
		 * interceptor method in {@link ReflectiveInterceptor}. The name and signature of the interceptor will be derived from the
		 * interceptable method.
		 * 
		 * For example, java.lang.Class.getMethod(Class[] params) ==> ReflectiveInterceptor.jlClassGetMethod(Class thiz, Class[]
		 * params)
		 * 
		 * @param owner Slashed class name of the declaring type.
		 * @param methodName Name of the interceptable method.
		 */
		private static void interceptable(String owner, String methodName) {
			String k = new StringBuilder(owner).append(".").append(methodName).toString();
			if (intercepted.contains(k)) {
				throw new IllegalStateException("Attempt to add duplicate entry " + k);
			}
			intercepted.add(k);
		}

		public boolean rewroteReflection = false;
		public boolean rewroteOtherKindOfOperation = false;
		public boolean thisClassIsReloadable = false;

		private static boolean isInterceptable(String owner, String methodName) {
			return intercepted.contains(owner + "." + methodName);
		}

		protected TypeRegistry typeRegistry;
		boolean isEnum = false;
		private boolean isGroovyClosure = false;
		int fieldcount = 0;
		private ReloadableType rtype; // Can be null if rewriting in a non reloadable type

		public RewriteClassAdaptor(TypeRegistry typeRegistry, ClassVisitor classWriter) {
			// TODO should it also compute frames?
			super(ASM5,classWriter);
			cw = cv;
			this.typeRegistry = typeRegistry;
		}

		public RewriteClassAdaptor(TypeRegistry typeRegistry) {
			this(typeRegistry, new ClassWriter(ClassWriter.COMPUTE_MAXS));
		}

		public byte[] getBytes() {
			byte[] bytes = ((ClassWriter) cw).toByteArray();
			return bytes;
		}

		public ClassVisitor getClassVisitor() {
			return cv;
		}
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			this.slashedclassname = name;

			thisClassIsReloadable = typeRegistry != null && typeRegistry.isReloadableTypeName(slashedclassname);
			// can this occur? surely agent is loaded up-top
			if (slashedclassname.startsWith("org/springsource/loaded/")) {
				throw new DontRewriteException();
			}
			if (superName.equals("java/lang/Enum")) {
				this.isEnum = true;
			} else if (superName.equals("groovy/lang/Closure")) {
				this.isGroovyClosure = true;
			}
		}

		public FieldVisitor visitField(final int access, final String name, final String desc, final String signature,
				final Object value) {
			fieldcount++;
			return super.visitField(access, name, desc, signature, value);
		}

		@Override
		public MethodVisitor visitMethod(int flags, String name, String descriptor, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(flags, name, descriptor, signature, exceptions);
			return new RewritingMethodAdapter(mv, name);
		}

		class RewritingMethodAdapter extends MethodVisitor implements Opcodes, Constants {

			// tracks max variable used in a method so we know what we can use
			// safely
			private int max = 0;

			private String methodname; // method being rewritten
			private boolean isClinitOrEnumInit = false;

			public RewritingMethodAdapter(MethodVisitor mv, String methodname) {
				super(ASM5,mv);
				this.methodname = methodname;
				if (isEnum) {
					isClinitOrEnumInit = this.methodname.length() > 2 && this.methodname.charAt(0) == '<'
							&& this.methodname.charAt(1) == 'c';
					if (!isClinitOrEnumInit) {
						isClinitOrEnumInit = this.methodname.startsWith(" enum constant initialization");
					}
				}
			}

			@Override
			public void visitVarInsn(int opcode, int var) {
				if (var > max) {
					if (opcode == LLOAD || opcode == DLOAD || opcode == LSTORE || opcode == DSTORE) {
						max = var + 1;
					} else {
						max = var;
					}
				} else if (var == max) {
					if (opcode == LLOAD || opcode == DLOAD || opcode == LSTORE || opcode == DSTORE) {
						max = var + 1;
					}
				}
				super.visitVarInsn(opcode, var);
			}

			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				if (!GlobalConfiguration.fieldRewriting) {
					super.visitFieldInsn(opcode, owner, name, desc);
					return;
				} else {
					boolean isReloadable = typeRegistry != null
							&& (owner.equals(slashedclassname) ? thisClassIsReloadable : typeRegistry.isReloadableTypeName(owner));
					// boolean isReloadable = typeRegistry != null &&
					// typeRegistry.isReloadableTypeName(owner);
					if (!isReloadable) {
						super.visitFieldInsn(opcode, owner, name, desc);
						return;
					}
					if (opcode == GETSTATIC) {
						if (name.equals("$callSiteArray") || name.equals("$staticClassInfo")) {
							super.visitFieldInsn(opcode, owner, name, desc);
							return;
						}
						if (isEnum && isClinitOrEnumInit && fieldcount > 1000) {
							super.visitFieldInsn(opcode, owner, name, desc);
							return;
						}
						rewriteGETSTATIC(opcode, owner, name, desc);
					} else if (opcode == PUTSTATIC) {
						if (isEnum && isClinitOrEnumInit && fieldcount > 1000) {
							super.visitFieldInsn(opcode, owner, name, desc);
							return;
						}
						rewritePUTSTATIC(opcode, owner, name, desc);
					} else if (opcode == GETFIELD) {
						rewriteGETFIELD(opcode, owner, name, desc);
					} else if (opcode == PUTFIELD) {
						rewritePUTFIELD(opcode, owner, name, desc);
					}
					rewroteOtherKindOfOperation = true;
				}
			}

			// TODO write up how the code looks for these in a comment
			/**
			 * code:
			 * 
			 * <code>
			 * <pre>
			 * boolean b = TypeRegistry.instanceFieldInterceptionRequired(regId|classId,name)
			 * if (b) {
			 *   instance.r$set(newvalue,instance,name)
			 * } else {
			 *   instance.name = newvalue
			 * }
			 * </pre>
			 * </code>
			 */
			private void rewritePUTFIELD(int opcode, String owner, String name, String desc) {
				int classId = typeRegistry.getTypeIdFor(owner, true);
				// Make a call to check if this field operation must be intercepted:
				mv.visitLdcInsn(Utils.toCombined(typeRegistry.getId(), classId));
				mv.visitLdcInsn(name);
				mv.visitMethodInsn(INVOKESTATIC, tRegistryType, mInstanceFieldInterceptionRequired, "(ILjava/lang/String;)Z");
				Label l1 = new Label();
				mv.visitJumpInsn(IFEQ, l1); // IF (false) GOTO l1
				Utils.insertBoxInsns(mv, desc); // box the value if necessary
				mv.visitInsn(SWAP);
				mv.visitInsn(DUP_X1);
				// now stack is: FieldAccessor|newValue|target
				mv.visitLdcInsn(name);
				mv.visitMethodInsn(INVOKESPECIAL, owner, mInstanceFieldSetterName,
						"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V");
				Label l2 = new Label();
				mv.visitJumpInsn(GOTO, l2);
				mv.visitLabel(l1); // Did not need intercepting, do what you were going to do:
				super.visitFieldInsn(opcode, owner, name, desc);
				mv.visitLabel(l2);
			}

			private void rewriteGETFIELD(int opcode, String owner, String name, String desc) {
				// TODO [cglib optimizations] could recognize things that dont
				// change in proxies
				// if (name.equals("CGLIB$CALLBACK_0")) {
				// super.visitFieldInsn(opcode, owner, name, desc);
				// return;
				// }
				int classId = typeRegistry.getTypeIdFor(owner, true);
				// Make a call to check if this field operation must be
				// intercepted
				mv.visitLdcInsn(Utils.toCombined(typeRegistry.getId(), classId));
				mv.visitLdcInsn(name);
				mv.visitMethodInsn(INVOKESTATIC, tRegistryType, mInstanceFieldInterceptionRequired, "(ILjava/lang/String;)Z");
				Label l1 = new Label();
				mv.visitJumpInsn(IFEQ, l1); // IF (false) GOTO l1
				mv.visitInsn(DUP);
				mv.visitLdcInsn(name);
				mv.visitMethodInsn(INVOKESPECIAL, owner, mInstanceFieldGetterName,
						"(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
				if (desc.length() != 1) {
					if (!desc.equals(jlObject)) {
						mv.visitTypeInsn(CHECKCAST, toDescriptor(desc));
					}
				} else {
					Utils.insertUnboxInsns(mv, desc.charAt(0), true);
				}
				Label l2 = new Label();
				mv.visitJumpInsn(GOTO, l2);
				mv.visitLabel(l1);
				super.visitFieldInsn(opcode, owner, name, desc);
				mv.visitLabel(l2);
			}

			private void rewritePUTSTATIC(int opcode, String owner, String name, String desc) {
				// TODO [perf] cache this information for 'us' so lookup not always necessary
				int classId = typeRegistry.getTypeIdFor(owner, true);
				mv.visitLdcInsn(Utils.toCombined(typeRegistry.getId(), classId));
				// Make a call to check if this field operation must be intercepted:
				mv.visitLdcInsn(name);
				mv.visitMethodInsn(INVOKESTATIC, tRegistryType, mStaticFieldInterceptionRequired, "(ILjava/lang/String;)Z");
				Label l1 = new Label();
				mv.visitJumpInsn(IFEQ, l1); // IF (false) GOTO l1
				// top of heap will be the new value
				Utils.insertBoxInsns(mv, desc);
				mv.visitLdcInsn(name);
				mv.visitMethodInsn(INVOKESTATIC, owner, mStaticFieldSetterName, "(Ljava/lang/Object;Ljava/lang/String;)V");
				Label l2 = new Label();
				mv.visitJumpInsn(GOTO, l2);
				mv.visitLabel(l1);
				super.visitFieldInsn(opcode, owner, name, desc);
				mv.visitLabel(l2);
			}

			private void rewriteGETSTATIC(int opcode, String owner, String name, String desc) {
				int classId = typeRegistry.getTypeIdFor(owner, true);
				// Make a call to check if this field operation must be intercepted:
				mv.visitLdcInsn(Utils.toCombined(typeRegistry.getId(), classId));
				mv.visitLdcInsn(name);
				mv.visitMethodInsn(INVOKESTATIC, tRegistryType, mStaticFieldInterceptionRequired, "(ILjava/lang/String;)Z");
				Label l1 = new Label();
				mv.visitJumpInsn(IFEQ, l1); // IF (false) GOTO l1
				// top of heap will be the new value
				mv.visitLdcInsn(name);
				mv.visitMethodInsn(INVOKESTATIC, owner, mStaticFieldGetterName, "(Ljava/lang/String;)Ljava/lang/Object;");
				if (desc.length() != 1) {
					if (!desc.equals(jlObject)) {
						mv.visitTypeInsn(CHECKCAST, toDescriptor(desc));
					}
				} else {
					Utils.insertUnboxInsnsIfNecessary(mv, desc, true);
				}
				Label l2 = new Label();
				mv.visitJumpInsn(GOTO, l2);
				mv.visitLabel(l1);
				super.visitFieldInsn(opcode, owner, name, desc);
				mv.visitLabel(l2);
			}

			private String toDescriptor(String longDescriptor) {
				if (longDescriptor.charAt(0) == '[') {
					return longDescriptor;
				}
				return longDescriptor.substring(1, longDescriptor.length() - 1);
			}

			/**
			 * The big method for intercepting reflection. It is passed what the original code is trying to do (which method it is
			 * calling) and decides:
			 * <ul>
			 * <li>whether to rewrite it
			 * <li>what method should be called instead
			 * </ul>
			 * 
			 * @return true if the call was modified/intercepted
			 */
			private boolean interceptReflection(String owner, String name, String desc) {
				if (isInterceptable(owner, name)) {
					//TODO: [...] this is probably a lot slower than unfolding this check into 
					//  bunch of optimised if cases, but it is also much easier to manage.
					//  It should be possible to write something to generate the optimised 
					//  if's from the contents of the 'interceptable' HashSet.  Measure before optimizing.
					callReflectiveInterceptor(owner, name, desc, mv);
					return true;
				}
				return false;
			}

			int unitializedObjectsCount = 0;

			@Override
			public void visitTypeInsn(final int opcode, final String type) {
				if (opcode == NEW) {
					unitializedObjectsCount++;
				}
				super.visitTypeInsn(opcode, type);
			}

			private String toString(Handle handle) {
				return "handle(tag="+handle.getTag()+",name="+handle.getName()+",desc="+handle.getDesc()+",owner="+handle.getOwner();
			}
			private String toString(Object[] oa) {
				StringBuilder buf = new StringBuilder();
				buf.append("[");
				if (oa!=null) {
					for (Object o:oa) {
						buf.append(" ");
						buf.append(o);
					}
				}
				buf.append("]");
				return buf.toString();
			}
			
			@Override
			public void visitInvokeDynamicInsn(String name, String desc, org.objectweb.asm.Handle bsm, Object... bsmArgs) {
				int classId = typeRegistry.getTypeIdFor(slashedclassname, false);
				if (classId==-1) {
					throw new IllegalStateException();
				}
				// TODO *shudder* what about invoke dynamic calls that target reflective APIs
				boolean handled = false;
				// TODO Perhaps (for sake of my sanity initially) make a distinction here between the general invokedynamic case and the special lambda support case?

				// name=m
				// desc=()Lbasic/LambdaA2$Foo;
				// bsm=handle(tag=6,
				//            name=metafactory,
				//            desc=(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;,
				//            owner=java/lang/invoke/LambdaMetafactory
				// bsmArgs=[ ()I basic/LambdaA2.lambda$run$1()I (6) ()I]
				if (bsm.getTag()==H_INVOKESTATIC) {
//					InvokeDynamic(name=m,desc=()Lbasic/LambdaA$Foo;,bsm=handle(tag=6,name=metafactory,desc=(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;,owner=java/lang/invoke/LambdaMetafactory,bsmArgs=[ ()I basic/LambdaA.lambda$run$0()I (6) ()I]
					System.out.println("InvokeDynamic(name="+name+",desc="+desc+",bsm="+toString(bsm)+",bsmArgs="+toString(bsmArgs));
					// The other invokes use the 'owner' of the target method to determine which type registry should be part of this
					// check. Here the 'owner' is wrapped up in the bootstrap method - as version 1 we can assume the owner is the lambdametafactory
					// which *wont* be getting reloaded - so we already know we don't need to do some jiggery pokery.
					
//					int classId = typeRegistry.getTypeIdFor(owner, true);
					// Call type registry to determine 'can we do what we were going to do?'
					
					// Stack parameters at callsite into object array
					// The name and descriptor (desc) show what the parameters are on the stack

					if (desc.charAt(1)==')') {
						// no params
						mv.visitInsn(ACONST_NULL);
					}
					else {
						Utils.collapseStackToArray(mv, desc);
					}
					
					
					int bsmReferenceId = typeRegistry.recordBootstrapMethod(slashedclassname,bsm,bsmArgs);
					 // Method java/lang/invoke/MethodHandles.lookup:()Ljava/lang/invoke/MethodHandles$Lookup;
					mv.visitLdcInsn(typeRegistry.getId());
					mv.visitLdcInsn(classId);
					mv.visitMethodInsn(INVOKESTATIC,"java/lang/invoke/MethodHandles","lookup","()Ljava/lang/invoke/MethodHandles$Lookup;");
					mv.visitLdcInsn(name+desc); // Ljava/lang/String;
					mv.visitLdcInsn(bsmReferenceId); // I
					mv.visitMethodInsn(INVOKESTATIC, tRegistryType, mPerformInvokeDynamicName, "([Ljava/lang/Object;IILjava/lang/Object;Ljava/lang/String;I)Ljava/lang/Object;");
					handled=true;
					// TODO handle return type
//					mv.visitLdcInsn(Utils.toCombined(typeRegistry.getId(),classId));
//					mv.visitLdcInsn(name+desc);
//					mv.visitLdcInsn(BSM_NUMBER);
//					mv.visitLdcInsn(bsmArgs);
					
					
					
//					// What can we check to see whether it is necessary to intercept this call? Is it the return type of the descriptor? (For when
//					// the bsm is recognizable as for lambda support)
//					mv.visitLdcInsn(Utils.toCombined(typeRegistry.getId(), classId));
//					mv.visitLdcInsn(name + desc);
//					mv.visitMethodInsn(INVOKESTATIC, tRegistryType, mChangedForInvokeVirtualName, "(ILjava/lang/String;)Z");
//					// Return value is the extracted interface to call if there is a
//					// change and it can't be called directly
//
//					// 2. preserve a copy of the return value (new target)
//					// mv.visitInsn(DUP);
//
//					// 3. Was it null?
//					Label l1 = new Label();
//					mv.visitJumpInsn(IFEQ, l1);
//
//					// 4. Not false
//
//					// 5. Store the target implementation of the interface that we
//					// will invoke later
//					// mv.visitVarInsn(ASTORE, max + 1);
//
//					// 6. Package up any parameters
//					if (hasParams) {
//						Utils.collapseStackToArray(mv, desc);
//					}
//
//					// Prepare for the invocation:
//					if (!hasParams) {
//						// [targetInstance]
//						mv.visitInsn(DUP);
//						mv.visitInsn(ACONST_NULL); // no parameters
//						mv.visitInsn(SWAP); // [targetInstance NULL targetInstance]
//					} else {
//						// [targetInstance paramArray]
//						mv.visitInsn(SWAP);
//						mv.visitInsn(DUP_X1); // [targetInstance paramArray
//												// targetInstance]
//					}
//
//					mv.visitLdcInsn(name + desc);
//
//					// calling __execute(params array,this,name+desc)
//					mv.visitMethodInsn(INVOKEVIRTUAL, owner, mDynamicDispatchName, mDynamicDispatchDescriptor);
//
//					insertAppropriateReturn(returnType);
//					Label gotolabel = new Label();
//					mv.visitJumpInsn(GOTO, gotolabel);
//					mv.visitLabel(l1);
//					// mv.visitInsn(POP);
//					// Here is where we end up if the test for changes failed (ie.
//					// there were no changes - just 'do what you were going to do'
//					super.visitMethodInsn(opcode, owner, name, desc);
//					mv.visitLabel(gotolabel);					
					
				}
				else {
					// TODO handle it!
				}
				if (!handled) {
				super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
				}
			}
			
			@Override
			public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
				if (GlobalConfiguration.interceptReflection && rewriteReflectiveCall(opcode, owner, name, desc)) {
					return;
				}
				if (opcode == INVOKESPECIAL) {
					unitializedObjectsCount--;
				}
				if (name.equals("$getCallSiteArray")) {
					super.visitMethodInsn(opcode, owner, name, desc);
					return;
				}
				// TODO [cglib optimizations] could recognize things that dont
				// change in proxies
				// if (name.equals("CGLIB$BIND_CALLBACKS")) {
				// super.visitMethodInsn(opcode, owner, name, desc);
				// return;
				// }
				boolean isReloadable = typeRegistry != null
						&& (owner.equals(slashedclassname) ? thisClassIsReloadable : typeRegistry.isReloadableTypeName(owner));
				if (!isReloadable) {
					super.visitMethodInsn(opcode, owner, name, desc);
					return;
				}
				rewroteOtherKindOfOperation = true;
				boolean hasParams = desc.charAt(1) != ')';
				ReturnType returnType = Utils.getReturnTypeDescriptor(desc);
				// boolean isVoidReturn = returnType.isVoid();
				int classId = typeRegistry.getTypeIdFor(owner, true);
				if (opcode == INVOKESTATIC) {
					rewriteINVOKESTATIC(opcode, owner, name, desc, hasParams, returnType, classId);
				} else if (opcode == INVOKEINTERFACE) {
					rewriteINVOKEINTERFACE(opcode, owner, name, desc, hasParams, returnType, classId);
				} else if (opcode == INVOKEVIRTUAL) {
					rewriteINVOKEVIRTUAL(opcode, owner, name, desc, hasParams, returnType, classId);
				} else if (opcode == INVOKESPECIAL) {
					rewriteINVOKESPECIAL(opcode, owner, name, desc, hasParams, returnType, classId);
				} else {
					Utils.logAndThrow(log, "Failed to rewrite instruction " + Utils.toOpcodeString(opcode) + " in method "
							+ this.methodname);
				}
			}

			/**
			 * Determine if a method call is a reflective call and an attempt should be made to rewrite it.
			 * 
			 * @return true if the call was rewritten
			 */
			private boolean rewriteReflectiveCall(int opcode, String owner, String name, String desc) {
				if (owner.length() > 10 && owner.charAt(0) == 'j'
						&& (owner.startsWith("java/lang/reflect/") || owner.equals("java/lang/Class"))) {
					boolean rewritten = interceptReflection(owner, name, desc);
					if (rewritten) {
						return true;
					}
					// if (GlobalConfiguration.logNonInterceptedReflectiveCalls
					// && !canIgnore(owner, name)) {
					// // Only log those that are not intercepted
					// if (GlobalConfiguration.logging &&
					// log.isLoggable(Level.WARNING)) {
					// log.log(Level.WARNING,
					// "Reflection (not intercepted) from " + owner +
					// " visitMethodInsn "
					// + Utils.toOpcodeString(opcode) + " " + owner + " " + name
					// + " " + desc);
					// }
					// }
				}
				return false;
			}

			/**
			 * Rewrite an INVOKESTATIC instruction.
			 */
			private void rewriteINVOKESTATIC(final int opcode, final String owner, final String name, final String desc,
					boolean hasParams, ReturnType returnType, int classId) {
				// 1. call istcheck(classId|methodId,
				// methodName+methodDescriptor)
				// If it returns 'null' then nothing has changed and the code
				// can run as before. If it is not null
				// then it is the instance of the extracted interface that
				// should be called instead.
				mv.visitLdcInsn(Utils.toCombined(typeRegistry.getId(), classId));
				mv.visitLdcInsn(name + desc);
				mv.visitMethodInsn(INVOKESTATIC, tRegistryType, mChangedForInvokeStaticName,
						"(ILjava/lang/String;)Ljava/lang/Object;");

				// 2. preserve a copy of the return value (new target)
				mv.visitInsn(DUP);

				// 3. Was it null?
				Label l1 = new Label();
				mv.visitJumpInsn(IFNULL, l1);

				// 4. Not null, we need to dispatch to it

				// 5. Store the target implementation of the interface that we will invoke later
				mv.visitTypeInsn(CHECKCAST, Utils.getInterfaceName(owner)); // TODO are checkcasts unnecessary sometimes? this one seems to be
				mv.visitVarInsn(ASTORE, max + 1);

				// 6. Package up any parameters
				if (hasParams) {
					Utils.collapseStackToArray(mv, desc);
				}

				// Prepare for the invocation:
				if (!hasParams) {
					mv.visitVarInsn(ALOAD, max + 1); // dispatcher instance
					mv.visitInsn(ACONST_NULL); // no parameters
					mv.visitInsn(ACONST_NULL); // no instance, static method invocation
				} else {
					mv.visitVarInsn(ALOAD, max + 1); // dispatcher instance
					mv.visitInsn(SWAP); // swap with that params array
					mv.visitInsn(ACONST_NULL); // no instance, static method invocation
				}

				// TODO optimize to index, can we do that? is it worthwhile?
				mv.visitLdcInsn(name + desc);

				// 7. calling __execute(params array,this,name+desc)
				mv.visitMethodInsn(INVOKEINTERFACE, Utils.getInterfaceName(owner), mDynamicDispatchName, mDynamicDispatchDescriptor);
				insertAppropriateReturn(returnType);

				// 8. jump over the original call
				Label gotolabel = new Label();
				mv.visitJumpInsn(GOTO, gotolabel);
				// 9. do what we were going to do
				mv.visitLabel(l1);
				mv.visitInsn(POP);
				super.visitMethodInsn(opcode, owner, name, desc);
				mv.visitLabel(gotolabel);
			}

			/**
			 * Based on the return type, insert the right return instructions. There will be an object on the stack when this method
			 * is called - the object must be either discarded (void), unboxed (primitive) or cast (reference) depending on the
			 * return type.
			 */
			private void insertAppropriateReturn(ReturnType returnType) {
				if (returnType.isVoid()) {
					mv.visitInsn(POP); // throw the result away (it was null)
				} else {
					if (returnType.isPrimitive()) {
						Utils.insertUnboxInsnsIfNecessary(mv, returnType.descriptor, true);
					} else {
						mv.visitTypeInsn(CHECKCAST, returnType.descriptor);
					}
				}
			}

			/**
			 * All we need to do is know if the INVOKEINTERFACE that is about to run is OK to execute.
			 * <p>
			 * Invokeinterface rewriting is done by calling the type registry to see if what we are about to do is OK. The method we
			 * call returns a boolean indicating whether it can be called directly or if we must direct it through the dynamic
			 * dispatch method.
			 * 
			 */
			private void rewriteINVOKEINTERFACE(final int opcode, final String owner, final String name, final String desc,
					boolean hasParams, ReturnType returnType, int classId) {
				// 1. call 'boolean iicheck(classId|methodId, methodName+methodDescriptor)' to see if this needs interception
				mv.visitLdcInsn(Utils.toCombined(typeRegistry.getId(), classId));
				mv.visitLdcInsn(name + desc);
				mv.visitMethodInsn(INVOKESTATIC, tRegistryType, mChangedForInvokeInterfaceName, "(ILjava/lang/String;)Z");

				// 3. if false, do what was going to be done anyway
				Label l1 = new Label();
				mv.visitJumpInsn(IFEQ, l1);

				// 6. Package up any parameters
				if (hasParams) {
					Utils.collapseStackToArray(mv, desc);
				}

				// Prepare for the invocation:
				if (!hasParams) {
					// [targetInstance]
					mv.visitInsn(DUP);
					mv.visitInsn(ACONST_NULL); // no parameters
					mv.visitInsn(SWAP); // [targetInstance NULL targetInstance]
				} else {
					// [targetInstance paramArray]
					mv.visitInsn(SWAP);
					mv.visitInsn(DUP_X1); // [targetInstance paramArray targetInstance]
				}

				mv.visitLdcInsn(name + desc); // [targetInstance paramArray targetInstance nameAndDescriptor]

				// calling __execute(params array, this, name+desc)
				mv.visitMethodInsn(INVOKEINTERFACE, owner, mDynamicDispatchName, mDynamicDispatchDescriptor);

				insertAppropriateReturn(returnType);
				Label gotolabel = new Label();
				mv.visitJumpInsn(GOTO, gotolabel);
				mv.visitLabel(l1);
				// do what we were going to do:
				super.visitMethodInsn(opcode, owner, name, desc);
				mv.visitLabel(gotolabel);
			}

			private void rewriteINVOKEVIRTUAL(final int opcode, final String owner, final String name, final String desc,
					boolean hasParams, ReturnType returnType, int classId) {
				// 1. call icheck(classId|methodId, methodName+methodDescriptor)
				// to see if this needs interception
				mv.visitLdcInsn(Utils.toCombined(typeRegistry.getId(), classId));
				mv.visitLdcInsn(name + desc);
				mv.visitMethodInsn(INVOKESTATIC, tRegistryType, mChangedForInvokeVirtualName, "(ILjava/lang/String;)Z");
				// Return value is the extracted interface to call if there is a
				// change and it can't be called directly

				// 2. preserve a copy of the return value (new target)
				// mv.visitInsn(DUP);

				// 3. Was it null?
				Label l1 = new Label();
				mv.visitJumpInsn(IFEQ, l1);

				// 4. Not false

				// 5. Store the target implementation of the interface that we
				// will invoke later
				// mv.visitVarInsn(ASTORE, max + 1);

				// 6. Package up any parameters
				if (hasParams) {
					Utils.collapseStackToArray(mv, desc);
				}

				// Prepare for the invocation:
				if (!hasParams) {
					// [targetInstance]
					mv.visitInsn(DUP);
					mv.visitInsn(ACONST_NULL); // no parameters
					mv.visitInsn(SWAP); // [targetInstance NULL targetInstance]
				} else {
					// [targetInstance paramArray]
					mv.visitInsn(SWAP);
					mv.visitInsn(DUP_X1); // [targetInstance paramArray
											// targetInstance]
				}

				mv.visitLdcInsn(name + desc);

				// calling __execute(params array,this,name+desc)
				mv.visitMethodInsn(INVOKEVIRTUAL, owner, mDynamicDispatchName, mDynamicDispatchDescriptor);

				insertAppropriateReturn(returnType);
				Label gotolabel = new Label();
				mv.visitJumpInsn(GOTO, gotolabel);
				mv.visitLabel(l1);
				// mv.visitInsn(POP);
				// Here is where we end up if the test for changes failed (ie.
				// there were no changes - just 'do what you were going to do'
				super.visitMethodInsn(opcode, owner, name, desc);
				mv.visitLabel(gotolabel);
			}

			/**
			 * Rewrite an INVOKESPECIAL that has been encountered in the code.
			 * <p>
			 * The basic premise is then simple: call the TypeRegistry to check whether we can make the call we want to make. If we
			 * can then just do it, if we can't then that method will return a dispatcher instance that can handle the method so
			 * package up our parameters and invoke it.
			 */
			private void rewriteINVOKESPECIAL(final int opcode, final String owner, final String name, final String desc,
					boolean hasParams, ReturnType returnType, int classId) {
				if (unitializedObjectsCount == -1 && name.charAt(0) == '<') {
					super.visitMethodInsn(opcode, owner, name, desc);
					return;
				}
				if (!name.equals("<init>") && owner.equals(slashedclassname)) {
					// being used to invoke a private method
					super.visitMethodInsn(opcode, owner, name, desc);
					// the executor builder will sort it out
					return;
				}

				if (name.charAt(0) == '<') {
					// constructor

					if (isEnum && isClinitOrEnumInit && fieldcount > 1000 && owner.equals(slashedclassname)) {
						super.visitMethodInsn(opcode, owner, name, desc);
						return;
					}

					// Ask for the relevant dispatcher to call:

					mv.visitLdcInsn(Utils.toCombined(typeRegistry.getId(), classId));
					mv.visitLdcInsn(desc);
					mv.visitMethodInsn(INVOKESTATIC, tRegistryType, mChangedForConstructorName,
							"(ILjava/lang/String;)Ljava/lang/Object;");
					mv.visitInsn(DUP);

					// 3. Was it null?
					Label l1 = new Label();
					mv.visitJumpInsn(IFNULL, l1);
					// if it is null, jump over this and do what you originally wanted to do.
					// It if is non-null, the fun begins

					// bytecode we have encountered is something like this:
					// NEW ctors/Callee2
					// DUP
					// LDC "abcde"
					// INVOKESPECIAL ctors/Callee2.<init>(Ljava/lang/String;)V
					// ARETURN

					// Pack up the arguments we were going to pass into the
					// constructor
					mv.visitTypeInsn(CHECKCAST, "org/springsource/loaded/__DynamicallyDispatchable");
					mv.visitVarInsn(ASTORE, max + 1);

					if (hasParams) {
						boolean selfEnumCall = owner.equals(slashedclassname) && isEnum;
						// stack is now the two instances and any params

						Utils.collapseStackToArray(mv, desc);
						// stack is now the two instances and a single 'Object[] params'
						mv.visitInsn(SWAP);
						// stack is now an instance then the 'Object[] params', then the instance
						mv.visitInsn(DUP_X2);
						// stack is now two instances, then the Object[] then the instance

						// if the target is an enum, we have to ensure the state
						// is passed across to initialize it
						if (selfEnumCall) {
							// TODO ok this is a bit hairy and needs tidying up basically the two values we want to pass to the
							// special constructor are in the array at index 0 and 1

							// Want entry 0 and 1 from the params array

							mv.visitInsn(SWAP); // now params array on top instance underneath
							mv.visitInsn(DUP_X1); // give us an array instance to retrieve 1 from
							mv.visitInsn(DUP); // give us an array instance to retrieve 0 from
							mv.visitLdcInsn(0);
							mv.visitInsn(AALOAD);
							mv.visitInsn(SWAP);
							mv.visitLdcInsn(1);
							mv.visitInsn(AALOAD);
							Utils.insertUnboxInsns(mv, 'I', true);

							mv.visitInsn(ACONST_NULL);
							mv.visitMethodInsn(INVOKESPECIAL, owner, "<init>", "(Ljava/lang/String;ILorg/springsource/loaded/C;)V");
						} else if (owner.contains("_closure")) { // TODO need more robust way to identify when target is a closure?
							mv.visitInsn(SWAP); // now params array on top instance underneath
							mv.visitInsn(DUP_X1); // give us an array instance to retrieve 1 from
							mv.visitInsn(DUP); // give us an array instance to retrieve 0 from
							mv.visitLdcInsn(0);
							mv.visitInsn(AALOAD);
							mv.visitInsn(SWAP);
							mv.visitLdcInsn(1);
							mv.visitInsn(AALOAD);
							mv.visitInsn(ACONST_NULL);
							mv.visitMethodInsn(INVOKESPECIAL, owner, "<init>",
									"(Ljava/lang/Object;Ljava/lang/Object;Lorg/springsource/loaded/C;)V");
						} else {
							mv.visitInsn(ACONST_NULL);
							mv.visitMethodInsn(INVOKESPECIAL, owner, "<init>", "(Lorg/springsource/loaded/C;)V");
						}

						// stack is now an instance then the params
						mv.visitVarInsn(ALOAD, max + 1);
						// stack is now an instance then the params then the dispatcher instance
						mv.visitInsn(DUP_X2);
						mv.visitInsn(POP);
						// stack is now the dispatcher instance then the instance then the params
						mv.visitInsn(SWAP);
						// stack is now the dispatcher instance then the params then the instance
						mv.visitLdcInsn(name + desc);
						// stack is now the dispatcher instance, the params, the instance and the name+desc!
					} else {
						// stack is now the two instances
						mv.visitInsn(DUP);
						mv.visitInsn(ACONST_NULL);
						mv.visitMethodInsn(INVOKESPECIAL, owner, "<init>", "(Lorg/springsource/loaded/C;)V");
						// stack is now an instance
						mv.visitVarInsn(ALOAD, max + 1);
						// stack is now an instance then the dispatcher instance
						mv.visitInsn(SWAP);
						mv.visitInsn(ACONST_NULL);
						mv.visitInsn(SWAP);
						// stack is now the dispatcher instance then null then the instance
						mv.visitLdcInsn(name + desc);
						// stack is now the dispatcher instance, null, the instance and the name+desc!
					}
					mv.visitMethodInsn(INVOKEINTERFACE, "org/springsource/loaded/__DynamicallyDispatchable", mDynamicDispatchName,
							mDynamicDispatchDescriptor);
					mv.visitInsn(POP);
					//					mv.visitMethodInsn(INVOKESPECIAL, "ctors/Callee", "<init>", "()V");

					// Follow the usual pattern for rewriting an INVOKESPECIAL
					// 1. Ask for the dispatcher to use for this call
					// 2. if NULL, we can just let it run as before
					// 3. if NON-NULL, we have to invoke our new funkyness:
					// 4. so, call our special ctor on the target that takes a reloadabletype (but pass in null)
					// 5. that will give us an initialized object.
					// 6. call the dispatcher we got back through its dynamic __execute method, this will dispatch
					//    it to the right ___init___ that will now exist in the executor.

					Label gotolabel = new Label();
					mv.visitJumpInsn(GOTO, gotolabel);
					mv.visitLabel(l1);
					mv.visitInsn(POP);
					super.visitMethodInsn(opcode, owner, name, desc);
					mv.visitLabel(gotolabel);

				} else {

					// 1. call ispcheck(classId|methodId, methodName+methodDescriptor) to see if this needs interception
					mv.visitLdcInsn(Utils.toCombined(typeRegistry.getId(), classId));
					mv.visitLdcInsn(name + desc);
					mv.visitMethodInsn(INVOKESTATIC, tRegistryType, mChangedForInvokeSpecialName,
							descriptorChangedForInvokeSpecialName);

					// Return value is the dispatcher instance to call if there is a
					// change such that it can't be called directly - the method we called
					// will have searched for the right one to call

					// 2. preserve a copy of the return value (new target)
					mv.visitInsn(DUP);

					// 3. Was it null?
					Label l1 = new Label();
					mv.visitJumpInsn(IFNULL, l1);

					// 4. Not null, we need to dispatch to the interface

					// stack is the now: originalTarget | params... | newTarget

					// 5. Store the target implementation of the interface that we will invoke later
					mv.visitVarInsn(ASTORE, max + 1);
					// 6. Package up any parameters
					if (hasParams) {
						Utils.collapseStackToArray(mv, desc);
						mv.visitInsn(SWAP);
					}
					mv.visitVarInsn(ASTORE, max + 2);

					// Prepare for the invocation:
					if (!hasParams) {
						mv.visitVarInsn(ALOAD, max + 1); // dispatcher instance
						mv.visitInsn(ACONST_NULL); // no parameters
						mv.visitVarInsn(ALOAD, max + 2); // instance
					} else {
						mv.visitVarInsn(ALOAD, max + 1); // dispatcher instance
						mv.visitInsn(SWAP); // swap with that params array
						mv.visitVarInsn(ALOAD, max + 2); // instance
					}

					mv.visitLdcInsn(name + desc);

					mv.visitMethodInsn(INVOKEINTERFACE, "org/springsource/loaded/__DynamicallyDispatchable", mDynamicDispatchName,
							mDynamicDispatchDescriptor);

					insertAppropriateReturn(returnType);
					Label gotolabel = new Label();
					mv.visitJumpInsn(GOTO, gotolabel);
					mv.visitLabel(l1);
					mv.visitInsn(POP);
					super.visitMethodInsn(opcode, owner, name, desc);
					mv.visitLabel(gotolabel);
				}
			}

			// /**
			// * We will log calls to reflective apis that were not intercepted,
			// unless this says otherwise.
			// *
			// */
			// private boolean canIgnore(String owner, String name) {
			// int index = owner.lastIndexOf('/');
			// String s = owner.substring(index + 1) + "." + name;
			// for (String is : ignored) {
			// // dot suffix means ignore all methods in this type
			// if (is.endsWith(".")) {
			// if (s.startsWith(is)) {
			// return true;
			// }
			// }
			// if (s.equals(is)) {
			// return true;
			// }
			// }
			// return false;
			// }

			// TODO fix string handling - performance
			private void callReflectiveInterceptor(String owner, String name, String desc, MethodVisitor mv) {
				StringBuilder methodName = new StringBuilder();
				methodName.append(owner.charAt(0));
				int stop = owner.lastIndexOf("/");
				int index = owner.indexOf("/");
				while (index < stop) {
					methodName.append(owner.charAt(index + 1));
					index = owner.indexOf("/", index + 1);
				}
				methodName.append(owner, stop + 1, owner.length());
				methodName.append(Character.toUpperCase(name.charAt(0)));
				methodName.append(name, 1, name.length());
				// return methodName.toString();
				//
				// String[] pieces = owner.split("/");
				// StringBuffer methodName = new StringBuffer();
				// for (int i = 0; i < pieces.length - 1; i++) {
				// methodName.append(pieces[i].charAt(0));
				// }
				// methodName.append(pieces[pieces.length - 1]);
				// methodName.append(Character.toUpperCase(name.charAt(0)));
				// methodName.append(name.substring(1));
				StringBuilder newDescriptor = new StringBuilder("(L").append(owner).append(";").append(desc, 1, desc.length());
				mv.visitMethodInsn(INVOKESTATIC, "org/springsource/loaded/ri/ReflectiveInterceptor", methodName.toString(),
						newDescriptor.toString());
				rewroteReflection = true;

			}

		}
	}

}
