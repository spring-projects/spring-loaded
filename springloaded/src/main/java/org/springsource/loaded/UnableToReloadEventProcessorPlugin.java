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
 * UnableToReloadEventProcessor Plugins are called when a type cannot be reloaded due to an unsupported change. For
 * information on registering them, see {@link Plugin}
 * 
 * @author Andy Clement
 * @since 0.7.3
 */
public interface UnableToReloadEventProcessorPlugin extends Plugin {

	/**
	 * Called when a type cannot be reloaded, due to a change being made that is not supported by the agent, for example
	 * when the set of interfaces for a type is changed. Note, the class is only truly defined to the VM once, and so
	 * the Class object (clazz parameter) is always the same for the same type (ignoring multiple classloader
	 * situations). It is passed here so that plugins processing events can clear any cached state related to it. The
	 * encodedTimestamp is an encoding of the ID that the agent has assigned to this reloaded version of this type. The
	 * TypeDelta (a work in progress) captures details about what changed in the type that could not be reloaded.
	 * 
	 * @param typename the (dotted) type name, for example java.lang.String
	 * @param clazz the Class object that has been reloaded
	 * @param typeDelta encapsulates information about the changes made in this version of the type that prevented the
	 *            reload
	 * @param encodedTimestamp an encoded time stamp for this version, containing chars (A-Za-z0-9)
	 */
	void unableToReloadEvent(String typename, Class<?> clazz, TypeDelta typeDelta, String encodedTimestamp);
}
