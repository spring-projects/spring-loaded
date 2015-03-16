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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Handle;
import org.springsource.loaded.agent.FileSystemWatcher;
import org.springsource.loaded.agent.ReloadDecision;
import org.springsource.loaded.agent.ReloadableFileChangeListener;
import org.springsource.loaded.agent.SpringLoadedPreProcessor;
import org.springsource.loaded.infra.UsedByGeneratedCode;
import org.springsource.loaded.support.Java8;


// TODO debug: stepping into deleted methods - should delete line number table for deleted methods
/**
 * The type registry tracks all reloadable types loaded by a specific class loader. It is configurable via a
 * springloaded.properties file (which it will discover as resources through the classloader) or directly via a
 * configure(Properties) method call.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class TypeRegistry {

	/**
	 * Types in these packages are not reloadable by default ('inclusions' must be specified to override this default).
	 */
	private final static String[][] ignorablePackagePrefixes;

	private static Logger log = Logger.getLogger(TypeRegistry.class.getName());

	// The first time something gets reloaded this is flipped
	public static boolean nothingReloaded = true;

	static {
		ignorablePackagePrefixes = new String[26][];
		ignorablePackagePrefixes['a' - 'a'] = new String[] { "antlr/" };
		ignorablePackagePrefixes['c' - 'a'] = new String[] { "com/springsource/tcserver/",
			"com/springsource/insight" };
		ignorablePackagePrefixes['g' - 'a'] = new String[] { "groovy/", "groovyjarjarantlr/", "groovyjarjarasm/",
			"grails/", };
		ignorablePackagePrefixes['j' - 'a'] = new String[] { "java/", "javassist/", "javax/" };
		ignorablePackagePrefixes['o' - 'a'] = new String[] { "org/springsource/loaded/", "org/objectweb/asm",
			"org/codehaus/groovy/", "org/apache/", "org/springframework/",
			"org/hibernate/", "org/hsqldb/", "org/aspectj/", "org/xml/", "org/h2/" };
	}

	// @formatter:off
	// These classloaders do not get a type registry (do not load reloadable types!)
	private final static String[] STANDARD_EXCLUDED_LOADERS = new String[] {
		// TODO DIFF rules for excluding this loader? is it necessary to usually exclude under tcserver?
		// sun.misc.Launcher$AppClassLoader
		"sun.misc.Launcher$ExtClassLoader",
		"sun.reflect.DelegatingClassLoader",
		"javax.management.remote.rmi.NoCallStackClassLoader",
		"org.springsource.loaded.ChildClassLoader",
		//		"groovy.lang.GroovyClassLoader$InnerLoader",
		// not excluding GCL$InnerLoader because we want the reflection stuff rewritten - think we need to separate out 
		// reflection rewriting from the rest of callside rewriting.  Although do we still need to rewrite call sites anyway, although the code there may not change (i.e. TypeRewriter not
		// required), the targets for some calls may come and go (may not have been in the original loaded version)
		"org.apache.jasper.servlet.JasperLoader",

			// tc server configuration...
			//	"org.apache.catalina.loader.StandardClassLoader" 
	};

	// @formatter:on

	public static final String Key_ExcludedLoaders = "excluded.loaders";

	public static final String Key_Inclusions = "inclusions";

	public static final String Key_Exclusions = "exclusions";

	public static final String Key_ReloadableRebase = "rebasePaths";

	public static final String Key_Profile = "profile";

	public static int nextFreeRegistryId = 0;

	private int maxClassDefinitions;

	/**
	 * Map from each classloader to the type registry responsible for that loader.
	 * <p>
	 * <b>Note:</b> Notice that this is a WeakHashMap - the keys are 'weak'. That means a reference in the map doesn't
	 * prevent GC of the ClassLoader. Once the ClassLoader is gone we don't need that TypeRegistry any more. It isn't
	 * WeakReference<TypeRegistry> because we do need those things around whilst the ClassLoader is around. Although
	 * there is a reference from a ReloadableType to a TypeRegistry there is a window after the TypeRegistry has been
	 * created before a ReloadableType object is created - and in that window TypeRegistries would be GCd if the
	 * reference here was weak.
	 */
	private static Map<ClassLoader, TypeRegistry> loaderToRegistryMap = Collections
			.synchronizedMap(new WeakHashMap<ClassLoader, TypeRegistry>());

	private static String[] excludedLoaders = STANDARD_EXCLUDED_LOADERS;

	/**
	 * Map from string prefixes to replacement prefixes - allows classes to be loaded from places other than where they
	 * are found initially.
	 */
	private Map<String, String> rebasePaths = new HashMap<String, String>();

	private List<String> pluginClassNames = new ArrayList<String>();

	List<Plugin> localPlugins = new ArrayList<Plugin>();

	/**
	 * Controls if the registry will define types or will allow the caller (possibly a transformer running under an
	 * agent) to define it.
	 */
	public boolean directlyDefineTypes = true;

	@SuppressWarnings("unchecked")
	public static void reinitialize() {
		nextFreeRegistryId = 0;
		loaderToRegistryMap.clear();
		registryInstances = new WeakReference[10];
	}

	/**
	 * The classloader for which this TypeRegistry is responsible. ONLY the registry instance holds the classloader.
	 */
	private WeakReference<ClassLoader> classLoader;

	/** The id number for the type registry, allocated at creation time */
	private int id;

	/** Reusable extractor */
	TypeDescriptorExtractor extractor;

	ExecutorBuilder executorBuilder;

	private boolean configured = false;

	/**
	 * Configuration properties for the TypeRegistry as loaded from springloaded.properties files
	 */
	private Properties configuration;

	private List<TypePattern> inclusionPatterns = null;

	private List<TypePattern> exclusionPatterns = null;

	// TODO have one map with some kinds of entry that can clean themselves up? (weakly ref'd)
	Map<String, TypeDescriptor> reloadableTypeDescriptorCache = new HashMap<String, TypeDescriptor>();

	// TODO make into a soft hashmap?
	Map<String, TypeDescriptor> typeDescriptorCache = new HashMap<String, TypeDescriptor>();

	Map<String, ReloadableType> cglibProxies = new HashMap<String, ReloadableType>();

	Map<String, ReloadableType> cglibProxiesFastClass = new HashMap<String, ReloadableType>();

	// Map from an interface name (eg. a/b/c/MyInterface) to a set of generated proxies for it (eg. $Proxy5)
	public Map<String, Set<ReloadableType>> jdkProxiesForInterface = new HashMap<String, Set<ReloadableType>>();

	// TODO !! Really needs tidying up on a reload event or decide if this ONLY contains non-reloadable types

	/**
	 * Create a TypeRegistry for a specified classloader. On creation an id number is allocated for the registry which
	 * can then be used as shorthand reference to the registry in rewritten code. A sub-classloader is created to handle
	 * loading generated artifacts - by using a child classloader it can be discarded after a number of reloadings have
	 * occurred to recover memory. This constructor is only used by the factory method getTypeRegistryFor().
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private TypeRegistry(ClassLoader classloader) {
		this.directlyDefineTypes = GlobalConfiguration.directlyDefineTypes;
		this.classLoader = new WeakReference(classloader);
		this.maxClassDefinitions = GlobalConfiguration.maxClassDefinitions;
		synchronized (TypeRegistry.class) {
			this.id = nextFreeRegistryId++;
		}
		//		this.childClassLoader = new WeakReference(new ChildClassLoader(classloader));
		if (this.id >= registryInstances.length) {
			WeakReference<TypeRegistry>[] newRegistryInstances = new WeakReference[registryInstances.length + 10];
			System.arraycopy(registryInstances, 0, newRegistryInstances, 0, registryInstances.length);
			registryInstances = newRegistryInstances;
		}
		registryInstances[this.id] = new WeakReference(this);
		loaderToRegistryMap.put(classloader, this);
		extractor = new TypeDescriptorExtractor(this);
		executorBuilder = new ExecutorBuilder(this);
		ensureConfigured();
	}

	private static List<String> excludedLoaderInstances = new ArrayList<String>();

	/**
	 * Check if a type registry exists for a specific type registry ID. Enables parts of the system (for example the
	 * FileSystemWatcher) to check if a type registry is still alive/active.
	 * 
	 * @param typeRegistryId the ID of a type registry
	 * @return true if that type registry is still around, false otherwise
	 */
	public static boolean typeRegistryExistsForId(int typeRegistryId) {
		for (TypeRegistry typeRegistry : loaderToRegistryMap.values()) {
			if (typeRegistry != null && typeRegistry.getId() == typeRegistryId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Factory access method for obtaining TypeRegistry instances. Returns a TypeRegistry for the specified classloader.
	 * 
	 * @param classloader The classloader to create/retrieve the type registry for
	 * @return the TypeRegistry for the classloader
	 */
	public static TypeRegistry getTypeRegistryFor(ClassLoader classloader) {
		if (classloader == null) {
			return null;
		}
		//WeakReference<TypeRegistry> existingRegistryRef = loaderToRegistryMap.get(classloader);
		TypeRegistry existingRegistry = loaderToRegistryMap.get(classloader);//existingRegistryRef==null?null:existingRegistryRef.get();
		if (existingRegistry != null) {
			return existingRegistry;
		}

		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
			if (excludedLoaderInstances.contains(classloader.toString())) {
				return null;
			}
		}
		String classloaderName = classloader.getClass().getName();
		if (classloaderName.equals("sun.reflect.DelegatingClassLoader")) {
			return null;
		}
		for (String excluded : excludedLoaders) {
			if (classloaderName.startsWith(excluded)) {
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINEST)) {
					log.info("Classloader " + classloaderName + " has been deliberately excluded");
				}
				excludedLoaderInstances.add(classloader.toString());
				return null;
			}
		}
		//		if (GlobalConfiguration.limit) {
		//			// only allow for certain loaders!
		//			boolean isOK = false;
		//			if (classloaderName.equals("org.apache.catalina.loader.StandardClassLoader")) {
		//				isOK = true;
		//			} else if (classloaderName.equals("com.springsource.insight.collection.tcserver.ltw.TomcatWeavingInsightClassLoader")) {
		//				isOK = true;
		//			} else if (classloaderName.equals("org.springframework.instrument.classloading.tomcat.TomcatInstrumentableClassLoader")) {
		//				isOK = true;
		//			} else if (classloaderName.equals("org.apache.catalina.loader.WebappClassLoader")) {
		//				isOK = true;
		//			}
		//			if (!isOK) {
		//				return null;
		//			}
		//		}

		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
			log.info("TypeRegistry.getRegistryFor(): creating new TypeRegistry for loader " + classloader);
		}

		TypeRegistry tr = new TypeRegistry(classloader);
		//		if (GlobalConfiguration.isRuntimeLogging) {
		//			Utils.log(100, "TypeRegistry.getTypeRegistryFor(classloader=" + classloader + ") returning " + tr);
		//		}
		return tr;
	}

	/**
	 * Only checks the reloadable types this registry knows about, it doesn't search beyond that.
	 * 
	 * @param slashedClassname the slashed classname (e.g. java/lang/String)
	 * @return the TypeDescriptor or null if that classname is unknown
	 */
	public TypeDescriptor getDescriptorForReloadableType(String slashedClassname) {
		return reloadableTypeDescriptorCache.get(slashedClassname);
	}

	public TypeDescriptor getDescriptorFor(String slashedname) {
		TypeDescriptor cached = checkCache(slashedname);
		if (cached != null) {
			return cached;
		}

		// TODO cheaper/faster to go up the typeregistry hierarchy?

		// This will not work for a generated class, what should we do in that case?
		byte[] data = Utils.loadSlashedClassAsBytes(classLoader.get(), slashedname);
		// As the caller did not say, we need to work it out:
		boolean isReloadableType = isReloadableTypeName(slashedname);
		TypeDescriptor td = extractor.extract(data, isReloadableType);
		if (isReloadableType) {
			reloadableTypeDescriptorCache.put(slashedname, td);
		}
		else {
			typeDescriptorCache.put(slashedname, td);
		}
		return td;
	}

	public TypeDescriptor getLatestDescriptorFor(String slashedname) {
		TypeDescriptor cached = checkCache(slashedname);
		if (cached != null) {
			return cached;
		}
		byte[] data = Utils.loadSlashedClassAsBytes(classLoader.get(), slashedname);
		// As the caller did not say, we need to work it out:
		boolean isReloadableType = isReloadableTypeName(slashedname);
		TypeDescriptor td = extractor.extract(data, isReloadableType);
		if (isReloadableType) {
			reloadableTypeDescriptorCache.put(slashedname, td);
		}
		else {
			typeDescriptorCache.put(slashedname, td);
		}
		return td;
	}

	private TypeDescriptor checkCache(String slashedname) {
		TypeDescriptor td = typeDescriptorCache.get(slashedname);
		if (td == null) {
			td = reloadableTypeDescriptorCache.get(slashedname);
		}
		return td;
	}

	/**
	 * Configure (if not already done) this TypeRegistry by locating springloaded.properties (through a findResources
	 * call) then loading it then processing any directives within it.
	 */
	public void ensureConfigured() {
		if (configured) {
			return;
		}
		loadPropertiesConfiguration();
		processPropertiesConfiguration();
		loadPlugins();
		configured = true;
	}

	// Determine if any plugins are visible from the attached classloader
	private void loadPlugins() {
		// Read the plugin class names from well known resources
		try {
			Enumeration<URL> pluginResources = classLoader.get().getResources(
					"META-INF/services/org.springsource.reloading.agent.Plugins");
			while (pluginResources.hasMoreElements()) {
				URL pluginResource = pluginResources.nextElement();
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINEST)) {
					log.finest("loadPlugins: TypeRegistry=" + this.toString() + ": loading plugin list file "
							+ pluginResource);
				}
				InputStream is = pluginResource.openStream();
				BufferedReader pluginClassNamesReader = new BufferedReader(new InputStreamReader(is));
				try {
					while (true) {
						String pluginName = pluginClassNamesReader.readLine();
						if (pluginName == null) {
							break;
						}
						if (!pluginName.startsWith("#")) {
							pluginClassNames.add(pluginName);
						}
					}
				}
				catch (IOException ioe) {
					// eof
				}
				is.close();
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Now load those plugins
		for (String pluginClassName : pluginClassNames) {
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINEST)) {
				log.finest("loadPlugins: TypeRegistry=" + this.toString() + ": loading plugin " + pluginClassName);
			}
			try {
				Class<?> pluginClass = Class.forName(pluginClassName, false, this.classLoader.get());
				Plugin pluginInstance = (Plugin) pluginClass.newInstance();
				localPlugins.add(pluginInstance);
			}
			catch (Exception e) {
				log.log(Level.WARNING, "Unable to find and instantiate plugin " + pluginClassName, e);
			}
		}
	}

	/**
	 * Configure this TypeRegistry using a specific set of properties - this will override any previous configuration.
	 * It is mainly provided for testing purposes.
	 * 
	 * @param properties the properties to use to configure this type registry
	 */
	public void configure(Properties properties) {
		resetConfiguration();
		configuration = properties;
		processPropertiesConfiguration();
		configured = true;
	}

	public void resetConfiguration() {
		inclusionPatterns = null;
		nothingReloaded = true;
	}

	public static void resetAllConfiguration() {
		nothingReloaded = true;
	}

	public List<TypePattern> getInclusionPatterns() {
		return inclusionPatterns;
	}

	public List<TypePattern> getExclusionPatterns() {
		return exclusionPatterns;
	}

	private void processPropertiesConfiguration() {
		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINEST)) {
			log.finest("processPropertiesConfiguration: TypeRegistry=" + this.toString());
		}
		inclusionPatterns = getPatternsFrom(configuration.getProperty(Key_Inclusions));
		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINEST)) {
			log.finest("processPropertiesConfiguration: inclusions are set to '" + inclusionPatterns + "'");
		}
		exclusionPatterns = getPatternsFrom(configuration.getProperty(Key_Exclusions));
		String value = configuration.getProperty(Key_ReloadableRebase);
		if (value != null) {
			parseRebasePaths(value);
		}
		// TODO what are we trying to achieve with this setting?
		value = configuration.getProperty(Key_ExcludedLoaders);
		if (value != null) {
			if (value.equals("NONE")) {
				// do nothing
			}
			else {
				List<String> loaders = new ArrayList<String>();
				StringTokenizer st = new StringTokenizer(value, ",");
				while (st.hasMoreElements()) {
					String loaderPrefix = st.nextToken();
					if (loaderPrefix.toLowerCase().equals("default")) {
						for (String element : STANDARD_EXCLUDED_LOADERS) {
							loaders.add(element);
						}
					}
					else {
						// TODO do they need marking as prefixes or exact names?
						loaders.add(loaderPrefix);
					}

				}
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
					log.log(Level.FINER, "Setting exclusions to " + loaders);
				}
				excludedLoaders = loaders.toArray(new String[0]);
			}
		}
	}

	/**
	 * Process a set of rebase definitions of the form 'a=b,c=d,e=f'.
	 */
	private void parseRebasePaths(String rebaseDefinitions) {
		StringTokenizer tokenizer = new StringTokenizer(rebaseDefinitions, ",");
		while (tokenizer.hasMoreTokens()) {
			String rebasePair = tokenizer.nextToken();
			int equals = rebasePair.indexOf('=');
			String fromPrefix = rebasePair.substring(0, equals);
			String toPrefix = rebasePair.substring(equals + 1);
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				log.info("processPropertiesConfiguration: adding rebase rule from '" + fromPrefix + "' to '" + toPrefix
						+ "'");
			}
			rebasePaths.put(fromPrefix, toPrefix);
		}
	}

	private void loadPropertiesConfiguration() {
		// Initial configuration is seeded with any global configuration
		configuration = new Properties(GlobalConfiguration.globalConfigurationProperties);
		try {
			Set<String> configurationFiles = new HashSet<String>();
			ClassLoader classloader = classLoader.get();
			Enumeration<URL> resources = classloader == null ? null
					: classloader.getResources("springloaded.properties");
			if (resources == null) {
				if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
					log.info("Unable to load springloaded.properties, cannot find it through classloader "
							+ classloader);
				}
			}
			else {
				while (resources.hasMoreElements()) {
					URL url = resources.nextElement();
					String configFile = url.toString();
					if (GlobalConfiguration.logging && log.isLoggable(Level.INFO)) {
						log.log(Level.INFO, this.toString() + ": processing config file: " + url.toString());
					}
					if (configurationFiles.contains(configFile)) {
						continue;
					}
					configurationFiles.add(configFile);
					InputStream is = url.openStream();

					Properties p = new Properties();
					p.load(is);
					is.close();
					Set<String> keys = p.stringPropertyNames();
					for (String key : keys) {
						if (!configuration.containsKey(key)) {
							configuration.put(key, p.getProperty(key));
						}
						else {
							// Extend our configuration
							String valueSoFar = configuration.getProperty(key);
							StringBuilder sb = new StringBuilder(valueSoFar);
							sb.append(",");
							sb.append(p.getProperty(key));
							configuration.put(key, sb.toString());
						}
					}
				}
			}
		}
		catch (Exception e) {
			throw new ReloadException(
					"loadPropertiesConfiguration: Problem accessing springloaded.properties file resources", e);
		}

		//		if (GlobalConfiguration.logging && log.isLoggable(Level.INFO)) {
		//			System.err.println("ee00");
		//			Set<String> configurationPropertyNames = configuration.stringPropertyNames();
		//			System.err.println("eeAA");
		//			if (configurationPropertyNames.isEmpty()) {
		//				System.err.println("eeBB");
		//				log.log(Level.INFO, "configuration:" + this + ": empty configuration");
		//			} else {
		//				System.err.println("eeCC");
		//				for (String configurationPropertyName : configurationPropertyNames) {
		//					System.err.println("eeDD");
		//					log.log(Level.INFO, "configuration:" + this + ": configuration: " + configurationPropertyName + "="
		//							+ configuration.getProperty(configurationPropertyName));
		//				}
		//			}
		//		}

	}

	private static Method getResourceMethod = null;

	/**
	 * If a type is found to come from a jar, we put the package name in here, which should save us looking for types in
	 * the same package. This does pre-req that there are no split packages.
	 */
	private List<String> packagesFound = new ArrayList<String>();

	private List<String> packagesNotFound = new ArrayList<String>();

	/**
	 * Determine if the named type could be reloadable. This method is invoked if the user has not setup any inclusions.
	 * With no inclusions specified, something is considered reloadable if it is accessible by the classloader for this
	 * registry and is not in a jar
	 * 
	 * @param slashedName the typename of interest (e.g. com/foo/Bar)
	 * @return true if the type should be considered reloadable
	 */
	private boolean couldBeReloadable(String slashedName) {
		if (slashedName == null) {
			return false;
		}
		if (slashedName.startsWith("java/")) {
			return false;
		}
		char ch = slashedName.charAt(0);
		int index = ch - 'a';
		if (index > 0 && index < 26) {
			String[] candidates = ignorablePackagePrefixes[index];
			if (candidates != null) {
				for (String ignorablePackagePrefix : candidates) {
					if (slashedName.startsWith(ignorablePackagePrefix)) {
						if (GlobalConfiguration.explainMode && log.isLoggable(Level.INFO)) {
							log.info("WhyNotReloadable? The type "
									+ slashedName
									+ " is using a package name '"
									+ ignorablePackagePrefix
									+ "' which is considered infrastructure and types within it are not made reloadable");
						}
						return false;
					}
				}
			}
		}
		if (slashedName.indexOf("$Proxy") != -1 || slashedName.indexOf("$$EnhancerBy") != -1
				|| slashedName.indexOf("$$FastClassBy") != -1) {
			return true;
		}
		// TODO review all these... are these four only loaded by jasperloader?
		int underscorePos = slashedName.indexOf("_");
		if (underscorePos != -1) {
			if (slashedName.endsWith("_jspx") || slashedName.endsWith("_tagx")) {
				return false;
			}
			if (slashedName.endsWith("_jspx$Helper") || slashedName.endsWith("_tagx$Helper")) {
				return false;
			}
			// skip grails scripts like "_PackagePlugins_groovy$_run_closure1_closure7"
			if (ch == '_' && slashedName.indexOf("_groovy") != -1) {
				return false;
			}
		}
		int lastSlashPos = slashedName.lastIndexOf('/');
		String packageName = lastSlashPos == -1 ? null : slashedName.substring(0, lastSlashPos);
		if (packageName != null) {
			// is it something we already know about?
			for (String foundPackageName : packagesFound) {
				if (packageName.equals(foundPackageName)) {
					//					System.out.println("fast accept " + slashedName);
					return true;
				}
			}
			for (String notfoundPackageName : packagesNotFound) {
				if (packageName.equals(notfoundPackageName)) {
					//					System.out.println("fast reject " + slashedName);
					return false;
				}
			}
		}
		if (ch == '[') {
			return false;
		}
		try {
			if (getResourceMethod == null) {
				try {
					getResourceMethod = ClassLoader.class.getDeclaredMethod("getResource", String.class);
				}
				catch (Exception e) {
					throw new ReloadException("Unable to locate 'getResource' on the ClassLoader class", e);
				}
			}
			getResourceMethod.setAccessible(true);
			URL url = (URL) getResourceMethod.invoke(classLoader.get(), slashedName + ".class");
			boolean reloadable = false;
			if (url != null) {
				String protocol = url.getProtocol();
				// ignore 'jar' - what others?
				//				if (!protocol.equals("file")) {
				//					System.out.println("FOOBAR:" + slashedName + " loader=" + classLoader);
				//					new RuntimeException().printStackTrace();
				//				}
				reloadable = protocol.equals("file");
			}
			if (packageName != null) {
				if (reloadable) {
					packagesFound.add(packageName);
				}
				else {
					packagesNotFound.add(packageName);
				}
				//			} else {
				//				System.out.println("expensive, no package name and URL checked: " + slashedName + " : " + url + " loader="
				//						+ classLoader);
			}
			return reloadable;
		}
		catch (Exception e) {
			throw new ReloadException("Unexpected problem locating the bytecode for " + slashedName + ".class", e);
		}
	}

	public boolean isReloadableTypeName(String slashedName) {
		return isReloadableTypeName(slashedName, null, null);
	}

	/**
	 * Determine if the type specified is a reloadable type. This method works purely by name, it does not load
	 * anything.
	 * 
	 * @param slashedName the type name, eg. a/b/c/D
	 * @param protectionDomain the protection domain this class is being loaded under
	 * @param bytes the class bytes for the class being loaded
	 * @return true if the type is reloadable, false otherwise
	 */
	public boolean isReloadableTypeName(String slashedName, ProtectionDomain protectionDomain, byte[] bytes) {
		if (GlobalConfiguration.verboseMode && log.isLoggable(Level.FINER)) {
			log.finer("entering TypeRegistry.isReloadableTypeName(" + slashedName + ")");
		}
		if (GlobalConfiguration.assertsMode) {
			Utils.assertSlashed(slashedName);
		}
		if (GlobalConfiguration.isProfiling) {
			if (slashedName.startsWith("com/yourkit")) {
				if (GlobalConfiguration.explainMode && log.isLoggable(Level.FINER)) {
					log.finer("[explanation] The type " + slashedName
							+ " is considered part of yourkit and is not being made reloadable");
				}
				return false;
			}
		}
		// Proxy types that implement a reloadable interface should themselves be made reloadable ... to be fleshed out
		//		if (slashedName.startsWith("$Proxy")) {
		//			try {
		//				String[] implementedInterfaces = QuickVisitor.getImplementedInterfaces(bytes);
		//				StringBuilder sb = new StringBuilder();
		//				if (implementedInterfaces != null) {
		//					for (String s : implementedInterfaces) {
		//						sb.append(s).append(" ");
		//					}
		//				}
		//				System.out.println("Proxy implements :" + sb.toString());
		//			} catch (NullPointerException npe) {
		//				throw new RuntimeException("bytes are null?" + (bytes == null ? true : bytes.length) + npe);
		//			}
		//		}
		// TODO special cases... review them
		//		if (/*slashedName.indexOf("/$Proxy") != -1 || */slashedName.indexOf("javassist") != -1) {
		//			return false;
		//		}

		for (IsReloadableTypePlugin plugin : SpringLoadedPreProcessor.getIsReloadableTypePlugins()) {
			ReloadDecision decision = plugin.shouldBeMadeReloadable(this, slashedName, protectionDomain, bytes);
			if (decision == ReloadDecision.YES) {
				if (GlobalConfiguration.explainMode && log.isLoggable(Level.FINER)) {
					log.finer("[explanation] The plugin " + plugin.getClass().getName() + " determined type "
							+ slashedName + " is reloadable");
				}
				return true;
			}
			else if (decision == ReloadDecision.NO) {
				if (GlobalConfiguration.explainMode && log.isLoggable(Level.FINER)) {
					log.finer("[explanation] The plugin " + plugin.getClass().getName() + " determined type "
							+ slashedName + " is not reloadable");
				}
				return false;
			}
		}

		if (inclusionPatterns.isEmpty()) {
			// No inclusions, so unless it matches an exclusion, it will be included
			if (exclusionPatterns.isEmpty()) {
				if (couldBeReloadable(slashedName)) {
					if (GlobalConfiguration.explainMode && log.isLoggable(Level.FINER)) {
						log.finer("[explanation] The class "
								+ slashedName
								+ " is currently considered reloadable. It matches no exclusions, is accessible from this classloader and is not in a jar/zip.");
					}
					return true;
				}
				else {
					if (GlobalConfiguration.explainMode && log.isLoggable(Level.FINER)) {
						log.finer("[explanation] The class " + slashedName
								+ " is not going to be treated as reloadable.");
					}
					return false;
				}
			}
			else {
				boolean isExcluded = false;
				String matchName = slashedName.replace('/', '.');
				for (TypePattern typepattern : exclusionPatterns) {
					if (typepattern.matches(matchName)) {
						isExcluded = true;
						break;
					}
				}
				if (isExcluded) {
					return false;
				}
				if (couldBeReloadable(slashedName)) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		else {
			// There are inclusion patterns, we must match one and not be excluded
			boolean isIncluded = false;
			String matchName = slashedName.replace('/', '.');
			for (TypePattern typepattern : inclusionPatterns) {
				if (typepattern.matches(matchName)) {
					isIncluded = true;
					break;
				}
			}
			if (!isIncluded) {
				return false;
			}
			// Ok it matched an inclusion, but it must not match any exclusions
			if (exclusionPatterns.isEmpty()) {
				return true;
			}
			else {
				boolean isExcluded = false;
				for (TypePattern typepattern : exclusionPatterns) {
					if (typepattern.matches(matchName)) {
						isExcluded = true;
						break;
					}
				}
				return !isExcluded;
			}
		}
	}

	/**
	 * Lookup the type ID for a string. First checks those allocated but not yet registered, then those that are already
	 * registered. If not found then a new one is allocated and recorded.
	 * 
	 * @param slashname the slashed type name, eg. a/b/c/D
	 * @param allocateIfNotFound determines whether an id should be allocated for the type if it cannot be found
	 * @return the unique ID number
	 */
	public int getTypeIdFor(String slashname, boolean allocateIfNotFound) {
		if (allocateIfNotFound) {
			return NameRegistry.getIdOrAllocateFor(slashname);
		}
		else {
			return NameRegistry.getIdFor(slashname);
		}
	}

	/*
	 * Rewrite the call sites in some class in the context of this registry (which knows about a particular set of types as being
	 * Reloadable).
	 */
	public byte[] methodCallRewrite(byte[] bytes) {
		return MethodInvokerRewriter.rewrite(this, bytes);
	}

	/*
	 * This version will attempt to use a cache if one is being managed.
	 */
	public byte[] methodCallRewriteUseCacheIfAvailable(String slashedClassName, byte[] bytes) {
		if (GlobalConfiguration.isCaching) {
			return MethodInvokerRewriter.rewriteUsingCache(slashedClassName, this, bytes);
		}
		else {
			return MethodInvokerRewriter.rewrite(this, bytes);
		}
	}

	public void loadNewVersion(ReloadableType rtype, File file) {
		String versionstamp = Utils.encode(file.lastModified());

		// load bytes for new version
		byte[] newBytes = null;
		try {
			newBytes = Utils.loadFromStream(new FileInputStream(file));
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		rtype.loadNewVersion(versionstamp, newBytes);
	}

	/**
	 * Map from a registry ID number to a registry instance. ID numbers are used in the rewritten code. WeakReferences
	 * so that we aren't preventing collection of TypeRegistry objects when their classloaders are GC'd.
	 */
	@SuppressWarnings("unchecked")
	private static WeakReference<TypeRegistry>[] registryInstances = new WeakReference[10];

	/**
	 * The child classloader that loads (re)generated artifacts. Can be discarded periodically to recover memory
	 * (permgen). ONLY the registry holds the classloader. As the child classloader has a reference to the parent, we
	 * want a weak reference to the child so that the parent is free to be GC'd. When it goes, this will go but that is
	 * fine.
	 */
	private WeakReference<ChildClassLoader> childClassLoader;

	/** Per registry array from allocated ID to ReloadadbleType */
	private ReloadableType[] reloadableTypes = new ReloadableType[10];

	/** Track how many elements of the array have been filled in */
	private int reloadableTypesSize = 0;

	/** Map from slashed type name to ReloadableType */
	//	public Map<String, ReloadableType> allocatedIds = new HashMap<String, ReloadableType>();

	/**
	 * Map from slashed type name to allocated ID. IDs are allocated on first reference which may occur before the type
	 * is loaded and registered. This map maintains an up to date list of names that have been allocated a number but
	 * not yet registered. Once they are registered they vanish from this map.
	 */
	//	public Map<String, Integer> allocatedButNotYetRegisteredItds = new HashMap<String, Integer>();

	/** Cached for reuse */
	private Method defineClassMethod = null;

	/**
	 * @return the classloader associated with this registry, the caller should not cache it.
	 */
	public ClassLoader getClassLoader() {
		return classLoader.get();
	}

	// TODO what about org.apache.jasper.servlet.JasperLoader

	/**
	 * @return the ID number of this type registry (that was allocated on creation)
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Add a type to the registry. The name should have already passed the isReloadableTypeName() test.
	 * 
	 * @param dottedname type name of the form a.b.c.D
	 * @param initialbytes the first version of the bytes as loaded
	 * @return the ReloadableType or null if it cannot be made reloadable
	 */
	public ReloadableType addType(String dottedname, byte[] initialbytes) {
		if (GlobalConfiguration.logging && log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "ReloadableType.addType(): processing " + dottedname);
		}
		//		if (GlobalConfiguration.assertsOn) {
		//			String slashedName = dottedname.replace('.', '/');
		//			Utils.assertTrue(isReloadableTypeName(slashedName), dottedname);
		//		}

		TypeDescriptor td = extractor.extract(initialbytes, true);

		// TODO annotations are not reloadable, they have a null reloadable type - who does that impact in a development setup?
		if (td.isAnnotation()) {
			return null;
		}

		String slashname = dottedname.replace('.', '/');
		reloadableTypeDescriptorCache.put(slashname, td);
		if (GlobalConfiguration.assertsMode) {
			Utils.assertTrue(td.getName().equals(slashname), "Name from bytecode '" + td.getName()
					+ "' does not match that passed in '" + slashname + "'");
		}
		int typeId = NameRegistry.getIdOrAllocateFor(slashname);
		ReloadableType rtype = new ReloadableType(dottedname, initialbytes, typeId, this, td);
		if (GlobalConfiguration.classesToDump != null && GlobalConfiguration.classesToDump.contains(slashname)) {
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				log.info("Dumping bytes for " + slashname);
			}
			Utils.dump(slashname, rtype.getBytesLoaded());
		}
		// expand by 10 if we need to - what is the right increment number here?
		if (typeId >= reloadableTypes.length) {
			resizeReloadableTypeArray(typeId);
		}
		reloadableTypes[typeId] = rtype;
		if ((typeId + 1) > reloadableTypesSize) {
			reloadableTypesSize = typeId + 1;
		}
		// allocatedIds.put(slashname, rtype);
		// allocatedButNotYetRegisteredItds.remove(slashname);
		int cglibIndex = slashname.indexOf("$$EnhancerBy");
		int fcIndex = slashname.indexOf("$$FastClassBy"); // a type can have both (the fast class for a proxy)
		if (fcIndex != -1) {
			String originalType = slashname.substring(0, fcIndex);
			cglibProxiesFastClass.put(originalType, rtype);
		}
		else if (cglibIndex != -1) {
			String originalType = slashname.substring(0, cglibIndex);
			cglibProxies.put(originalType, rtype);
		}
		int jdkProxyIndex = slashname.indexOf("$Proxy");
		if (jdkProxyIndex == 0 || (jdkProxyIndex > 0 && slashname.charAt(jdkProxyIndex - 1) == '/')) {
			// Determine if the interfaces being implemented are reloadable
			String[] interfacesImplemented = Utils.discoverInterfaces(initialbytes);
			if (interfacesImplemented != null) {
				// Want to record which interfaces (when they change) should cause which proxies to reload
				for (int i = 0; i < interfacesImplemented.length; i++) {
					Set<ReloadableType> l = jdkProxiesForInterface.get(interfacesImplemented[i]);
					if (l == null) {
						l = new HashSet<ReloadableType>();
						jdkProxiesForInterface.put(interfacesImplemented[i], l);
					}
					l.add(rtype);
				}
			}
		}
		if (GlobalConfiguration.logging && log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "ReloadableType.addType(): Type '" + dottedname + "' is now reloadable! id=" + typeId);
		}
		return rtype;
	}

	private synchronized void resizeReloadableTypeArray(int typeId) {
		if (typeId < reloadableTypes.length) {
			// Another thread already did it
			return;
		}
		int extraSpace = (typeId - reloadableTypes.length) + 1;
		if (extraSpace < 10) {
			extraSpace = 10;
		}
		ReloadableType[] newReloadableTypes = new ReloadableType[reloadableTypes.length + extraSpace];
		System.arraycopy(reloadableTypes, 0, newReloadableTypes, 0, reloadableTypes.length);
		reloadableTypes = newReloadableTypes;
	}

	public ReloadableType getReloadableType(int typeId) {
		if (typeId >= reloadableTypesSize) {
			return null;
		}
		return reloadableTypes[typeId];
	}

	/**
	 * Sometimes we discover the reloadabletype during program execution, for example A calls B and we haven't yet seen
	 * B. We find B has been loaded by a parent classloader, let's remember B here so we can do fast lookups for it.
	 * 
	 * @param typeId the id for the type
	 * @param rtype the ReloadableType to associate with the id
	 */
	public void rememberReloadableType(int typeId, ReloadableType rtype) {
		if (typeId >= reloadableTypes.length) {
			resizeReloadableTypeArray(typeId);
		}
		reloadableTypes[typeId] = rtype;
		if ((typeId + 1) > reloadableTypesSize) {
			reloadableTypesSize = typeId + 1;
		}
	}

	/**
	 * Determine the reloadabletype object representation for a specified class. If the caller already knows the ID for
	 * the type, that would be a quicker way to locate the reloadable type object.
	 * 
	 * @param slashedClassName the slashed (e.g. java/lang/String) class name
	 * @return the ReloadableType
	 */
	public ReloadableType getReloadableType(String slashedClassName) {
		int id = getTypeIdFor(slashedClassName, true);
		if (id >= reloadableTypesSize) {
			return null;
		}
		return getReloadableType(id);
	}

	public ReloadableType getReloadableSuperType(String slashedClassname) {
		//		int id = getTypeIdFor(slashedClassname, false);
		ReloadableType rtype = getReloadableTypeInTypeRegistryHierarchy(slashedClassname);
		if (rtype != null) {
			return rtype;
		}
		return getReloadableType(slashedClassname);
	}

	/**
	 * For a specific classname, this method will search in the current type registry and any parent type registries
	 * (similar to a regular classloader delegation strategy). Returns null if the type is not found. It does not
	 * attempt to load anything in.
	 * 
	 * @param classname the type being searched for, e.g. com/foo/Bar
	 * @return the ReloadableType if found, otherwise null
	 */
	private ReloadableType getReloadableTypeInTypeRegistryHierarchy(String classname) {
		ReloadableType rtype = getReloadableType(classname, false);
		if (rtype == null) {
			// search
			TypeRegistry tr = this;
			while (rtype == null) {
				ClassLoader pcl = tr.getClassLoader().getParent();
				tr = TypeRegistry.getTypeRegistryFor(pcl);
				if (tr != null) {
					rtype = tr.getReloadableType(classname, false);
				}
				else {
					break;
				}
			}
			if (rtype != null) {
				return rtype;
			}
		}
		return rtype;
	}

	/**
	 * Find the ReloadableType object for a given classname. If the allocateIdIfNotYetLoaded option is set then a new id
	 * will be allocated for this classname if it hasn't previously been seen before. This method does not create new
	 * ReloadableType objects, they are expected to come into existence when defined by the classloader.
	 * 
	 * @param slashedClassname the slashed class name (e.g. java/lang/String)
	 * @param allocateIdIfNotYetLoaded if true an id will be allocated because sometime later the type will be loaded
	 *            (and made reloadable)
	 * @return the ReloadableType discovered or allocated, or null if not found and !allocateIdIfNotYetLoaded
	 */
	public ReloadableType getReloadableType(String slashedClassname, boolean allocateIdIfNotYetLoaded) {
		if (allocateIdIfNotYetLoaded) {
			return getReloadableType(getTypeIdFor(slashedClassname, allocateIdIfNotYetLoaded));
		}
		else {
			for (int i = 0; i < reloadableTypesSize; i++) {
				ReloadableType rtype = reloadableTypes[i];
				if (rtype != null && rtype.getSlashedName().equals(slashedClassname)) {
					return rtype;
				}
			}
			return null;
		}
	}

	/**
	 * @param name dotted name (e.g. java.lang.String)
	 * @param bytes bytes for the class
	 * @param permanent determines if the type should be defined in the classloader attached to this registry or in the
	 *            child classloader that can periodically by discarded
	 */
	Class<?> defineClass(String name, byte[] bytes, boolean permanent) {
		Class<?> clazz = null;
		ChildClassLoader ccl = (childClassLoader == null ? null : childClassLoader.get());
		if (ccl == null) {
			// ChildClassLoader instances are created and 'used' immediately - this usage ensures
			// they aren't GC'd straightaway, which they would be if the field childClassLoader
			// were simply initialized with a ChildClassLoader instance.
			ccl = new ChildClassLoader(this.getClassLoader());
			childClassLoader = new WeakReference<ChildClassLoader>(ccl);
		}
		try {
			//			System.out.println("defining " + name);
			if (permanent) {
				//				ClassPrinter.print(bytes);
				if (defineClassMethod == null) {
					defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass",
							new Class[] { String.class, bytes.getClass(), int.class, int.class });
				}
				defineClassMethod.setAccessible(true);
				ClassLoader loaderToUse = null;
				loaderToUse = classLoader.get();
				clazz = (Class<?>) defineClassMethod.invoke(loaderToUse, new Object[] { name, bytes, 0, bytes.length });
			}
			else {
				clazz = ccl.defineClass(name, bytes);
			}
		}
		catch (InvocationTargetException e) {
			throw new ReloadException("Problem defining class " + name, e);
		}
		catch (Exception e) {
			throw new ReloadException("Problem defining class " + name, e);
		}
		return clazz;
	}

	public TypeDescriptorExtractor getExtractor() {
		return extractor;
	}

	public Map<String, String> getRebasePaths() {
		return rebasePaths;
	}

	public boolean shouldDefineClasses() {
		return directlyDefineTypes;
	}

	public void setShouldDefineClasses(boolean should) {
		directlyDefineTypes = should;
	}

	/**
	 * Used to determine if the invokedynamic needs to be intercepted.
	 * 
	 * @return null if nothing has been reloaded
	 */
	@UsedByGeneratedCode
	public static Object idycheck() {
		if (TypeRegistry.nothingReloaded) {
			return null;
		}
		else {
			return "reloading-happened";
		}
	}

	/**
	 * Determine if something has changed in a particular type related to a particular descriptor and so the dispatcher
	 * interface should be used. The type registry ID and class ID are merged in the 'ids' parameter. This method is for
	 * INVOKESTATIC rewrites and so performs additional checks because it assumes the target is static.
	 * 
	 * @param ids packed representation of the registryId (top 16bits) and typeId (bottom 16bits)
	 * @param nameAndDescriptor the name and descriptor of the method about to be INVOKESTATIC'd
	 * @return null if the original code can run otherwise return the dispatcher to use
	 */
	@UsedByGeneratedCode
	public static Object istcheck(int ids, String nameAndDescriptor) {
		if (TypeRegistry.nothingReloaded) {
			return null;
		}
		int registryId = ids >>> 16;
		int typeId = ids & 0xffff;
		TypeRegistry typeRegistry = registryInstances[registryId].get();
		ReloadableType reloadableType = typeRegistry.getReloadableType(typeId);

		if (reloadableType == null) {
			reloadableType = searchForReloadableType(typeId, typeRegistry);
		}

		// Check 2: Info computed earlier
		if (reloadableType != null && !reloadableType.isAffectedByReload()) {
			return null;
		}

		if (reloadableType != null && reloadableType.hasBeenReloaded()) {
			MethodMember method = reloadableType.getLiveVersion().incrementalTypeDescriptor
					.getFromLatestByDescriptor(nameAndDescriptor);
			boolean dispatchThroughDescriptor = false;
			if (method == null) {
				// method has been deleted or is on a supertype. Look for it:

				// TODO this block is based on something below in invokespecial handling but this has some
				// fixes in - should they be migrated down below or a common util method constructed?

				Object dispatcherToUse = null;
				String supertypename = reloadableType.getTypeDescriptor().getSupertypeName();
				TypeRegistry reg = reloadableType.getTypeRegistry();
				boolean found = false;
				while (supertypename != null) {
					ReloadableType nextInHierarchy = reg.getReloadableType(supertypename);
					if (nextInHierarchy == null) {
						TypeDescriptor td = reg.getDescriptorFor(supertypename);
						if (td != null) {
							method = td.getByNameAndDescriptor(nameAndDescriptor);
							supertypename = td.getSupertypeName();
						}
						else {
							break;
						}
					}
					else if (nextInHierarchy.hasBeenReloaded()) {
						method = nextInHierarchy.getLiveVersion().incrementalTypeDescriptor.getFromLatestByDescriptor(nameAndDescriptor);
						if (method != null && IncrementalTypeDescriptor.wasDeleted(method)) {
							method = null;
						}
						// ignore catchers because the dynamic __execute method wont have an implementation of them, we should
						// just keep looking for the real thing
						if (method != null
								&& (MethodMember.isCatcher(method) || MethodMember.isSuperDispatcher(method))) {
							method = null;
						}
					}
					else {
						// it is reloadable but has not been reloaded
						method = nextInHierarchy.getMethod(nameAndDescriptor);
					}
					if (method != null) {
						found = true;
						break;
					}
					// the nextInHierarchy==null case will have already set the supertypename
					if (nextInHierarchy != null) {
						supertypename = nextInHierarchy.getSlashedSupertypeName();
					}
				}
				if (found) {
					return dispatcherToUse;
				}
				throw new NoSuchMethodError(reloadableType.getBaseName() + "." + nameAndDescriptor);
			}
			else if (IncrementalTypeDescriptor.isBrandNewMethod(method)) {
				// definetly need to use the dispatcher
				dispatchThroughDescriptor = true;
			}
			else if (IncrementalTypeDescriptor.hasChanged(method)) {
				if (IncrementalTypeDescriptor.isNowNonStatic(method)) {
					throw new IncompatibleClassChangeError("SpringLoaded: Target of static call is no longer static '"
							+ reloadableType.getBaseName() + "." + nameAndDescriptor + "'");
				}
				// TODO need a check in here for a visibility change? Something like this:
				//				if (IncrementalTypeDescriptor.hasVisibilityChanged(method)) {
				//					dispatchThroughDescriptor = true;
				//				}
			}
			if (dispatchThroughDescriptor) {
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
					log.info("istcheck(): reloadabletype=" + reloadableType + " versionstamp "
							+ reloadableType.getLiveVersion().versionstamp);
				}
				return reloadableType.getLatestDispatcherInstance();
			}
		}
		return null;
	}

	// NOTE we don't throw NSME here (we could...) instead we let the body of the deleted method (that was rewritten) throw it
	// TODO what about visibility changes?
	public static Object invokespecialSearch(ReloadableType rt, String nameAndDescriptor) {
		// does this type define it?  If yes - work out if I need to call through the dispatcher or not.  If no - try my super
		ReloadableType next = rt;
		while (next != null) {
			MethodMember m = null;
			if (next.hasBeenReloaded()) {
				m = next.getLiveVersion().incrementalTypeDescriptor.getFromLatestByDescriptor(nameAndDescriptor);
				if (m != null && IncrementalTypeDescriptor.wasDeleted(m)) {
					m = null;
				}
				// ignore catchers because the dynamic __execute method wont have an implementation of them, we should
				// just keep looking for the real thing
				if (m != null && (MethodMember.isCatcher(m) || MethodMember.isSuperDispatcher(m))) {
					m = null;
				}
			}
			else {
				m = next.getMethod(nameAndDescriptor);
			}
			if (m != null) {
				if (next.hasBeenReloaded()) {
					return next.getLatestDispatcherInstance();
				}
				else {
					return null; // do what you were going to do anyway
				}
			}
			next = next.getTypeRegistry().getReloadableType(next.getTypeDescriptor().getSupertypeName(), false);
		}
		return null; // let it fail anyway
	}

	/**
	 * See notes.md#001
	 * 
	 * @param instance the object instance on which the INVOKEINTERFACE is being called
	 * @param params the parameters to the INVOKEINTERFACE call
	 * @param instance2 the object instance on which the INVOKEINTERFACE is being called
	 * @param nameAndDescriptor the name and descriptor of what is being called (e.g. foo(Ljava/lang/String)I)
	 * @return the result of making the INVOKEINTERFACE call
	 */
	public static Object iiIntercept(Object instance, Object[] params, Object instance2, String nameAndDescriptor) {
		Class<?> clazz = instance.getClass();
		try {
			if (clazz.getName().contains("$$Lambda")) {
				// There may be multiple methods here, we want the public one (I think!)
				Method[] ms = instance.getClass().getDeclaredMethods();
				// public java.lang.String basic.LambdaJ$$E002$$Lambda$2/1484594489.m(java.lang.String,java.lang.String)
				// private static basic.LambdaJ$Foo basic.LambdaJ$$E002$$Lambda$2/1484594489.get$Lambda(basic.LambdaJ)
				Method toUse = null;
				for (Method m : ms) {
					if (Modifier.isPublic(m.getModifiers())) {
						toUse = m;
						break;
					}
				}
				toUse.setAccessible(true);
				Object o = toUse.invoke(instance, params);
				return o;
			}
			else {
				// Do what you were going to do...
				Method m = instance.getClass().getDeclaredMethod("__execute", Object[].class, Object.class,
						String.class);
				m.setAccessible(true);
				return m.invoke(instance, params, instance, nameAndDescriptor);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@UsedByGeneratedCode
	public static __DynamicallyDispatchable ispcheck(int ids, String nameAndDescriptor) {

		// TOD why no check about whether anything has been reloaded???
		if (nothingReloaded) {
			return null;
		}

		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
			log.entering("TypeRegistry", "spcheck", new Object[] { ids, nameAndDescriptor });
		}
		int typeId = ids & 0xffff;
		TypeRegistry typeRegistry = registryInstances[ids >>> 16].get();
		ReloadableType reloadableType = typeRegistry.getReloadableType(typeId);

		if (reloadableType == null) {
			reloadableType = searchForReloadableType(typeId, typeRegistry);
		}
		// Check 2: Info computed earlier
		//		if (!reloadableType.isAffectedByReload()) {
		//			return false;
		//		}
		// Search for the dispatcher we can call
		__DynamicallyDispatchable o = (__DynamicallyDispatchable) invokespecialSearch(reloadableType, nameAndDescriptor);
		return o;
	}

	/**
	 * If the reloadabletype cannot currently be located, this method will search the hierarchy of classloaders for it.
	 * If it is found, we'll record it for later quick access. TODO need to work out what to do if it is not found, dont
	 * want to keep looking - does that mean it isn't reloadable?
	 */
	private static ReloadableType searchForReloadableType(int typeId, TypeRegistry typeRegistry) {
		ReloadableType reloadableType;
		reloadableType = typeRegistry.getReloadableTypeInTypeRegistryHierarchy(NameRegistry.getTypenameById(typeId));
		typeRegistry.rememberReloadableType(typeId, reloadableType);
		return reloadableType;
	}

	@UsedByGeneratedCode
	public static Object ccheck(int ids, String descriptor) {
		if (TypeRegistry.nothingReloaded) {
			return null;
		}
		int typeId = ids & 0xffff;
		TypeRegistry typeRegistry = registryInstances[ids >>> 16].get();
		ReloadableType reloadableType = typeRegistry.getReloadableType(typeId);
		// i think only testcases can cause situations where reloadableType is null
		if (reloadableType != null && reloadableType.hasBeenReloaded()) {
			if (reloadableType.cchanged(descriptor)) {
				return reloadableType.getLatestDispatcherInstance();
			}
		}
		return null;
	}

	/**
	 * Determine if something has changed in a particular type related to a particular descriptor and so the dispatcher
	 * interface should be used. The type registry ID and class ID are merged in the 'ids' parameter. This method is for
	 * INVOKEINTERFACE rewrites and so performs additional checks because it assumes the target is an interface.
	 * <p>
	 * Methods on interfaces cannot really 'change' - the visibility is always public and they are never static. This
	 * means everything that the descriptor embodies everything about a method interface. Therefore, if something
	 * changes about the descriptor it is considered an entirely different method (and the old form is a deleted
	 * method). For this reason there is no need to consider 'changed' methods, because the static-ness nor visibility
	 * cannot change.
	 * 
	 * @param ids packed representation of the registryId (top 16bits) and typeId (bottom 16bits)
	 * @param nameAndDescriptor the name and descriptor of the method about to be INVOKEINTERFACE'd
	 * @return true if the original method operation must be intercepted
	 */
	@UsedByGeneratedCode
	public static boolean iincheck(int ids, String nameAndDescriptor) {
		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
			log.entering("TypeRegistry", "iincheck", new Object[] { ids, nameAndDescriptor });
		}
		int registryId = ids >>> 16;
		int typeId = ids & 0xffff;
		TypeRegistry typeRegistry = registryInstances[registryId].get();
		ReloadableType reloadableType = typeRegistry.getReloadableType(typeId);
		if (reloadableType == null) {
			reloadableType = searchForReloadableType(typeId, typeRegistry);
		}
		// Check 2: Info computed earlier
		if (reloadableType != null && !reloadableType.isAffectedByReload()) {
			return false;
		}
		if (reloadableType != null && reloadableType.hasBeenReloaded()) {
			MethodMember method = reloadableType.getLiveVersion().incrementalTypeDescriptor
					.getFromLatestByDescriptor(nameAndDescriptor);
			boolean dispatchThroughDescriptor = false;
			if (method == null) {
				// method does not exist
				throw new NoSuchMethodError(reloadableType.getBaseName() + "." + nameAndDescriptor);
			}
			else if (IncrementalTypeDescriptor.isBrandNewMethod(method)) {
				// definetly need to use the dispatcher
				dispatchThroughDescriptor = true;
			}
			if (dispatchThroughDescriptor) {
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
					log.info("versionstamp " + reloadableType.getLiveVersion().versionstamp);
					log.exiting("TypeRegistry", "iincheck", true);
				}
				return true;
			}
		}
		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
			log.exiting("TypeRegistry", "icheck", false);
		}
		return false;
	}

	/*
	 * notes on ivicheck.
	 * ivicheck is the guard call placed on invokevirtual operations.   The basic principal question it asks is
	 * "can i call what I was going to call, or not?"
	 * The answer to that question primarily depends on whether the method was previously defined in the target hierarchy.  If it was then
	 * yes, make the call and let catchers sort it out.  If not then we need to jump through firey hoops.
	 * 
	 * For example, this code:
	 * public int run1() {
		XX zz = new ZZ();
		return zz.foo();
	   }
	 * 
	 * results in this invokevirtual:
	 * 
	  INVOKEVIRTUAL invokevirtual/XX.foo()I
	 * 
	 * Notice the static type of the variable is used in the method descriptor for the invoke.
	 * 
	 * The rewriter then turns it into this:
	LDC 65537
	LDC foo()I
	INVOKESTATIC org/springsource/loaded/TypeRegistry.vcheck(ILjava/lang/String;)Z
	IFEQ L4
	DUP
	ACONST_NULL
	SWAP
	LDC foo()I
	INVOKEVIRTUAL invokevirtual/XX.__execute([Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
	CHECKCAST java/lang/Integer
	INVOKEVIRTUAL java/lang/Integer.intValue()I
	GOTO L5
	L4
	INVOKEVIRTUAL invokevirtual/XX.foo()I
	L5
	 *
	 * What that says is: call ivicheck for 65537,foo()I (65537 embodies the type registry id and the class ID, XX in our case, as per the descriptor).
	 * 
	 * vcheck should return true for methods that do not exist - since we can't run the invokevirtual
	 * 
	 * If vcheck returns false, do what you were going to do anyway:
	 *   this will actually cause us to jump into a catcher method.
	 * If vcheck returns true, call the __execute() method on the type XX - however, due to virtual dispatch and all the types implementing __execute() we
	 * will end up in the one for the dynamic type (ZZ.__execute())
	 * 
	 * These two paths proceed as follows.
	 * 
	 * 1) If we jumped into a catcher method, we actually hit the catcher ZZ.foo()
	 * The catcher works as follows - grab the latest version of this type (if it has been reloaded) and call foo() on the dispatcher, otherwise call super.foo().
	 * The catcher exists because the type did not originally implement the method.  It exists to enable the type to implement the method later.  The same sequence
	 * will continue (through catchers) until a type is hit that provides an implementation which did not used to, or an original implementation is hit, or we run out
	 * of options and an NSME is created.  The catcher code is below.
	 * 
	 * 2) In the ZZ.__execute() method we actually ask the type registry to tell us what to call - we call determineDispatcher which uses the runtime type for the call
	 * and discovers which dispatcher should be used.  it is a bit naughty in that if it finds an reloadabletype that is the right answer but that hasn't been reloaded,
	 * it forces a reload of the original code to create a dispatcher instance that can be returned.
	 * 
	 * __execute is for methods that were never there at all
	 * 
	METHOD: 0x0001(public) foo()I
	CODE
	GETSTATIC invokevirtual/ZZ.r$type Lorg/springsource/loaded/ReloadableType;
	LDC 0
	INVOKEVIRTUAL org/springsource/loaded/ReloadableType.fetchLatestIfExists(I)Ljava/lang/Object;
	DUP
	IFNULL L0
	CHECKCAST invokevirtual/ZZ__I
	ALOAD 0
	INVOKEINTERFACE invokevirtual/ZZ__I.foo(Linvokevirtual/ZZ;)I
	IRETURN
	L0
	POP
	ALOAD 0
	INVOKESPECIAL invokevirtual/YY.foo()I
	IRETURN
	 * 
	 * 
	 * 
	 */

	/**
	 * Called for a field operation - trying to determine whether a particular field needs special handling.
	 * 
	 * @param ids packed representation of the registryId (top 16bits) and typeId (bottom 16bits)
	 * @param name the name of the instance field about to be accessed
	 * @return true if the field operation must be intercepted
	 */
	@UsedByGeneratedCode
	public static boolean instanceFieldInterceptionRequired(int ids, String name) {
		if (nothingReloaded) {
			return false;
		}
		int registryId = ids >>> 16;
		int typeId = ids & 0xffff;
		TypeRegistry typeRegistry = registryInstances[registryId].get();
		ReloadableType reloadableType = typeRegistry.getReloadableType(typeId);
		// TODO covers all situations?
		if (reloadableType != null) {
			if (reloadableType.hasFieldChangedInHierarchy(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Called for a field operation - trying to determine whether a particular field needs special handling.
	 *
	 * @param ids packed representation of the registryId (top 16bits) and typeId (bottom 16bits)
	 * @param name the name of the static field about to be accessed
	 * @return true if the field operation must be intercepted
	 */
	@UsedByGeneratedCode
	public static boolean staticFieldInterceptionRequired(int ids, String name) {
		if (TypeRegistry.nothingReloaded) {
			return false;
		}
		int registryId = ids >>> 16;
		int typeId = ids & 0xffff;
		TypeRegistry typeRegistry = registryInstances[registryId].get();
		ReloadableType reloadableType = typeRegistry.getReloadableType(typeId);
		// TODO all scenarios covered?
		if (reloadableType != null) {
			if (reloadableType.hasFieldChangedInHierarchy(name)) {
				//				System.out.println("Checking if field changed in hierarchy for " + name + " = yes");
				return true;
			}
			//			System.out.println("Checking if field changed in hierarchy for " + name + "= no");
		}
		return false;
	}

	@UsedByGeneratedCode
	public static Object idyrun(Object[] indyParams, int typeRegistryId, int classId, Object caller,
			String nameAndDescriptor, int bsmId) {
		// Typical next line: lookup=basic.LambdaA nameAD=m()Lbasic/LambdaA$Foo; bsmId=0
		// System.err.println("idyrun("+caller+","+nameAndDescriptor+","+bsmId+")");
		// TODO Currently leaking entries in bsmmap with reloads (new ones get added, old ones not removed)
		ReloadableType rtype = TypeRegistry.getReloadableType(typeRegistryId, classId);
		BsmInfo bsmi = bsmmap.get(rtype.getSlashedName())[bsmId];
		return Java8.emulateInvokeDynamic(rtype, rtype.getLatestExecutorClass(), bsmi.bsm, bsmi.bsmArgs, caller,
				nameAndDescriptor, indyParams);
	}

	/**
	 * Used in code the generated code replaces invokevirtual calls. Determine if the code can run as it was originally
	 * compiled.
	 * 
	 * This method will return FALSE if nothing has changed to interfere with the invocation and it should proceed. This
	 * method will return TRUE if something has changed and the caller needs to do something different.
	 *
	 * @param ids packed representation of the registryId (top 16bits) and typeId (bottom 16bits)
	 * @param nameAndDescriptor the name and descriptor of the method about to be INVOKEVIRTUAL'd
	 * @return true if the original method operation must be intercepted
	 */
	@UsedByGeneratedCode
	public static boolean ivicheck(int ids, String nameAndDescriptor) {
		// Check 1: FAST: Has anything at all been reloaded?
		if (nothingReloaded) {
			return false;
		}
		//		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
		//			log.entering("TypeRegistry", "ivicheck", new Object[] { ids, nameAndDescriptor });
		//		}

		// TODO [perf] global check (anything been reloaded?)
		// TODO [perf] local check (type or anything in its hierarchy reloaded)

		int registryId = ids >>> 16;
		int typeId = ids & 0xffff;
		TypeRegistry typeRegistry = registryInstances[registryId].get();
		ReloadableType reloadableType = typeRegistry.getReloadableType(typeId);


		// Ok, think about what null means here.  It means this registry has not loaded this type as a reloadable type.  That doesn't
		// mean it isn't reloadable as a parent loaded may have found it.  We have 3 options:
		// 1. assume names are unique - we can look up this type and find the registry in question
		// 2. assume delegating classloaders and search the parent registry for it
		// 3. pass something in at the call site (the class obejct), this would give us the classloader and thus the registry

		// 3 is ideal, but slower.  2 is nice but not always safe.  1 will work in a lot of situations.
		// let's try with a (2) strategy, fallback on a (1) - when we revisit this we can end up doing (3) maybe...

		// TODO [grails] We need a sentinel to indicate that we've had a look, so that we dont go off searching every time, but for now, lets
		// just do the search:
		if (reloadableType == null) {
			reloadableType = searchForReloadableType(typeId, typeRegistry);
		}

		// Check 2: Info computed earlier
		if (reloadableType != null && !reloadableType.isAffectedByReload()) {
			return false;
		}

		if (reloadableType != null && reloadableType.hasBeenReloaded()) {
			MethodMember method = reloadableType.getLiveVersion().incrementalTypeDescriptor
					.getFromLatestByDescriptor(nameAndDescriptor);
			boolean dispatchThroughDescriptor = false;
			if (method == null) {
				if (!reloadableType.getTypeDescriptor().isFinalInHierarchy(nameAndDescriptor)) {
					// Reloading has occurred and method does not exist in new version, throw NSME
					throw new NoSuchMethodError(reloadableType.getBaseName() + "." + nameAndDescriptor);
				}
			}
			else if (IncrementalTypeDescriptor.isBrandNewMethod(method)) {
				// Reloading has occurred and method has been added (it wasn't in the original) definetly need to use the dispatcher
				dispatchThroughDescriptor = true;
			}
			else if (IncrementalTypeDescriptor.hasChanged(method)) {
				// Reloading has occurred and the method has changed in some way
				// Method has been deleted - let the catcher/new generated dispatcher deal with it
				if (!IncrementalTypeDescriptor.isCatcher(method)) {
					if (!IncrementalTypeDescriptor.wasDeleted(method)) {
						// Don't want to call the one that was there!
						dispatchThroughDescriptor = true;
					}
					//				} else if (IncrementalTypeDescriptor.wasDeleted(method)) {
					//					// The method is a catcher because it used to be there, it no longer is
					//					dispatchThroughDescriptor = true;
				}
			}
			if (dispatchThroughDescriptor) {
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
					log.info("versionstamp " + reloadableType.getLiveVersion().versionstamp);
					log.exiting("TypeRegistry", "ivicheck", true);
				}
				return true;
			}
		}
		//		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
		//			log.exiting("TypeRegistry", "ivicheck", true);
		//		}
		return false;
	}

	private String getTypeById(int typeId) {
		return NameRegistry.getTypenameById(typeId);
	}

	/**
	 * This method discovers the reloadable type instance for the registry and type id specified.
	 * 
	 * @param typeRegistryId the type registry id
	 * @param typeId the type id
	 * @return the ReloadableType (if there is no ReloadableType an exception will be thrown)
	 */
	@UsedByGeneratedCode
	public static ReloadableType getReloadableType(int typeRegistryId, int typeId) {
		if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
			log.info(">TypeRegistry.getReloadableType(typeRegistryId=" + typeRegistryId + ",typeId=" + typeId + ")");
		}
		TypeRegistry typeRegistry = registryInstances[typeRegistryId].get();
		if (typeRegistry == null) {
			throw new IllegalStateException("Request to access registry id " + typeRegistryId
					+ " but no registry with that id has been created");
		}
		ReloadableType reloadableType = typeRegistry.getReloadableType(typeId);
		if (reloadableType == null) {
			throw new IllegalStateException("The type registry " + typeRegistry + " does not know about type id "
					+ typeId);
		}
		reloadableType.setResolved();
		if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
			log.info("<TypeRegistry.getReloadableType(typeRegistryId=" + typeRegistryId + ",typeId=" + typeId
					+ ") returning " + reloadableType);
		}
		reloadableType.createTypeAssociations();
		return reloadableType;
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("TypeRegistry(id=");
		s.append(System.identityHashCode(this));
		s.append(",loader=" + classLoader.get().getClass().getName());
		s.append(")");
		return s.toString();
	}

	private FileChangeListener fileChangeListener;

	private FileSystemWatcher fsWatcher;

	private Set<String> watching = new HashSet<String>();

	public void monitorForUpdates(ReloadableType rtype, String externalForm) {
		if (externalForm.charAt(1) == ':') {
			externalForm = Character.toLowerCase(externalForm.charAt(0)) + externalForm.substring(1);
		}

		// Check about rebasing the externalForm
		if (!rebasePaths.isEmpty()) {
			String forwardSlashForm = externalForm.replace('\\', '/');
			for (Map.Entry<String, String> path : rebasePaths.entrySet()) {
				System.out.println("Comparing " + forwardSlashForm + " with " + path.getKey());
				if (forwardSlashForm.startsWith(path.getKey())) {
					if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
						log.info("Rebasing from " + externalForm);
					}
					externalForm = path.getValue() + externalForm.substring(path.getKey().length());
					if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
						log.info("Now " + externalForm);
					}
				}
			}
		}

		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
			log.info("Called to monitor " + rtype.dottedtypename + " from " + externalForm);
		}

		if (!watching.contains(externalForm)) {
			// classFileToType.put(externalForm, rtype.slashedtypename);
			File f = new File(externalForm);
			if (fileChangeListener == null) {
				fileChangeListener = new ReloadableFileChangeListener(this);
			}
			if (fsWatcher == null) {
				fsWatcher = new FileSystemWatcher(fileChangeListener, id, getClassLoaderName());
			}
			fileChangeListener.register(rtype, f);
			fsWatcher.register(f);
			watching.add(externalForm);
		}
	}

	private String getClassLoaderName() {
		ClassLoader cl = getClassLoader();
		if (cl == null) {
			return "NULL";
		}
		else {
			return cl.toString();
		}
	}

	public boolean shouldRerunStaticInitializer(ReloadableType reloadableType, String versionsuffix) {
		// 'local' plugins
		for (Plugin plugin : localPlugins) {
			if (plugin instanceof ReloadEventProcessorPlugin) {
				if (((ReloadEventProcessorPlugin) plugin).shouldRerunStaticInitializer(reloadableType.getName(),
						reloadableType.getClazz(), versionsuffix)) {
					return true;
				}
			}
		}
		// 'global' plugins
		for (Plugin plugin : SpringLoadedPreProcessor.getGlobalPlugins()) {
			if (plugin instanceof ReloadEventProcessorPlugin) {
				if (((ReloadEventProcessorPlugin) plugin).shouldRerunStaticInitializer(reloadableType.getName(),
						reloadableType.getClazz(), versionsuffix)) {
					return true;
				}
			}
		}
		return false;
	}

	public void fireReloadEvent(ReloadableType reloadableType, String versionsuffix) {
		// 'local' plugins
		for (Plugin plugin : localPlugins) {
			if (plugin instanceof ReloadEventProcessorPlugin) {
				((ReloadEventProcessorPlugin) plugin).reloadEvent(reloadableType.getName(), reloadableType.getClazz(),
						versionsuffix);
			}
		}
		// 'global' plugins
		for (Plugin plugin : SpringLoadedPreProcessor.getGlobalPlugins()) {
			if (plugin instanceof ReloadEventProcessorPlugin) {
				((ReloadEventProcessorPlugin) plugin).reloadEvent(reloadableType.getName(), reloadableType.getClazz(),
						versionsuffix);
			}
		}
	}

	public boolean fireUnableToReloadEvent(ReloadableType reloadableType, TypeDelta td, String versionsuffix) {
		boolean calledSomething = false;
		// 'local' plugins
		for (Plugin plugin : localPlugins) {
			if (plugin instanceof UnableToReloadEventProcessorPlugin) {
				((UnableToReloadEventProcessorPlugin) plugin).unableToReloadEvent(reloadableType.getName(),
						reloadableType.getClazz(), td, versionsuffix);
				calledSomething = true;
			}
		}
		// 'global' plugins
		for (Plugin plugin : SpringLoadedPreProcessor.getGlobalPlugins()) {
			if (plugin instanceof UnableToReloadEventProcessorPlugin) {
				((UnableToReloadEventProcessorPlugin) plugin).unableToReloadEvent(reloadableType.getName(),
						reloadableType.getClazz(), td, versionsuffix);
				calledSomething = true;
			}
		}
		return calledSomething;
	}

	/**
	 * Process some type pattern objects from the supplied value. For example the value might be
	 * 'com.foo.Bar,!com.foo.Goo'
	 * 
	 * @param value string defining a comma separated list of type patterns
	 * @return list of TypePatterns
	 */
	private List<TypePattern> getPatternsFrom(String value) {
		if (value == null) {
			return Collections.emptyList();
		}
		List<TypePattern> typePatterns = new ArrayList<TypePattern>();
		StringTokenizer st = new StringTokenizer(value, ",");
		while (st.hasMoreElements()) {
			String typepattern = st.nextToken();
			TypePattern typePattern = null;
			if (typepattern.endsWith("..*")) {
				typePattern = new PrefixTypePattern(typepattern);
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
					log.info("registered package prefix '" + typepattern + "'");
				}
			}
			else if (typepattern.equals("*")) {
				typePattern = new AnyTypePattern();
			}
			else {
				typePattern = new ExactTypePattern(typepattern);
			}
			typePatterns.add(typePattern);
		}
		return typePatterns;
	}

	private Class<?> class_GroovySystem;

	private Class<?> class_ClassInfo;

	private Method method_ClassInfo_getClassInfo;

	private Field field_ClassInfo_cachedClassRef;

	public Class<?> getClass_GroovySystem() {
		if (class_GroovySystem == null) {
			try {
				class_GroovySystem = Class.forName("groovy.lang.GroovySystem", false, this.classLoader.get());
			}
			catch (ClassNotFoundException e) {
				new RuntimeException("Unable to located GroovySystem to reset type", e).printStackTrace();
			}
		}
		return class_GroovySystem;
	}

	public Class<?> getClass_ClassInfo() {
		if (class_ClassInfo == null) {
			try {
				class_ClassInfo = Class.forName("org.codehaus.groovy.reflection.ClassInfo", false,
						this.classLoader.get());
			}
			catch (ClassNotFoundException e) {
				new RuntimeException("Unable to located ClassInfo to reset type", e).printStackTrace();
			}
		}
		return class_ClassInfo;
	}

	public Method getMethod_ClassInfo_getClassInfo() {
		if (method_ClassInfo_getClassInfo == null) {
			Class<?> clazz = getClass_ClassInfo();
			try {
				method_ClassInfo_getClassInfo = clazz.getDeclaredMethod("getClassInfo", Class.class);
			}
			catch (Exception e) {
				new RuntimeException("Unable to located method getClassInfo to reset type", e).printStackTrace();
			}
		}
		return method_ClassInfo_getClassInfo;
	}

	public Field getField_ClassInfo_cachedClassRef() {
		if (field_ClassInfo_cachedClassRef == null) {
			Class<?> clazz = getClass_ClassInfo();
			try {
				field_ClassInfo_cachedClassRef = clazz.getDeclaredField("cachedClassRef");
			}
			catch (Exception e) {
				new RuntimeException("Unable to located field cachedClassRef to reset type", e).printStackTrace();
			}
		}
		return field_ClassInfo_cachedClassRef;
	}

	private long lastTidyup = 0;

	/**
	 * To avoid leaking permgen we want to periodically discard the child classloader and recreate a new one. We will
	 * need to then redefine types again over time as they are used (the most recent variants of them).
	 * 
	 * @param currentlyDefining the reloadable type currently being defined reloaded
	 */
	public void checkChildClassLoader(ReloadableType currentlyDefining) {
		ChildClassLoader ccl = childClassLoader == null ? null : childClassLoader.get();
		int definedCount = (ccl == null ? 0 : ccl.getDefinedCount());
		long time = System.currentTimeMillis();
		// Don't do this more than every 5 seconds - that allows for situations where a lot of types are being redefined all in one go
		if (definedCount > maxClassDefinitions && ((time - lastTidyup) > 5000)) {
			lastTidyup = time;
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				log.info("Recreating the typeregistry managed classloader, limit(#"
						+ GlobalConfiguration.maxClassDefinitions
						+ ") reached");
			}
			ccl = new ChildClassLoader(classLoader.get());
			this.childClassLoader = new WeakReference<ChildClassLoader>(ccl);
			// Need to tidy up all the links to this classloader!
			for (int i = 0; i < reloadableTypesSize; i++) {
				ReloadableType rtype = reloadableTypes[i];
				if (rtype != null && rtype != currentlyDefining) {
					rtype.clearClassloaderLinks();
					// TODO [performance] could avoid doing this now - that would mean we would have to do it
					// 'on demand' and that would add an extra check to every operation
					rtype.reloadMostRecentDispatcherAndExecutor();
				}
			}
			for (int i = 0; i < reloadableTypesSize; i++) {
				ReloadableType rtype = reloadableTypes[i];
				if (rtype != null && rtype != currentlyDefining && rtype.hasBeenReloaded()) {
					if (rtype.getLiveVersion().staticInitializedNeedsRerunningOnDefine) {
						rtype.runStaticInitializer();
					}
				}
			}
			// Up the limit if it is too low, or too much time will be spent constantly over the limit (and so reloading)
			int count = ccl.getDefinedCount() + 3;
			if (count > maxClassDefinitions) {
				maxClassDefinitions = count;
			}
		}
	}

	// FOR TESTING
	public ChildClassLoader getChildClassLoader() {
		return childClassLoader.get();
	}

	public boolean isResolved(Class<?> clazz) {
		String n = clazz.getName().replace('.', '/');
		ReloadableType rt = getReloadableType(n);
		if (rt == null) {
			throw new IllegalStateException("reloadable type not found for " + n);
		}
		return rt.isResolved();
	}

	public ReloadableType getReloadableType(Class<?> clazz) {
		for (int r = 0; r < reloadableTypesSize; r++) {
			ReloadableType rt = reloadableTypes[r];
			if (rt != null) {
				if (rt.getClazz() == clazz) {
					return rt;
				}
			}
		}
		return null;
	}

	public TypeRegistry getParentRegistry() {
		ClassLoader cl = classLoader.get();
		if (cl == null) { // GRAILS-10134
			return null;
		}
		else {
			return TypeRegistry.getTypeRegistryFor(cl.getParent());
		}
	}

	public ReloadableType[] getReloadableTypes() {
		return this.reloadableTypes;
	}

	public Set<ReloadableType> getJDKProxiesFor(String slashedInterfaceTypeName) {
		return jdkProxiesForInterface.get(slashedInterfaceTypeName);
	}


	/**
	 * When an invokedynamic instruction is reached, we allocate an id that recognizes that bsm and the parameters to
	 * that bsm. The index can be used when rewriting that invokedynamic
	 * 
	 * @param slashedClassName the slashed class name containing the bootstrap method
	 * @param bsm the bootstrap methods
	 * @param bsmArgs the bootstrap method arguments (asm types)
	 * @return id that represents this bootstrap method usage
	 */
	public synchronized int recordBootstrapMethod(String slashedClassName, Handle bsm, Object[] bsmArgs) {
		if (bsmmap == null) {
			bsmmap = new HashMap<String, BsmInfo[]>();
		}
		BsmInfo[] bsminfo = bsmmap.get(slashedClassName);
		if (bsminfo == null) {
			bsminfo = new BsmInfo[1];
			// TODO do we need BsmInfo or can we just use Handle directly?
			bsminfo[0] = new BsmInfo(bsm, bsmArgs);
			bsmmap.put(slashedClassName, bsminfo);
			return 0;
		}
		else {
			int len = bsminfo.length;
			BsmInfo[] newarray = new BsmInfo[len + 1];
			System.arraycopy(bsminfo, 0, newarray, 0, len);
			bsminfo = newarray;
			bsmmap.put(slashedClassName, bsminfo);
			bsminfo[len] = new BsmInfo(bsm, bsmArgs);
			return len;
		}
		// TODO [memory] search the existing bsmInfos for a matching one! Reuse!
	}

	private static Map<String, BsmInfo[]> bsmmap;

	static class BsmInfo {

		Handle bsm;

		Object[] bsmArgs;

		public BsmInfo(Handle bsm, Object[] bsmArgs) {
			this.bsm = bsm;
			this.bsmArgs = bsmArgs;
		}
	}

	/**
	 * Called from the static initializer of a reloadabletype, allowing it to connect itself to the parent type, such
	 * that when reloading occurs we can mark all relevant types in the hierarchy as being impacted by the reload.
	 * 
	 * @param child the ReloadableType actively being initialized
	 * @param parent the superclass of the reloadable type (may or may not be reloadable!)
	 */
	@UsedByGeneratedCode
	public static void associateReloadableType(ReloadableType child, Class<?> parent) {
		// TODO performance - can we make this cheaper?
		ClassLoader parentClassLoader = parent.getClassLoader();
		if (parentClassLoader == null) {
			return;
		}
		TypeRegistry parentTypeRegistry = TypeRegistry.getTypeRegistryFor(parent.getClassLoader());
		ReloadableType parentReloadableType = parentTypeRegistry.getReloadableType(parent);
		if (parentReloadableType != null) {
			parentReloadableType.recordSubtype(child);
		}
	}

}
