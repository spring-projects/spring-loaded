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

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springsource.loaded.Constants;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.IsReloadableTypePlugin;
import org.springsource.loaded.LoadtimeInstrumentationPlugin;
import org.springsource.loaded.Log;
import org.springsource.loaded.Plugin;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.SystemClassReflectionRewriter;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.Utils;
import org.springsource.loaded.SystemClassReflectionRewriter.RewriteResult;
import org.springsource.loaded.ri.ReflectiveInterceptor;
import org.springsource.loaded.support.Java8;

/**
 * The entry point for the agent - all classes that can be modified will be passed into preProcess(). They have to be dealt with in
 * one of these ways:
 * <ul>
 * <li>reloadable types need their bytecode rewriting so that they can be modified later
 * <li>'framework' types (not loaded by the system classloader) need their reflection calls rewritten
 * <li>system classes also need their reflection calls modified but in a different way (they cannot have dependencies on types they cannot see)
 * </ul>
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class SpringLoadedPreProcessor implements Constants {

	private static Logger log = Logger.getLogger(SpringLoadedPreProcessor.class.getName());
	private static List<Plugin> plugins = null;

	// Global control to turn off the agent, used when testing
	public static boolean disabled = false;

	// These are system classes that contain reflection code and so need instrumenting when encountered.
	private static List<String> systemClassesContainingReflection;

	// Once the system classes have been encountered and instrumented, they need initialization once they have been defined
	// to the VM.  This records the list of those that have not yet been initialized.
	private Map<String, Integer> systemClassesRequiringInitialization = new HashMap<String, Integer>();

	// Once the first reloadabletype is hit, we can start initializing the system classes with reflective interceptors.
	// Doing it early can lead to hangs
	private static boolean firstReloadableTypeHit = false;

	public void initialize() {
		// When spring loaded is running as an agent, it should not be defining types directly (this setting does not apply to
		// the generated suuport types)
		GlobalConfiguration.directlyDefineTypes = false;
		GlobalConfiguration.fileSystemMonitoring = true;
		systemClassesContainingReflection = new ArrayList<String>();
		// So that jaxb annotations will cause discovery of the correct properties:
		systemClassesContainingReflection.add("com/sun/xml/internal/bind/v2/model/nav/ReflectionNavigator");
		// So that proxies are generated with the right set of methods inside
		systemClassesContainingReflection.add("sun/misc/ProxyGenerator");
		// (at least) the call to getModifiers() needs interception
		systemClassesContainingReflection.add("java/lang/reflect/Proxy");
		// So that javabeans introspection is intercepter
		systemClassesContainingReflection.add("java/beans/Introspector");
		// Don't need this right now, instead we are not removing 'final' from the serialVersionUID
		//		// Need to catch at least the call to access the serialVersionUID made in getDeclaredSUID()
		//		systemClassesContainingReflection.add("java/io/ObjectStreamClass$2");
	}

	/**
	 * Main entry point to Spring Loaded when it is running as an agent. This method will use the classLoader and the class name in
	 * order to determine whether the type should be made reloadable. Non-reloadable types will at least get their call sites
	 * rewritten.
	 * 
	 * @return potentially modified bytes
	 */
	public byte[] preProcess(ClassLoader classLoader, String slashedClassName, ProtectionDomain protectionDomain, byte[] bytes) {
		if (disabled) {
			return bytes;
		}

		// TODO need configurable debug here, ability to dump any code before/after
		for (Plugin plugin : getGlobalPlugins()) {
			if (plugin instanceof LoadtimeInstrumentationPlugin) {
				LoadtimeInstrumentationPlugin loadtimeInstrumentationPlugin = (LoadtimeInstrumentationPlugin) plugin;
				if (loadtimeInstrumentationPlugin.accept(slashedClassName, classLoader, protectionDomain, bytes)) {
					bytes = loadtimeInstrumentationPlugin.modify(slashedClassName, classLoader, bytes);
				}
			}
		}

		tryToEnsureSystemClassesInitialized(slashedClassName);

		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(classLoader);
		
		if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
			logPreProcess(classLoader, slashedClassName, typeRegistry);
		}

		if (typeRegistry == null) { // A null type registry indicates nothing is being made reloadable for the classloader
			if (classLoader == null) { // Indicates loading of a system class
				if (systemClassesContainingReflection.contains(slashedClassName)) {
					try {
						// TODO [perf] why are we not using the cache here, is it because the list is so short?
						RewriteResult rr = SystemClassReflectionRewriter.rewrite(slashedClassName, bytes);
						if (GlobalConfiguration.verboseMode && log.isLoggable(Level.FINER)) {
							log.finer("System class rewritten: name="+slashedClassName+" rewrite summary="+rr.summarize());
						}
						systemClassesRequiringInitialization.put(slashedClassName, rr.bits);
						return rr.bytes;
					} catch (Exception re) {
						re.printStackTrace();
					}
					// This block can help when you suspect there is a system class using reflection and that
					// class isn't on the 'shortlist' (in systemClassesContainingReflection). Currently we skip
					// this for performance, we could make it optional baed on a configuration option
					//	} else {
					//		// We should really track whether this type is using reflection...
					//		if (SystemClassReflectionInvestigator.investigate(slashedClassName, bytes) > 0) {
					//		RewriteResult rr = SystemClassReflectionRewriter.rewrite(slashedClassName, bytes);
					//		System.err.println("Type " + slashedClassName + " rewrite summary: " + rr.summarize());
					//		systemClassesRequiringInitialization.put(slashedClassName, rr.bits);
					//		return rr.bytes;
					//	}
				}
				else if (slashedClassName.equals("java/lang/invoke/InnerClassLambdaMetafactory")) {
					bytes = Java8.enhanceInnerClassLambdaMetaFactory(bytes);
					return bytes;
				}
			}
			return bytes;
		}

		// What happens here? The aim is to determine if the type should be made reloadable.
		// 1. If NO, but something in this classloader might be, then rewrite the call sites.
		// 2. If NO, and nothing in this classloader might be, return the original bytes.
		// 3. If YES, make the type reloadable (including rewriting call sites)

		boolean isReloadableTypeName = typeRegistry.isReloadableTypeName(slashedClassName, protectionDomain, bytes);
		
		if (isReloadableTypeName && GlobalConfiguration.explainMode && log.isLoggable(Level.INFO)) {
			log.info("[explanation] Based on the name, type "+slashedClassName+" is considered to be reloadable");
		}
		
		// logging causes a ClassCircularity problem when reporting on:
		// SL: Type 'org/codehaus/groovy/grails/cli/logging/GrailsConsolePrintStream' is not being made reloadable
