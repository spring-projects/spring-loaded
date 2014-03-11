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
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.springsource.loaded.agent.ReloadDecision;


/**
 * This is not a 'default' plugin, it must be registered by specifying the following on the springloaded option:
 * "plugins=org.springsource.loaded.SystemPropertyConfiguredIsReloadableTypePlugin". The behaviour of this plugin is configured by a
 * system property that is constantly checked (not cached), this property determines whether files in certain paths are reloadable
 * or not.
 * 
 * @author Andy Clement
 * @since 0.7.3
 */
public class SystemPropertyConfiguredIsReloadableTypePlugin implements IsReloadableTypePlugin {

	public final static boolean debug;

	static {
		boolean value = false;
		try {
			value = System.getProperty("springloaded.directoriesContainingReloadableCode.debug", "false").equalsIgnoreCase("true");
		} catch (Exception e) {

		}
		debug = value;
	}

	public SystemPropertyConfiguredIsReloadableTypePlugin() {
		if (debug) {
			System.out.println("SystemPropertyConfiguredIsReloadableTypePlugin: instantiated");
		}
	}

	private List<String> includes = new ArrayList<String>();
	private List<String> excludes = new ArrayList<String>();
	private String mostRecentReloadableDirs = null;

	// TODO need try/catch protection when calling plugins, in case of bad ones
	public ReloadDecision shouldBeMadeReloadable(TypeRegistry typeRegistry, String typename, ProtectionDomain protectionDomain, byte[] bytes) {
		if (debug) {
			System.out.println("SystemPropertyConfiguredIsReloadableTypePlugin: entered, for typename " + typename);
		}
		if (protectionDomain == null) {
			return ReloadDecision.PASS;
		}
		String reloadableDirs = System.getProperty("springloaded.directoriesContainingReloadableCode");
		if (debug) {
			System.out.println("SystemPropertyConfiguredIsReloadableTypePlugin: reloadableDirs=" + reloadableDirs);
		}
		if (reloadableDirs == null) {
			return ReloadDecision.PASS;
		} else {
			if (mostRecentReloadableDirs != reloadableDirs) {
				synchronized (includes) {
					if (mostRecentReloadableDirs != reloadableDirs) {
						includes.clear();
						excludes.clear();
						// update our cached information
						StringTokenizer st = new StringTokenizer(reloadableDirs, ",");
						while (st.hasMoreTokens()) {
							String nextDir = st.nextToken();
							boolean isNot = nextDir.charAt(0) == '!';
							if (isNot) {
								excludes.add(nextDir.substring(1));
							} else {
								includes.add(nextDir);
							}
						}
						mostRecentReloadableDirs = reloadableDirs;
					}
				}
			}
		}
		// Typical example:
		// typename = com/vmware/rabbit/HomeController
		// codeSource.getLocation() = file:/Users/aclement/springsource/tc-server-developer-2.1.1.RELEASE/spring-insight-instance/wtpwebapps/hello-rabbit-client/WEB-INF/classes/com/vmware/rabbit/HomeController.class
		CodeSource codeSource = protectionDomain.getCodeSource();
		if (codeSource == null || codeSource.getLocation() == null) {
			return ReloadDecision.NO; // nothing to watch...
			//			if (debug) {
			//				System.out.println("SystemPropertyConfiguredIsReloadableTypePlugin: " + typename + " does not have a codeSource");
			//			}
		} else {
			// May have to do something special for CGLIB types
			// These will have a type name of something like: grails/plugin/springsecurity/SpringSecurityService$$EnhancerByCGLIB$$8f956be2
			// But a codesource location of file:/Users/aclement/.m2/repository/org/springframework/spring-core/3.2.5.RELEASE/spring-core-3.2.5.RELEASE.jar
			int cglibIndex = typename.indexOf("$$EnhancerBy");
			if (cglibIndex == -1) {
				cglibIndex = typename.indexOf("$$FastClassBy");
			}
			if (cglibIndex != -1) {
				String originalType = typename.substring(0, typename.indexOf("$$")); // assuming first $$ is good enough
				while (typeRegistry != null) {
					ReloadableType originalReloadable = typeRegistry.getReloadableType(originalType);
					if (originalReloadable != null) {
						return ReloadDecision.YES;
					}
					typeRegistry = typeRegistry.getParentRegistry();
				}
			}

			if (debug) {
				System.out.println("SystemPropertyConfiguredIsReloadableTypePlugin: " + typename + " codeSource.getLocation() is "
						+ codeSource.getLocation());
			}
		}
		try {
			URI uri = codeSource.getLocation().toURI();
			File file = new File(uri);
			String path = file.toString();

			synchronized (includes) {
				for (String exclude : excludes) {
					if (path.contains(exclude)) {
						if (debug) {
							System.out.println("SystemPropertyConfiguredIsReloadableTypePlugin: " + typename
									+ " is not being made reloadable");
						}
						return ReloadDecision.NO;
					}
				}
	
				for (String include : includes) {
					if (path.contains(include)) {
						if (debug) {
							System.out.println("SystemPropertyConfiguredIsReloadableTypePlugin: " + typename
									+ " is being made reloadable");
						}
						return ReloadDecision.YES;
					}
				}
			}
			
			//			StringTokenizer st = new StringTokenizer(reloadableDirs, ",");
			//			while (st.hasMoreTokens()) {
			//				String nextDir = st.nextToken();
			//				boolean isNot = nextDir.charAt(0) == '!';
			//				if (isNot)
			//					nextDir = nextDir.substring(1);
			//				if (path.contains(nextDir)) {
			//					if (isNot) {
			//						if (debug) {
			//							System.out.println("SystemPropertyConfiguredIsReloadableTypePlugin: " + typename
			//									+ " is not being made reloadable");
			//						}
			//						return ReloadDecision.NO;
			//					} else {
			//						if (debug) {
			//							System.out.println("SystemPropertyConfiguredIsReloadableTypePlugin: " + typename
			//									+ " is being made reloadable");
			//						}
			//						return ReloadDecision.YES;
			//					}
			//				}
			//			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException iae) {
			// grails-9654
			// On File.<init>() call:
			// IAE: URI is not hierarchical
			if (debug) {
				try {
					System.out.println("IllegalArgumentException: URI is not hierarchical, uri is "+codeSource.getLocation().toURI());
				} catch (URISyntaxException use) {
					System.out.println("IllegalArgumentException: URI is not hierarchical, uri is "+codeSource.getLocation());
				}
			}
		}
		if (debug) {
			System.out.println("SystemPropertyConfiguredIsReloadableTypePlugin: " + typename + " is being PASSed on");
		}
		return ReloadDecision.PASS;
	}

}
