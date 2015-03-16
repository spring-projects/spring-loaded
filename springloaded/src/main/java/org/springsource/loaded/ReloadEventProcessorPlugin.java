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

/**
 * ReloadEventProcessor Plugins are called when a type is reloading. For information on registering them, see
 * {@link Plugin}
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public interface ReloadEventProcessorPlugin extends Plugin {

	/**
	 * Called when a type has been reloaded, allows the plugin to decide if the static initializer should be re-run for
	 * the reloaded type. If the reloaded type has a different static initializer, the new one is the one that will run.
	 * 
	 * @param typename the (dotted) type name, for example java.lang.String
	 * @param clazz the Class object that has been reloaded
	 * @param encodedTimestamp an encoded time stamp for this version, containing chars (A-Za-z0-9)
	 * @return true if the static initializer should be re-run
	 */
	boolean shouldRerunStaticInitializer(String typename, Class<?> clazz, String encodedTimestamp);

	// TODO expose detailed delta for changes in the type? (i.e. what new fields/methods/etc)
	// TODO expose instances when they are being tracked?
	/**
	 * Called when a type has been reloaded. Note, the class is only truly defined to the VM once, and so the Class
	 * object (clazz parameter) is always the same for the same type (ignoring multiple classloader situations). It is
	 * passed here so that plugins processing events can clear any cached state related to it. The encodedTimestamp is
	 * an encoding of the ID that the agent has assigned to this reloaded version of this type.
	 * 
	 * @param typename the (dotted) type name, for example java.lang.String
	 * @param clazz the Class object that has been reloaded
	 * @param encodedTimestamp an encoded time stamp for this version, containing chars (A-Za-z0-9)
	 */
	void reloadEvent(String typename, Class<?> clazz, String encodedTimestamp);

}
