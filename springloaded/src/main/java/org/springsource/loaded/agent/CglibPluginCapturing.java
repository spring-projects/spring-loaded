/*
 * Copyright 2010-2012 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springsource.loaded.agent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.springsource.loaded.Constants;
import org.springsource.loaded.GlobalConfiguration;

/**
 * This bytecode rewriter intercepts calls to generate made in the CGLIB framework and allows us to record what
 * generator is called to create the proxy for some type. The same generator can then be driven again if the type is
 * reloaded.
 *
 * @author Andy Clement
 * @since 0.8.3
 */
public class CglibPluginCapturing extends ClassVisitor implements Constants {

	private static Logger log = Logger.getLogger(CglibPluginCapturing.class.getName());

	public static Map<Class<?>, Object[]> clazzToGeneratorStrategyAndClassGeneratorMap = new HashMap<Class<?>, Object[]>();

	public static Map<Class<?>, Object[]> clazzToGeneratorStrategyAndFastClassGeneratorMap = new HashMap<Class<?>, Object[]>();

	public String prefix;

	public static byte[] catchGenerate(byte[] bytesIn) {
		ClassReader cr = new ClassReader(bytesIn);
		CglibPluginCapturing ca = new CglibPluginCapturing();
		cr.accept(ca, 0);
		byte[] newbytes = ca.getBytes();
		return newbytes;
	}

	private CglibPluginCapturing() {
		super(ASM5, new ClassWriter(0)); // TODO review 0 here
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		// The name could be a repackaged form of cglib:
		// net/sf/cglib/core/AbstractClassGenerator
		// org/springframework/cglib/core/AbstractClassGenerator
		int index = name.indexOf("/cglib");
		prefix = name.substring(0, index);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	public byte[] getBytes() {
		return ((ClassWriter) cv).toByteArray();
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (name.equals("create")) {
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				log.info("intercepting create method");
			}
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			return new CreateMethodInterceptor(mv);
		}
		else if (name.equals("generate")) {
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				log.info("intercepting generate method");
			}
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			return new GenerateMethodInterceptor(mv);
		}
		else {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
	}

	class CreateMethodInterceptor extends MethodVisitor implements Constants {

		public CreateMethodInterceptor(MethodVisitor mv) {
			super(ASM5, mv);
		}

		@Override
		public void visitCode() {
		}

		/**
		 * Recognize a call to 'generate' being made. When we see it add some extra code after it that calls the record
		 * method in this type so that we can remember the generator used (and drive it again later when the related
		 * type is reloaded).
		 */
		@Override
		public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc,
				final boolean itf) {
			super.visitMethodInsn(opcode, owner, name, desc, itf);
			if (name.equals("generate")) {
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
					log.info("intercepting call to generate in create method");
				}
				// Code that calls generate:
				//	ALOAD 0
				//	GETFIELD net/sf/cglib/core/AbstractClassGenerator.strategy : Lnet/sf/cglib/core/GeneratorStrategy;
				//	ALOAD 0
				//	INVOKEINTERFACE net/sf/cglib/core/GeneratorStrategy.generate(Lnet/sf/cglib/core/ClassGenerator;)[B
				mv.visitVarInsn(ALOAD, 0); // AbstractClassGenerator instance
				mv.visitFieldInsn(GETFIELD, prefix + "/cglib/core/AbstractClassGenerator", "strategy",
						"L" + prefix + "/cglib/core/GeneratorStrategy;");
				mv.visitVarInsn(ALOAD, 0); // AbstractClassGenerator instance
				mv.visitMethodInsn(INVOKESTATIC, "org/springsource/loaded/agent/CglibPluginCapturing", "record",
						"(Ljava/lang/Object;Ljava/lang/Object;)V", false);//Lnet/sf/cglib/core/GeneratorStrategy;Lnet/sf/cglib/core/AbstractClassGenerator);");
			}
		}

	}


	class GenerateMethodInterceptor extends MethodVisitor implements Constants {

		public GenerateMethodInterceptor(MethodVisitor mv) {
			super(ASM5, mv);
		}

		@Override
		public void visitCode() {
			mv.visitVarInsn(ALOAD, 0); // AbstractClassGenerator instance
			mv.visitFieldInsn(GETFIELD, prefix + "/cglib/core/AbstractClassGenerator", "strategy",
					"L" + prefix + "/cglib/core/GeneratorStrategy;");
			mv.visitVarInsn(ALOAD, 0); // AbstractClassGenerator instance
			mv.visitMethodInsn(INVOKESTATIC, "org/springsource/loaded/agent/CglibPluginCapturing", "record",
					"(Ljava/lang/Object;Ljava/lang/Object;)V", false);//Lnet/sf/cglib/core/GeneratorStrategy;Lnet/sf/cglib/core/AbstractClassGenerator);");

		}

	}

	/**
	 * The classloader for class artifacts is used to load the generated classes for call sites. We need to rewrite
	 * these classes because they may be either calling something that disappears on a later reload (so need to fail
	 * appropriately) or calling something that isnt there on the first load - in this latter case they are changed to
	 * route the dynamic executor method.
	 *
	 * @param a the GeneratorStrategy being used
	 * @param b the AbstractClassGenerator
	 */
	public static void record(Object a, Object b) {
		//		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
		//			log.info("recording invocation of generate with " + (a == null ? "null" : a.getClass().getName()) + " b="
		//					+ (b == null ? "null" : b.getClass().getName()));
		//		}
		// a is a Lnet/sf/cglib/core/GeneratorStrategy;
		// b is a Lnet/sf/cglib/core/AbstractClassGenerator (or specifically net/sf/cglib/reflect/FastClass$Generator)
		// a is something like 'UndeclaredThrowableStrategy'
		// b is an Enhancer:  namePrefix="example.Simple" superclass=example.Simple
		String generatorName = b.getClass().getName();
		if (generatorName.endsWith(".cglib.proxy.Enhancer")) {
			try {
				Field f = b.getClass().getDeclaredField("superclass");
				f.setAccessible(true);
				Class<?> clazz = (Class<?>) f.get(b);
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
					log.info("recording pair " + clazz.getName() + " > " + b);
				}
				clazzToGeneratorStrategyAndClassGeneratorMap.put(clazz, new Object[] { a, b });
			}
			catch (Throwable re) {
				re.printStackTrace();
			}
		}
		else if (generatorName.endsWith(".cglib.reflect.FastClass$Generator")) {
			try {
				Field f = b.getClass().getDeclaredField("type");
				f.setAccessible(true);
				Class<?> clazz = (Class<?>) f.get(b);
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
					log.info("recording pair (fastclass) " + clazz.getName() + " > " + b);
				}
				clazzToGeneratorStrategyAndFastClassGeneratorMap.put(clazz, new Object[] { a, b });
			}
			catch (Throwable re) {
				re.printStackTrace();
			}
		}
	}
}
