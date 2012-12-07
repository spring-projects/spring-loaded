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

import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.springsource.loaded.LoadtimeInstrumentationPlugin;
import org.springsource.loaded.ReloadEventProcessorPlugin;


/**
 * What does it do?
 * <p>
 * So far the GroovyPlugin can do two different things - configurable through the 'allowCompilableCallSites' flag.
 * 
 * <p>
 * If the flag is false: The plugin intercepts two of the main system types in groovy and turns OFF call site compilation. Without
 * this compilation the compiler will not be generating classes, it will instead be using reflection all the time. This is simpler
 * to handle (as we intercept reflection) but performance == thesuck.
 * <p>
 * If the flag is true: The plugin leaves groovy to compile call sites. We intercept the define method in the classloader used to
 * define these generated call site classes and ensure they are rewritten correctly. Note there is an alternative here of getting
 * the SpringLoadedPreProcessor to recognize these special classloaders and just instrument them that way. However, if we let the
 * plugin do it it is easier to test!
 * <p>
 * To see the difference in these approaches, check the numbers in the Groovy Benchmark tests.
 * 
 * @author Andy Clement
 * @since 0.7.0
 */
public class GroovyPlugin implements LoadtimeInstrumentationPlugin, ReloadEventProcessorPlugin {

	boolean allowCompilableCallSites = true;

	// GroovySunClassLoader - can make the final field non final so it can be set to null (it is checked as part of the isCompilable methodin the callsitegenerator)
	// CallSiteGenerator - make isCompilable return false, which means we will never generate a direct call to a method that may not yet be on the target
	// implementing LoadtimeInstrumentationPlugin
	public boolean accept(String slashedTypeName, ClassLoader classLoader, ProtectionDomain protectionDomain, byte[] bytes) {
		// TODO take classloader into account?
		if (!allowCompilableCallSites) {
			return slashedTypeName.equals("org/codehaus/groovy/runtime/callsite/GroovySunClassLoader")
					|| slashedTypeName.equals("org/codehaus/groovy/runtime/callsite/CallSiteGenerator");
		} else {
			if (slashedTypeName.equals("org/codehaus/groovy/reflection/ClassLoaderForClassArtifacts")) {
				return true;
			}
		}
		return false;
	}

	public byte[] modify(String slashedClassName, ClassLoader classLoader, byte[] bytes) {
		if (allowCompilableCallSites) {
			return modifyDefineInClassLoaderForClassArtifacts(bytes);
		} else {
			// Deactivate compilation
			if (slashedClassName.equals("org/codehaus/groovy/runtime/callsite/GroovySunClassLoader")) {
				//		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				//			log.info("loadtime modifying " + slashedClassName);
				//		}
				ClassReader cr = new ClassReader(bytes);
				NonFinalizer ca = new NonFinalizer("sunVM");
				//		ClassVisitingConstructorAppender ca = new ClassVisitingConstructorAppender("org/springsource/loaded/agent/SpringPlugin",
				//				"recordInstance");
				cr.accept(ca, 0);
				byte[] newbytes = ca.getBytes();
				return newbytes;
			} else {
				// must be the CallSiteGenerator
				ClassReader cr = new ClassReader(bytes);
				FalseReturner ca = new FalseReturner("isCompilable");
				cr.accept(ca, 0);
				byte[] newbytes = ca.getBytes();
				return newbytes;

			}
		}
	}

	private byte[] modifyDefineInClassLoaderForClassArtifacts(byte[] bytes) {
		ClassReader cr = new ClassReader(bytes);
		ModifyDefineInClassLoaderForClassArtifactsType ca = new ModifyDefineInClassLoaderForClassArtifactsType();
		cr.accept(ca, 0);
		byte[] newbytes = ca.getBytes();
		return newbytes;
	}

	// called by the modified code
	public static void recordInstance(Object obj) {
	}

	// implementing CallbackPlugin
	public void reloadEvent(String typename, Class<?> clazz, String versionsuffix) {
	}

	public boolean shouldRerunStaticInitializer(String typename, Class<?> clazz, String encodedTimestamp) {
		return false;
	}

}
