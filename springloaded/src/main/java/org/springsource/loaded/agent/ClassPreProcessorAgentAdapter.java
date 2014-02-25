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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;


/**
 * Class pre-processor.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class ClassPreProcessorAgentAdapter implements ClassFileTransformer {

	private static Logger log = Logger.getLogger(ClassPreProcessorAgentAdapter.class.getName());

	private static SpringLoadedPreProcessor preProcessor;

	private static ClassPreProcessorAgentAdapter instance;

	public ClassPreProcessorAgentAdapter() {
		instance = this;
	}

	static {
		try {
			preProcessor = new SpringLoadedPreProcessor();
			preProcessor.initialize();
		} catch (Exception e) {
			throw new ExceptionInInitializerError("could not initialize JSR163 preprocessor due to: " + e.toString());
		}
	}

	/**
	 * @param loader the defining class loader
	 * @param className the name of class being loaded
	 * @param classBeingRedefined when hotswap is called
	 * @param protectionDomain the ProtectionDomain for the class represented by the bytes
	 * @param bytes the bytecode before weaving
	 * @return the weaved bytecode
	 */
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] bytes) throws IllegalClassFormatException {
		try {
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				log.info("> (loader=" + loader + " className=" + className + ", classBeingRedefined=" + classBeingRedefined
						+ ", protectedDomain=" + (protectionDomain != null) + ", bytes= " + (bytes == null ? "null" : bytes.length));
			}

			// TODO determine if this is the right behaviour for hot code replace:
			// Handling class redefinition (hot code replace) - what to do depends on whether the type is a reloadable type or not
			// If reloadable - return the class as originally defined, and treat this new input data as the new version to make live
			// If not-reloadable - rewrite the call sites and attempt hot code replace

			if (classBeingRedefined != null) {
				// pretend no-one attempted the reload by returning original bytes.  The 'watcher' for the class
				// should see the changes and pick them up.  Should we force it here?
				TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(loader);
				if (typeRegistry == null) {
					return null;
				}
				boolean isRTN = typeRegistry.isReloadableTypeName(className);
				if (isRTN) {
					ReloadableType rtype = typeRegistry.getReloadableType(className, false);
					//				CurrentLiveVersion clv = rtype.getLiveVersion();
					//				String suffix = "0";
					//				if (clv != null) {
					//					suffix = clv.getVersionStamp() + "H";
					//				}
					//				rtype.loadNewVersion(suffix, bytes);
					if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
						log.info("Tricking HCR for " + className);
					}
					return rtype.bytesLoaded; // returning original bytes
				}
				return null;
			}
			// System.err.println("transform(" + loader.getClass().getName() + ",classname=" + className +
			// ",classBeingRedefined=" + classBeingRedefined + ",protectionDomain=" + protectionDomain + ")");
			return preProcessor.preProcess(loader, className, protectionDomain, bytes);
		} catch (Throwable t) {
			new RuntimeException("Reloading agent exited via exception, please raise a jira", t).printStackTrace();
			return bytes;
		}
	}

	public static void reload(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {
		instance.transform(loader, className, classBeingRedefined, protectionDomain, bytes);
	}

}