//		if (GlobalConfiguration.verboseMode && isReloadableTypeName) {
//			Log.log("Type '"+slashedClassName+"' is preliminarily being considered a reloadable type");
//		}
		if (isReloadableTypeName) {
			if (!firstReloadableTypeHit) {
				firstReloadableTypeHit = true;
				// TODO move into the ctor for ReloadableType so that it can't block loading
				tryToEnsureSystemClassesInitialized(slashedClassName);
			}

			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				log.info("processing " + slashedClassName + " as a reloadable type");
			}

			try {
				// TODO decide one way or the other on slashed/dotted from preprocessor to infrastructure
				String dottedClassName = slashedClassName.replace('/', '.');
				String watchPath = getWatchPathFromProtectionDomain(protectionDomain, slashedClassName);
				if (watchPath == null) {
					// For a CGLIB generated type, we may still need to make the type reloadable.  For example:
					// type: com/vmware/rabbit/ApplicationContext$$EnhancerByCGLIB$$512eb60c 
					// codesource determined to be: file:/Users/aclement/springsource/tc-server-developer-2.1.1.RELEASE/spring-insight-instance/wtpwebapps/hello-rabbit-client/WEB-INF/lib/cglib-nodep-2.2.jar <no signer certificates>
					// But if the type 'com/vmware/rabbit/ApplicationContext' is reloadable, then this should be too
					boolean makeReloadableAnyway = false;
					int cglibIndex = slashedClassName.indexOf("$$EnhancerByCGLIB");

					if (cglibIndex != -1) {
						String originalType = slashedClassName.substring(0, cglibIndex);
						if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
							log.info("Appears to be a CGLIB type, checking if type " + originalType + " is reloadable");
						}
						TypeRegistry currentRegistry = typeRegistry;
						while (currentRegistry != null) {
							ReloadableType originalReloadable = currentRegistry.getReloadableType(originalType);
							if (originalReloadable != null) {
								makeReloadableAnyway = true;
								break;
							}
							currentRegistry = currentRegistry.getParentRegistry();
						}
//						if (typeRegistry.isReloadableTypeName(originalType)) {
//							if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
//								log.info("Type " + originalType + " is reloadable, so making CGLIB type " + slashedClassName
//										+ " reloadable");
//							}
//							makeReloadableAnyway = true;
//						}
					}

					int cglibIndex2 = makeReloadableAnyway?-1:slashedClassName.indexOf("$$FastClassByCGLIB");
					if (cglibIndex2 != -1) {
						String originalType = slashedClassName.substring(0, cglibIndex2);
						if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
							log.info("Appears to be a CGLIB FastClass type, checking if type " + originalType + " is reloadable");
						}
						TypeRegistry currentRegistry = typeRegistry;
						while (currentRegistry != null) {
							ReloadableType originalReloadable = currentRegistry.getReloadableType(originalType);
							if (originalReloadable != null) {
								makeReloadableAnyway = true;
								break;
							}
							currentRegistry = currentRegistry.getParentRegistry();
						}
//						if (typeRegistry.isReloadableTypeName(originalType)) {
//							if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
//								log.info("Type " + originalType + " is reloadable, so making CGLIB type " + slashedClassName
//										+ " reloadable");
//							}
//							makeReloadableAnyway = true;
//						}
					}

					int proxyIndex = makeReloadableAnyway?-1:slashedClassName.indexOf("$Proxy");
					if (proxyIndex == 0 || (proxyIndex > 0 && slashedClassName.charAt(proxyIndex - 1) == '/')) {
						// Determine if the interfaces being implemented are reloadable
						String[] interfacesImplemented = Utils.discoverInterfaces(bytes);
						if (interfacesImplemented != null) {
							for (int i = 0; i < interfacesImplemented.length; i++) {
								TypeRegistry currentRegistry = typeRegistry;
								while (currentRegistry != null) {
									ReloadableType originalReloadable = currentRegistry.getReloadableType(interfacesImplemented[i]);
									if (originalReloadable != null) {
										makeReloadableAnyway = true;
										break;
									}
									currentRegistry = currentRegistry.getParentRegistry();
								}
//								if (typeRegistry.isReloadableTypeName(interfacesImplemented[i])) {
//									makeReloadableAnyway = true;
//								}
							}
						}
					}
					// GRAILS-8098
					// The scaffolding loader will load stuff in this innerloader - if we don't make the types in it reloadable then they will clash
					// with the original (ordinary version) controller loaded by URLClassLoader (e.g. in an istcheck for some type we will
					// not find it in the InnerClassLoader, but find it in the super classloader, and it'll be the wrong one).
					// I wonder if the more general rule should be that
					// all classloaders below one loading reloadable stuff should also load reloadable stuff.
					if (!makeReloadableAnyway && classLoader.getClass().getName().endsWith("GroovyClassLoader$InnerLoader")) {
						makeReloadableAnyway = true;
					}

					if (!makeReloadableAnyway) {
						// can't watch it for updates (it comes from a jar perhaps) so just rewrite call sites and return
						if (GlobalConfiguration.verboseMode) {
							Log.log("Cannot watch "+slashedClassName+": not making it reloadable");
						}
						if (needsClientSideRewriting(slashedClassName)) {
							bytes = typeRegistry.methodCallRewriteUseCacheIfAvailable(slashedClassName, bytes);
						}
						return bytes;
					}
				}
				ReloadableType rtype = typeRegistry.addType(dottedClassName, bytes);
				if (rtype == null && GlobalConfiguration.callsideRewritingOn) {
					// it is not a candidate for being made reloadable (maybe it is an annotation type)
					// but we still need to rewrite call sites.
					bytes = typeRegistry.methodCallRewrite(bytes);
				} else {
					if (GlobalConfiguration.fileSystemMonitoring && watchPath != null) {
						typeRegistry.monitorForUpdates(rtype, watchPath);
					}
					return rtype.bytesLoaded;
				}
			} catch (RuntimeException re) {
				log.throwing("SpringLoadedPreProcessor", "preProcess", re);
				throw re;
			}
		} else {
			try {
				// TODO what happens across classloader boundaries? (for regular code and reflective calls)
				if (needsClientSideRewriting(slashedClassName)) {
					bytes = typeRegistry.methodCallRewriteUseCacheIfAvailable(slashedClassName, bytes);
				}
			} catch (Throwable t) {
				log.log(Level.SEVERE, "Unexpected problem transforming call sites", t);
			}
		}
		return bytes;
	}

	private void tryToEnsureSystemClassesInitialized(String slashedClassName) {
		if (firstReloadableTypeHit && !systemClassesRequiringInitialization.isEmpty()) {
			int lastSlash = slashedClassName.lastIndexOf('/');
			String pkg = lastSlash == -1 ? null : slashedClassName.substring(0, lastSlash);
			ensurePreparedForInjection();
			List<String> toRemoveList = new ArrayList<String>();
			for (Map.Entry<String, Integer> me : systemClassesRequiringInitialization.entrySet()) {
				String classname = me.getKey();
				// A ClassCircularityError can occur in the injectReflectiveInterceptorMethods() method below.  Reason:
				// ===
				// CCE: "A class or interface could not be loaded because it would be its own superclass or superinterface"
				// according to the Java Virtual Machine Specification (JVMS 2.17.2).
				// The implementation of the virtual machine generally detects this by noting the
				// beginning of an attempt to load a class and then noticing when the
				// same task thread attempts to load that same class again before the original
				// attempt has completed (is still in progress).
				// ===
				// So, if we attempt to 'fix up' a class here which has a relationship with the type we are currently
				// loading, then it looks like a CCE.  The crude initial fix is to avoid working on anything in the
				// same package as us.  This doesn't quite fix all the cases of course but addresses a chunk of them.
				// One remaining case I can clearly see in the log is that java.beans.Introspector (which needs fixing up)
				// uses a field of type com.sun.beans.WeakCache.

				// A full list of the special relationships could be encoded here (don't touch X until Y,Z,etc loaded)
				// but that will just get out of date so quickly.  Given that it isn't necessarily a problem because
				// the fixing up will be re-attempted again, the simplest thing would be just to avoid printing
				// CCEs (but log all other issues).
				if (pkg != null && classname.startsWith(pkg)) {
					continue;
				}
				int bits = me.getValue();
				try {
					Class<?> clazz = SpringLoadedPreProcessor.class.getClassLoader().loadClass(classname.replace('/', '.'));
					injectReflectiveInterceptorMethods(slashedClassName, bits, clazz);
					toRemoveList.add(classname);
				} catch (ClassCircularityError cce) {
					// See comment above. 'assume' this is OK, the initialization will happen again next time around.
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for (String toRemove : toRemoveList) {
				systemClassesRequiringInitialization.remove(toRemove); // TODO threads?
			}
		}
	}

	// TODO should cache these retrieved fields/methods for injection into types
	/**
	 * This method tries to inject the ReflectiveInterceptor methods into any system types that have been rewritten.
	 */
	private void injectReflectiveInterceptorMethods(String slashedClassName, int bits, Class<?> clazz) throws NoSuchFieldException,
			IllegalAccessException, NoSuchMethodException {
		// TODO log the bits
		if ((bits & Constants.JLC_GETDECLAREDFIELDS) != 0) {
			Field f = clazz.getDeclaredField("__sljlcgdfs");
			f.setAccessible(true);
			f.set(null, method_jlcgdfs);
		}
		if ((bits & Constants.JLC_GETDECLAREDFIELD) != 0) {
			Field f = clazz.getDeclaredField(jlcgdf);
			f.setAccessible(true);
			f.set(null, method_jlcgdf);
		}
		if ((bits & Constants.JLC_GETFIELD) != 0) {
			Field f = clazz.getDeclaredField(jlcgf);
			f.setAccessible(true);
			f.set(null, method_jlcgf);
		}
		if ((bits & Constants.JLC_GETDECLAREDMETHODS) != 0) {
			Field f = clazz.getDeclaredField(jlcgdms);
			f.setAccessible(true);
			f.set(null, method_jlcgdms);
		}
		if ((bits & Constants.JLC_GETDECLAREDMETHOD) != 0) {
			Field f = clazz.getDeclaredField(jlcgdm);
			f.setAccessible(true);
			f.set(null, method_jlcgdm);
		}
		if ((bits & Constants.JLC_GETMETHOD) != 0) {
			Field f = clazz.getDeclaredField(jlcgm);
			f.setAccessible(true);
			f.set(null, method_jlcgm);
		}
		if ((bits & Constants.JLC_GETDECLAREDCONSTRUCTOR) != 0) {
			Field f = clazz.getDeclaredField(jlcgdc);
			f.setAccessible(true);
			f.set(null, method_jlcgdc);
		}
		if ((bits & Constants.JLC_GETMODIFIERS) != 0) {
			Field f = clazz.getDeclaredField(jlcgmods);
			f.setAccessible(true);
			f.set(null, method_jlcgmods);
		}
		if ((bits & Constants.JLC_GETMETHODS) != 0) {
			Field f = clazz.getDeclaredField(jlcgms);
			f.setAccessible(true);
			f.set(null, method_jlcgms);
		}
		if ((bits & Constants.JLC_GETCONSTRUCTOR) != 0) {
			Field f = clazz.getDeclaredField(jlcgc);
			f.setAccessible(true);
			f.set(null, method_jlcgc);
		}
	}

	private static final Class<?> EMPTY_CLASS_ARRAY_CLAZZ = Class[].class;
	// TODO threads
	private static boolean prepared = false;
	private static Method method_jlcgdfs, method_jlcgdf, method_jlcgf, method_jlcgdms, method_jlcgdm, method_jlcgm, method_jlcgdc,
			method_jlcgc, method_jlcgmods, method_jlcgms;

	/**
	 * Cache the Method objects that will be injected.
	 */
	private void ensurePreparedForInjection() {
		if (!prepared) {
			try {
				Class<ReflectiveInterceptor> clazz = ReflectiveInterceptor.class;
				method_jlcgdfs = clazz.getDeclaredMethod("jlClassGetDeclaredFields", Class.class);
				method_jlcgdf = clazz.getDeclaredMethod("jlClassGetDeclaredField", Class.class, String.class);
				method_jlcgf = clazz.getDeclaredMethod("jlClassGetField", Class.class, String.class);
				method_jlcgdms = clazz.getDeclaredMethod("jlClassGetDeclaredMethods", Class.class);
				method_jlcgdm = clazz.getDeclaredMethod("jlClassGetDeclaredMethod", Class.class, String.class,
						EMPTY_CLASS_ARRAY_CLAZZ);
				method_jlcgm = clazz.getDeclaredMethod("jlClassGetMethod", Class.class, String.class, EMPTY_CLASS_ARRAY_CLAZZ);
				method_jlcgdc = clazz.getDeclaredMethod("jlClassGetDeclaredConstructor", Class.class, EMPTY_CLASS_ARRAY_CLAZZ);
				method_jlcgc = clazz.getDeclaredMethod("jlClassGetConstructor", Class.class, EMPTY_CLASS_ARRAY_CLAZZ);
				method_jlcgmods = clazz.getDeclaredMethod("jlClassGetModifiers", Class.class);
				method_jlcgms = clazz.getDeclaredMethod("jlClassGetMethods", Class.class);
			} catch (NoSuchMethodException nsme) {
				// cant happen, a-hahaha
				throw new Impossible(nsme);
			}
			prepared = true;
		}
	}

	private static boolean needsClientSideRewriting(String slashedClassName) {
		if (slashedClassName!=null && slashedClassName.charAt(0)=='o' && slashedClassName.startsWith("org/springsource/loaded")) {
			return false;
		}
		return true;
	}

	/**
	 * Determine where to watch for changes based on the protectionDomain. Relying on the protectionDomain may prove fragile though,
	 * as it is up to the classloader in question to create it. Some classloaders will create one protectionDomain per 'directory'
	 * containing class files (and so the slashedClassName must be appended to the codesource). Some classloaders have a
	 * protectiondomain per class.
	 * 
	 * @param protectionDomain the protection domain passed in to the defineclass call
	 * @param slashedClassName the slashed class name currently being defined
	 * @return the path to watch for changes to this class
	 */
	private String getWatchPathFromProtectionDomain(ProtectionDomain protectionDomain, String slashedClassName) {
		String watchPath = null;
		//		System.err.println("protectionDomain=" + protectionDomain + " slashedClassName=" + slashedClassName + " protdom="
		//				+ protectionDomain + " codesource=" + (protectionDomain == null ? "null" : protectionDomain.getCodeSource()));
		if (protectionDomain == null) {
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.WARNING)) {
				log.warning("Changes to type cannot be tracked: " + slashedClassName + " - no protection domain");
			}
		} else {
			try {
				CodeSource codeSource = protectionDomain.getCodeSource();
				if (codeSource.getLocation() == null) {
					if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.WARNING)) {
						log.warning("null codesource for " + slashedClassName);
					}
				} else {
					if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINEST)) {
						log.finest("Codesource.getLocation()=" + codeSource.getLocation());
					}
					// A 'URI is not hierarchical' message can come out when the File ctor is called. Cases seen
					// so far: 
					// GRAILS-10384: relative URL file:../foo/bar - should have built it with new File().toURI.toURL() and not just new URL()
					File file = null;
					URI uri = null;
					try {
						uri = codeSource.getLocation().toURI();
						file = new File(uri);
					} catch (IllegalArgumentException iae) {
						boolean recovered = false;
						if (iae.toString().indexOf("URI is not hierarchical")!=-1) {
							// try another approach...
							String uristring = uri.toString();
							if (uristring.startsWith("file:../")) {
								file = new File(uristring.substring(8)).getAbsoluteFile();
							} else if (uristring.startsWith("file:./")) {
								file = new File(uristring.substring(7)).getAbsoluteFile();
							}
							if (file.exists()) {
								recovered = true;
							}
						}
						if (!recovered) {
							System.out.println("Unable to watch file: classname = "+slashedClassName+" codesource location = "+codeSource.getLocation()+" ex = "+iae.toString());
							return null;
						}
					}
					if (file.isDirectory()) {
						file = new File(file, slashedClassName + ".class");
					} else if (file.getName().endsWith(".class")) {
						// great! nothing to do
					} else if (file.getName().endsWith(".jar")) {
						if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.WARNING)) {
							log.warning("unable to watch this jar file entry: " + slashedClassName.replace('/', '.')
									+ ". Computed location=" + file.toString());
						}
						return null;
					} else if (file.toString().equals("/groovy/script") || file.toString().equals("\\groovy\\script")) {
						// nothing to do, compiled/loaded by a GroovyClassLoader$InnerLoader - there is nothing to watch.  If the type is to be
						// reloaded we will have to be told via an alternate route
						return null;
					} else if (!file.toString().endsWith(".class")) {
						// GRAILS-9076: it ended in .groovy
						// GRAILS-9069/GRAILS-9070: it was /groovy/shell
						// something other than a class, no point in watching it
						return null;
					} else {
						throw new UnsupportedOperationException("unable to watch " + slashedClassName.replace('/', '.')
								+ ". Computed location=" + file.toString());
					}
					watchPath = file.toString();
					if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
						log.info("Watched location for changes to " + slashedClassName + " is " + watchPath);
					}
				}
			} catch (URISyntaxException e) {
				throw new IllegalStateException("Unexpected problem processing URI ", e);
			}
		}
		return watchPath;
	}

	private void logPreProcess(ClassLoader classLoader, String slashedClassName, TypeRegistry typeRegistry) {
		String clname = classLoader == null ? "null" : classLoader.getClass().getName();
		if (clname.indexOf('.') != -1) {
			clname = clname.substring(clname.lastIndexOf('.') + 1);
		}
		log.info("SpringLoaded preprocessing: classname="+slashedClassName+" classloader="+clname+" typeRegistry="+typeRegistry);
	}

	public static List<Plugin> getGlobalPlugins() {
		if (plugins == null) {
			plugins = new ArrayList<Plugin>();
			// Ordering is important here (for some of the plugins) - try to do the lowest level things first in case the higher level
			// operations cause something to happen that will drive the lower level function.  For example, the JVM plugin clears the
			// Introspector class which is used by the Spring CachedIntrospectionResults class, which is used by the Grails ClassPropertyFetcher (
			// through its calls to BeanUtils).  If you don't clear the lower level things first then the higher level reinit operations will
			// still see the old (incorrect) results.
			plugins.add(new JVMPlugin());
			plugins.add(new SpringPlugin());
			plugins.add(new GroovyPlugin());
			plugins.add(new CglibPlugin());
			// Not used right now, grails mechanisms are clearing the state that this plugin is trying to
			// plugins.add(new GrailsPlugin());
			List<String> extraGlobalPlugins = GlobalConfiguration.pluginClassnameList;
			if (extraGlobalPlugins != null) {
				for (String globalPlugin : extraGlobalPlugins) {
					try {
						Class<?> pluginClass = Class.forName(globalPlugin, false, SpringLoadedPreProcessor.class.getClassLoader());
						plugins.add((Plugin) pluginClass.newInstance());
					} catch (ClassNotFoundException e) {
						System.err.println("Unexpected problem loading global plugin:" + globalPlugin);
						e.printStackTrace(System.err);
					} catch (InstantiationException e) {
						System.err.println("Unexpected problem loading global plugin:" + globalPlugin);
						e.printStackTrace(System.err);
					} catch (IllegalAccessException e) {
						System.err.println("Unexpected problem loading global plugin:" + globalPlugin);
						e.printStackTrace(System.err);
					}
				}
			}
		}
		return plugins;
	}

	private static List<IsReloadableTypePlugin> isReloadableTypePlugins = null;

	public static List<IsReloadableTypePlugin> getIsReloadableTypePlugins() {
		if (isReloadableTypePlugins == null) {
			synchronized (SpringLoadedPreProcessor.class) {
				if (isReloadableTypePlugins == null) {
					isReloadableTypePlugins = new ArrayList<IsReloadableTypePlugin>();
					for (Plugin p : getGlobalPlugins()) {
						if (p instanceof IsReloadableTypePlugin) {
							isReloadableTypePlugins.add((IsReloadableTypePlugin) p);
						}
					}
				}
			}
		}
		return isReloadableTypePlugins;
	}

	public static void registerGlobalPlugin(Plugin instance) {
		getGlobalPlugins(); // trigger initialization
		plugins.add(instance);
		isReloadableTypePlugins = null; // reset this cached value
	}

	public static void unregisterGlobalPlugin(Plugin instance) {
		getGlobalPlugins(); // trigger initialization
		plugins.remove(instance);
		isReloadableTypePlugins = null; // reset this cached value
	}
}
