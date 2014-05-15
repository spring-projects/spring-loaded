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
package org.springsource.loaded.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.springsource.loaded.Constants;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.MethodInvokerRewriter;
import org.springsource.loaded.TypeRegistry;


/**
 * Related to the groovy support, this modifies a piece of the groovy runtime so SpringLoaded can be in the mix.
 * 
 * @author Andy Clement
 * @since 0.7.3
 */
public class ModifyDefineInClassLoaderForClassArtifactsType extends ClassVisitor implements Constants {

	public ModifyDefineInClassLoaderForClassArtifactsType() {
		super(ASM5,new ClassWriter(0)); // TODO review 0 here
	}

	public byte[] getBytes() {
		return ((ClassWriter) cv).toByteArray();
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (name.equals("define")) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			return new DefineClassModifierVisitor(mv);
		} else {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
	}

	class DefineClassModifierVisitor extends MethodVisitor implements Constants {

		public DefineClassModifierVisitor(MethodVisitor mv) {
			super(ASM5,mv);
		}

		@Override
		public void visitCode() {
			mv.visitVarInsn(ALOAD, 0); // ClassLoaderForClassArtifacts this
			mv.visitVarInsn(ALOAD, 1); // String name
			mv.visitVarInsn(ALOAD, 2); // byte[] bytes
			mv.visitMethodInsn(INVOKESTATIC, "org/springsource/loaded/agent/ModifyDefineInClassLoaderForClassArtifactsType",
					"modify", "(Ljava/lang/ClassLoader;Ljava/lang/String;[B)[B", false);
			mv.visitVarInsn(ASTORE, 2);
		}

	}

	/*
	 * The classloader for class artifacts (org/codehaus/groovy/reflection/ClassLoaderForClassArtifacts) is used to load the 
	 * generated classes for call sites. We need to rewrite these classes
	 * because they may be either calling something that disappears on a later reload (so need to fail appropriately) or calling
	 * something that is not there on the first load - in this latter case they are changed to route the dynamic executor method.
	 */
	public static byte[] modify(ClassLoader classloader, String name, byte[] bytes) {
		ClassLoader parent = classloader.getParent();
		if (parent != null) {
			TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(parent);
			// classloader.getParent() can return null - I've seen it when the target of the call being compiled is
			// java_lang_class$getDeclaredFields (i.e. a system class that cant change, so rewriting is unnecessary...)
			if (typeRegistry != null) {
				bytes = typeRegistry.methodCallRewrite(bytes);
			} else {
				if (GlobalConfiguration.verboseMode) {
					System.out.println("No type registry found for parent classloader: " + parent);
				}
				bytes = MethodInvokerRewriter.rewrite(null, bytes, true);
			}
		} else {
			bytes = MethodInvokerRewriter.rewrite(null, bytes, true);
		}
		return bytes;
	}
}