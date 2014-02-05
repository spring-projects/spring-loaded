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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springsource.loaded.agent.SpringPlugin;

/**
 * Encapsulates configurable elements - these are set (to values other than the defaults) in TypeRegistry when the system property
 * springloaded configuration is processed. Some of the options are only used by testcases to make the testcases easier to write and
 * more straightforward.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class GlobalConfiguration {

	private static Logger log = Logger.getLogger(GlobalConfiguration.class.getName());

	/**
	 * Are references to fields being modified - covering both the GETS/SETS and the reflective references.
	 */
	public final static boolean fieldRewriting = true;

	public static boolean catchersOn = true;

	/**
	 * If active, SpringLoaded will be trying to watch for types changing on the file system once they have been made reloadable.
	 */
	public static boolean fileSystemMonitoring = false;

	/**
	 * Global control for loadtime logging
	 */
	public static boolean logging = false;

	/**
	 * verbose mode can trigger extra messages. Enable with 'verbose=true'
	 */
	public static boolean verboseMode = false;
	
	/**
	 * asserts mode will trigger extra checking (performance impact but confirms correctness)
	 */
	public static boolean assertsMode = false;
	
	/**
	 * Can be turned on to enable users to determine the decision process around why
	 * something is not reloadable.
	 */
	public static boolean explainMode = false;

	/**
	 * Global control for runtime logging
	 */
	public static boolean isRuntimeLogging = false;

	public static boolean callsideRewritingOn = true;

	/**
	 * Allows a cache to be cleaned up as the agent starts (effectively starting with a new cache, if 'caching' is true)
	 */
	public static boolean cleanCache = false;

	/**
	 * Determine whether on disk caching will be used.
	 */
	public static boolean isCaching = false;

	/**
	 * A well known profile (e.g. grails) can tweak a lot of the default options in a particular way.
	 */
	public static String profile = null;

	/**
	 * The base directory in which to create any cache (.slcache folder). If null then user.home will be used.
	 */
	public static String cacheDir = null;

	public final static boolean logNonInterceptedReflectiveCalls = false;

	/**
	 * Global control for checking assertions
	 */
	public final static boolean isProfiling = false;

	public static boolean directlyDefineTypes = true;

	public final static boolean interceptReflection = true;

	public static boolean reloadMessages = false;// can be forced on for testing

	/**
	 * When a reload is attempted, if this is true it will be checked to confirm it is allowed and does not violate the supported
	 * reloadable changes that can be made to a type.
	 */
	public static boolean verifyReloads = true;

	/**
	 * When classes are dumped by Utils.dump() this specifies where. A null value will cause us to dump into the default temp
	 * folder.
	 */
	public static String dumpFolder = null;

	/**
	 * Global configuration properties set based on the value of system property 'springloaded'. If null then not yet initialized
	 * (and a call to initializeFromSystemProperty()) is needed. If settings are truely once per VM, they are set directly in
	 * GlobalConfiguration whereas if they may be overridden on a per classloader level, they are set in this properties object and
	 * may be overridden by the springloaded.properties files accessible through each classloader.
	 */
	public static Properties globalConfigurationProperties;

	/**
	 * List of slashed classnames for types we should 'dump' during processing (for debugging purposes).
	 */
	public static List<String> classesToDump;

	public static int maxClassDefinitions = 100;

	/**
	 * List of dotted classnames representing classnames of plugins that should be loaded.
	 */
	public static List<String> pluginClassnameList;

	public final static boolean debugplugins;

	
	private static void printUsage() {
		System.out.println("SpringLoaded");
		System.out.println("============");
		System.out.println();
		System.out.println("Usage: java -noverify -javaagent:<pathto>/springloaded.jar");
		System.out.println("Optionally specify configuration through -Dspringloaded=<options>");
		System.out.println("<options> is a ';' separated list of directives or name=value options");
		System.out.println("Example: -Dspringloaded=verbose;cacheDir=/tmp");
		System.out.println();
		System.out.println("Directives:");
		System.out.println("  ? - print this usage text");
		System.out.println("  verbose - the reloader will log important lifecycle events");
		System.out.println("Options:");
		System.exit(0);
	}
	
	/**
	 * Look for a springloaded system property and initialize the 'default system wide' configuration based upon it.
	 * Support configuration options:
	 * <ul>
	 * <li><tt>info</tt> - print usage information on the options
	 * <li><tt>verbose</tt> - this directive causes SpringLoaded to report on decisions it is making.
	 * </ul>
	 */
	static {
		globalConfigurationProperties = new Properties();
		boolean debugPlugins = false;
		try {
			boolean specifiedCaching = false;
			String value = System.getProperty("springloaded");
			// value is a ';' separated list of configuration options which either may be name=value settings or directives (just a name)
			if (value != null) {
				StringTokenizer st = new StringTokenizer(value, ";");
				while (st.hasMoreTokens()) {
					String kv = st.nextToken();
					int equals = kv.indexOf('=');
					if (equals != -1) {
						// key=value
						String key = kv.substring(0, equals);
						// Supported settings:

						// dump=XX,YYY,ZZZ
						// - this option lists classes for which we should dump the bytecode, names are dotted
						if (key.equals("dump")) {
							String classList = kv.substring(equals + 1);
							StringTokenizer clSt = new StringTokenizer(classList, ",");
							classesToDump = new ArrayList<String>();
							while (clSt.hasMoreTokens()) {
								classesToDump.add(clSt.nextToken().replace('.', '/'));
							}
							if (isRuntimeLogging && log.isLoggable(Level.INFO)) {
								log.info("configuration: dumping: " + classesToDump);
							}
							//						} else if (key.equals("interceptReflection")) { // global setting
							//							interceptReflection = kv.substring(equals + 1).equalsIgnoreCase("true");
							//							if (isRuntimeLogging && log.isLoggable(Level.INFO)) {
							//								log.info("configuration: interceptReflection = " + interceptReflection);
							//							}
						} else if (key.equals("cleanCache")) {
							cleanCache = kv.substring(equals + 1).equalsIgnoreCase("true");
						} else if (key.equals("caching")) {
							specifiedCaching = true;
							isCaching = kv.substring(equals + 1).equalsIgnoreCase("true");
						} else if (key.equals("debugplugins")) {
							debugPlugins = true;
						} else if (key.equals("profile")) {
							profile = kv.substring(equals + 1);
						} else if (key.equals("cacheDir")) {
							cacheDir = kv.substring(equals + 1);
						} else if (key.equals("callsideRewritingOn")) { // global setting
							callsideRewritingOn = kv.substring(equals + 1).equalsIgnoreCase("true");
							if (isRuntimeLogging && log.isLoggable(Level.INFO)) {
								log.info("configuration: callsideRewritingOn = " + callsideRewritingOn);
							}
							//						} else if (key.equals("logNonInterceptedReflectiveCalls")) { // global setting
							//							logNonInterceptedReflectiveCalls = kv.substring(equals + 1).equalsIgnoreCase("true");
							//							if (isRuntimeLogging && log.isLoggable(Level.INFO)) {
							//								log.info("configuration: logNonInterceptedReflectiveCalls = " + logNonInterceptedReflectiveCalls);
							//							}
						} else if (key.equals("verifyReloads")) { // global setting
							verifyReloads = kv.substring(equals + 1).equalsIgnoreCase("true");
							if (isRuntimeLogging && log.isLoggable(Level.INFO)) {
								log.info("configuration: verifyReloads = " + verifyReloads);
							}
						} else if (key.equals("dumpFolder")) { // global setting
							dumpFolder = kv.substring(equals + 1);
							if (isRuntimeLogging && log.isLoggable(Level.INFO)) {
								log.info("configuration: dumpFolder = " + dumpFolder);
							}
						} else if (key.equals("maxClassDefinitions")) {
							try {
								maxClassDefinitions = Integer.parseInt(kv.substring(equals + 1));
								if (isRuntimeLogging && log.isLoggable(Level.INFO)) {
									log.info("configuration: maxClassDefinitions = " + maxClassDefinitions);
								}
							} catch (NumberFormatException nfe) {
								System.err.println("ERROR: unable to parse " + kv.substring(equals + 1) + " as a integer");
							}
						} else if (key.equals("logging")) {
							GlobalConfiguration.isRuntimeLogging = kv.substring(equals + 1).equalsIgnoreCase("true");
							GlobalConfiguration.logging = kv.substring(equals + 1).equalsIgnoreCase("true");
							System.out.println("Spring-Loaded logging = (" + GlobalConfiguration.isRuntimeLogging + ","
									+ GlobalConfiguration.logging + ")");
						} else if (key.equals("verbose")) {
							verboseMode = kv.substring(equals + 1).equalsIgnoreCase("true");
							reloadMessages = verboseMode;
						}
						else if (key.equals("asserts")) {
							assertsMode = kv.substring(equals + 1).equalsIgnoreCase("true");
						}
						else if (key.equals("rebasePaths")) {
							// value is a series of "a=b,c=d,e=f" indicating from and to
							globalConfigurationProperties.put("rebasePaths", kv.substring(equals + 1));
						} else if (key.equals("inclusions")) {
							globalConfigurationProperties.put("inclusions", kv.substring(equals + 1));
						} else if (key.equals("exclusions")) {
							globalConfigurationProperties.put("exclusions", kv.substring(equals + 1));
						} else if (key.equals("plugins")) {
							// plugins=com.myplugin.Plugin,com.somethingelse.SomeOtherPlugin
							String pluginList = kv.substring(equals + 1);
							StringTokenizer pluginListTokenizer = new StringTokenizer(pluginList, ",");
							pluginClassnameList = new ArrayList<String>();
							while (pluginListTokenizer.hasMoreTokens()) {
								pluginClassnameList.add(pluginListTokenizer.nextToken());
							}
						}
					} else {
						if (kv.equals("?")) {
							printUsage();
						}
						else if (kv.equals("verbose")) {
							Log.log("[verbose mode on] Full configuration is:"+value);
							verboseMode = true;
							reloadMessages = true;
						}
						else if (kv.equals("asserts")) {
							Log.log("[asserts mode on] Will verify system coherence");
							assertsMode = true;
						}
						else if (kv.equals("explain")) {
							Log.log("[explain mode on] Reporting on the decision making process within SpringLoaded");
							explainMode = true;
						}
					}
				}
			}

			// Profile support.  A profile is a shortcut for configuring a bunch of options
			if (profile != null) {
				if (profile.equals("grails")) {
					// Configure options based on a grails profile
					// turn on caching if we have a cacheDir set or can put one in the .grails folder under user.home
					if (cacheDir == null) {
						try {
							String userhome = System.getProperty("user.home");
							if (userhome != null) {
								cacheDir = new StringBuilder(userhome).append(File.separator).append(".grails").toString();
								new File(cacheDir).mkdir();
							}
						} catch (Throwable t) {
							System.err.println("looks like user.home is not set, or cannot write to it: cannot create cache.");
							t.printStackTrace(System.err);
						}
					}
					if (!specifiedCaching) {
						if (cacheDir != null) {
							isCaching = true;
						}
					}
					if (pluginClassnameList == null) {
						pluginClassnameList = new ArrayList<String>();
					}
					pluginClassnameList.add("org.springsource.loaded.SystemPropertyConfiguredIsReloadableTypePlugin");
					// turn off the 3.0 reloading, for now (just because it hasn't been tested)
					SpringPlugin.support305 = false;
				}
			} else {
				if (isCaching) {
					try {
						String userhome = System.getProperty("user.home");
						if (userhome != null) {
							cacheDir = userhome;
						}
					} catch (Throwable t) {
						System.err.println("looks like user.home is not set: cannot create cache.");
						t.printStackTrace(System.err);
					}
				}
			}
			if (isCaching) {
				// Ensure cache folder exists
				try {
					File cacheDirFile = new File(cacheDir);
					if (!cacheDirFile.exists()) {
						boolean created = cacheDirFile.mkdirs();
						if (!created) {
							System.err.println("Caching deactivated: failed to create cache directory: " + cacheDir);
							isCaching = false;
						}
					} else {
						if (!cacheDirFile.isDirectory()) {
							System.err.println("Caching deactivated: unable to use specified cache area, it is not a directory: "
									+ cacheDirFile);
							isCaching = false;
						}
					}
				} catch (Exception e) {
					System.err.println("Unexpected problem creating specified cachedir: " + cacheDir);
					e.printStackTrace();
				}
			}
		} catch (Throwable t) {
			System.err.println("Unexpected problem reading global configuration setting:" + t.toString());
			t.printStackTrace();
		}
		debugplugins = debugPlugins;
	}
}
