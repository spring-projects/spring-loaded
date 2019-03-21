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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.LoadtimeInstrumentationPlugin;
import org.springsource.loaded.ReloadEventProcessorPlugin;


/**
 * 
 * 
 * @author Andy Clement
 * @since 0.7.3
 */
public class GrailsPlugin implements LoadtimeInstrumentationPlugin, ReloadEventProcessorPlugin {

	//	private static Logger log = Logger.getLogger(GrailsPlugin.class.getName());

	private static final String DefaultClassPropertyFetcher = "org/codehaus/groovy/grails/commons/ClassPropertyFetcher";

	private static List<WeakReference<Object>> classPropertyFetcherInstances = new ArrayList<WeakReference<Object>>();

	private static ReferenceQueue<Object> rq = new ReferenceQueue<Object>();

	/**
	 * @return true for types this plugin would like to change on startup
	 */
	public boolean accept(String slashedTypeName, ClassLoader classLoader, ProtectionDomain protectionDomain,
			byte[] bytes) {
		// TODO take classloader into account?
		return false;//DefaultClassPropertyFetcher.equals(slashedTypeName);
	}

	public byte[] modify(String slashedClassName, ClassLoader classLoader, byte[] bytes) {
		return PluginUtils.addInstanceTracking(bytes, "org/springsource/loaded/agent/GrailsPlugin");
	}

	// called by the modified code
	public static void recordInstance(Object obj) {
		// obj will be a ClassPropertyFetcher instance
		System.err.println("new instance queued " + System.identityHashCode(obj));
		// TODO urgent - race condition here, can create Co-modification problem if adding whilst another thread is processing
		classPropertyFetcherInstances.add(new WeakReference<Object>(obj, rq));
	}

	private Field classPropertyFetcher_clazz;

	private Method classPropertyFetcher_init;

	public void reloadEvent(String typename, Class<?> reloadedClazz, String versionsuffix) {
		// Clear references to objects that have been GCd
		// Do they ever get cleared out??
		Reference<?> r = rq.poll();
		while (r != null) {
			classPropertyFetcherInstances.remove(r);
			r = rq.poll();
		}
		try {
			// Currently not needing to track classPropertyFetcherInstances
			for (WeakReference<Object> ref : classPropertyFetcherInstances) {
				Object instance = ref.get();
				if (instance != null) {
					if (classPropertyFetcher_clazz == null) {
						classPropertyFetcher_clazz = instance.getClass().getDeclaredField("clazz");
					}
					classPropertyFetcher_clazz.setAccessible(true);
					Class<?> clazz = (Class<?>) classPropertyFetcher_clazz.get(instance);
					if (clazz == reloadedClazz) {
						if (classPropertyFetcher_init == null) {
							classPropertyFetcher_init = instance.getClass().getDeclaredMethod("init");
						}
						classPropertyFetcher_init.setAccessible(true);
						classPropertyFetcher_init.invoke(instance);
						if (GlobalConfiguration.debugplugins) {
							System.err.println("GrailsPlugin: re-initing classPropertyFetcher instance for "
									+ clazz.getName()
									+ " " + System.identityHashCode(instance));
						}
						//						System.out.println("re-initing " + reloadedClazz.getName());
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean shouldRerunStaticInitializer(String typename, Class<?> clazz, String encodedTimestamp) {
		return false;
	}
}
