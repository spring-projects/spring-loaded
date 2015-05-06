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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.LoadtimeInstrumentationPlugin;
import org.springsource.loaded.ReloadEventProcessorPlugin;


/**
 * Reloading plugin for 'poking' JVM classes that are known to cache reflective state. Some of the behaviour is switched
 * ON based on which classes are loaded. For example the Introspector clearing logic is only activated if the
 * Introspector gets loaded.
 *
 * @author Andy Clement
 * @since 0.7.3
 */
public class JVMPlugin implements ReloadEventProcessorPlugin, LoadtimeInstrumentationPlugin {

	private boolean pluginBroken = false;

	private boolean introspectorLoaded = false;

	private boolean threadGroupContextLoaded = false;

	private Field beanInfoCacheField;

	private Field declaredMethodCacheField;

	private Method putMethod;

	private Class<?> threadGroupContextClass;

	private Field threadGroupContext_contextsField; /* Map<ThreadGroup,ThreadGroupContext> */

	private Method threadGroupContext_removeBeanInfoMethod; /*  removeBeanInfo(Class<?> type) { */


	private void tidySerialization(Class<?> reloadedClass) {
		//		if (true) return;
		try {
			Class<?> clazz = Class.forName("java.io.ObjectStreamClass$Caches");
			Field localDescsField = clazz.getDeclaredField("localDescs");
			localDescsField.setAccessible(true);
			ConcurrentMap cm = (ConcurrentMap) localDescsField.get(null);
			// TODO [serialization] a bit extreme to wipe out everything
			cm.clear();
			// For some reason clearing the reflectors damages serialization - is it not a true cache?
			//			Field reflectorsField = clazz.getDeclaredField("reflectors");
			//			reflectorsField.setAccessible(true);
			//			cm = (ConcurrentMap)reflectorsField.get(null);
			//			cm.clear();
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
		catch (NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
		catch (SecurityException e) {
			throw new IllegalStateException(e);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}


		//		private static class Caches {
		//	        /** cache mapping local classes -> descriptors */
		//	        static final ConcurrentMap<WeakClassKey,Reference<?>> localDescs =
		//	            new ConcurrentHashMap<>();
		//
		//	        /** cache mapping field group/local desc pairs -> field reflectors */
		//	        static final ConcurrentMap<FieldReflectorKey,Reference<?>> reflectors =
		//	            new ConcurrentHashMap<>();
		//
		//	        /** queue for WeakReferences to local classes */
		//	        private static final ReferenceQueue<Class<?>> localDescsQueue =
		//	            new ReferenceQueue<>();
		//	        /** queue for WeakReferences to field reflectors keys */
		//	        private static final ReferenceQueue<Class<?>> reflectorsQueue =
		//	            new ReferenceQueue<>();
		//	    }
	}


	@SuppressWarnings({ "restriction", "unchecked" })
	public void reloadEvent(String typename, Class<?> clazz, String encodedTimestamp) {
		if (pluginBroken) {
			return;
		}
		tidySerialization(clazz);
		if (introspectorLoaded) {
			// Clear out the Introspector BeanInfo cache entry that might exist for this class

			boolean beanInfoCacheCleared = false;
			// In Java7 the AppContext stuff is gone, replaced by a ThreadGroupContext.
			// This code grabs the contexts map from the ThreadGroupContext object and clears out the bean info for the reloaded clazz
			if (threadGroupContextLoaded) { // In Java 7
				beanInfoCacheCleared = clearThreadGroupContext(clazz);
			}

			// GRAILS-9505 - had to introduce the flushFromCaches(). The appcontext we seem to be able to
			// access from AppContext.getAppContext() isn't the same one the Introspector will be using
			// so we can fail to clean up the cache.  Strangely calling getAppContexts() and clearing them
			// all (the code commented out below) doesn't fetch all the contexts. I'm sure it is a nuance of
			// app context handling but for now the introspector call is sufficient.
			// TODO doesn't this just only clear the beaninfocache for the thread the reload event
			// is occurring on? which may not be the thread that was actually using the cache.
			if (!beanInfoCacheCleared) {
				try {
					if (beanInfoCacheField == null) {
						beanInfoCacheField = Introspector.class.getDeclaredField("BEANINFO_CACHE");
					}
					beanInfoCacheField.setAccessible(true);
					Object key = beanInfoCacheField.get(null);
					Map<Class<?>, BeanInfo> map = (Map<Class<?>, BeanInfo>) sun.awt.AppContext.getAppContext().get(key);
					if (map != null) {
						if (GlobalConfiguration.debugplugins) {
							System.err.println("JVMPlugin: clearing out BeanInfo for " + clazz.getName());
						}
						map.remove(clazz);
					}

					//					Set<sun.awt.AppContext> appcontexts = sun.awt.AppContext.getAppContexts();
					//					for (sun.awt.AppContext appcontext: appcontexts) {
					//						map = (Map<Class<?>, BeanInfo>) appcontext.get(key);
					//						if (map != null) {
					//							if (GlobalConfiguration.debugplugins) {
					//								System.err.println("JVMPlugin: clearing out BeanInfo for " + clazz.getName());
					//							}
					//							map.remove(clazz);
					//						}
					//					}
					Introspector.flushFromCaches(clazz);
				}
				catch (NoSuchFieldException nsfe) {
					// this can happen on Java7 as the field isn't there any more, see the code above.
					System.out.println("Reloading: JVMPlugin: warning: unable to clear BEANINFO_CACHE, cant find field");
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

			// Clear out the declaredMethodCache that may exist for this class
			try {
				if (declaredMethodCacheField == null) {
					declaredMethodCacheField = Introspector.class.getDeclaredField("declaredMethodCache");
				}
				declaredMethodCacheField.setAccessible(true);
				Object theCache = declaredMethodCacheField.get(null);
				if (putMethod == null) {
					putMethod = theCache.getClass().getDeclaredMethod("put", Object.class, Object.class);
				}
				putMethod.setAccessible(true);

				if (GlobalConfiguration.debugplugins) {
					System.err.println("JVMPlugin: clearing out declaredMethodCache in Introspector for class "
							+ clazz.getName());
				}
				putMethod.invoke(theCache, clazz, null);
			}
			catch (NoSuchFieldException nsfe) {
				pluginBroken = true;
				System.out
						.println("Reloading: JVMPlugin: warning: unable to clear declaredMethodCache, cant find field (JDK update may fix it)");
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private boolean clearThreadGroupContext(Class<?> clazz) {
		boolean beanInfoCacheCleared = false;
		try {
			if (threadGroupContextClass == null) {
				threadGroupContextClass = Class.forName("java.beans.ThreadGroupContext", true,
						Introspector.class.getClassLoader());
			}
			if (threadGroupContextClass != null) {
				if (threadGroupContext_contextsField == null) {
					threadGroupContext_contextsField = threadGroupContextClass.getDeclaredField("contexts");
					threadGroupContext_removeBeanInfoMethod = threadGroupContextClass.getDeclaredMethod(
							"removeBeanInfo",
							Class.class);
				}
				if (threadGroupContext_contextsField != null) {
					threadGroupContext_contextsField.setAccessible(true);
					Object threadGroupContext_contextsField_value = threadGroupContext_contextsField.get(null);
					if (threadGroupContext_contextsField_value == null) {
						beanInfoCacheCleared = true;
					}
					else {
						if (threadGroupContext_contextsField_value instanceof Map) {
							// Indicates Java 7 up to rev21
							Map<?, ?> m = (Map<?, ?>) threadGroupContext_contextsField_value;
							Collection<?> threadGroupContexts = m.values();
							for (Object o : threadGroupContexts) {
								threadGroupContext_removeBeanInfoMethod.setAccessible(true);
								threadGroupContext_removeBeanInfoMethod.invoke(o, clazz);
							}
							beanInfoCacheCleared = true;
						}
						else {
							// At update Java7u21 it changes
							Class weakIdentityMapClazz = threadGroupContext_contextsField.getType();
							Field tableField = weakIdentityMapClazz.getDeclaredField("table");
							tableField.setAccessible(true);
							Reference<?>[] refs = (Reference[]) tableField.get(threadGroupContext_contextsField_value);
							Field valueField = null;
							if (refs != null) {
								for (int i = 0; i < refs.length; i++) {
									Reference<?> r = refs[i];
									Object o = (r == null ? null : r.get());
									if (o != null) {
										if (valueField == null) {
											valueField = r.getClass().getDeclaredField("value");
										}
										valueField.setAccessible(true);
										Object threadGroupContext = valueField.get(r);
										threadGroupContext_removeBeanInfoMethod.setAccessible(true);
										threadGroupContext_removeBeanInfoMethod.invoke(threadGroupContext, clazz);
									}
								}
							}
							beanInfoCacheCleared = true;
						}
					}
				}
			}
		}
		catch (Throwable t) {
			System.err.println("Unexpected problem clearing ThreadGroupContext beaninfo: ");
			t.printStackTrace();
		}
		return beanInfoCacheCleared;
	}

	public boolean accept(String slashedTypeName, ClassLoader classLoader, ProtectionDomain protectionDomain,
			byte[] bytes) {
		if (slashedTypeName != null) {
			if (slashedTypeName.equals("java/beans/Introspector")) {
				introspectorLoaded = true;
			}
			else if (slashedTypeName.equals("java/beans/ThreadGroupContext")) {
				threadGroupContextLoaded = true;
			}
		}
		return false;
	}

	public byte[] modify(String slashedClassName, ClassLoader classLoader, byte[] bytes) {
		return null;
	}

	public boolean shouldRerunStaticInitializer(String typename, Class<?> clazz, String encodedTimestamp) {
		return false;
	}

}
