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

package org.springsource.loaded;

import java.security.ProtectionDomain;

/**
 * Plugins that implement this interface are allowed to modify types as they are loaded - this can be necessary
 * sometimes to ensure, for example, that a particular field is accessible later when a reload event occurs or that some
 * factory method returns a wrapper rather than the original object it intended to. For information on how to register
 * plugins with the agent, see {@link Plugin}
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public interface LoadtimeInstrumentationPlugin extends Plugin {

	// TODO should probably be dotted names rather than slashed
	/**
	 * Called by the agent to determine if this plugin is interested in changing the specified type at load time. This
	 * is used when the plugin wishes to do some kind of transformation itself before the type is loaded - for example
	 * modify it to record something that will later be used on a reload event.
	 * 
	 * @param slashedTypeName the type name, slashed form (e.g. java/lang/String)
	 * @param classLoader the classloader loading the type
	 * @param protectionDomain the ProtectionDomain for the class represented by the bytes
	 * @param bytes the classfile contents for the type
	 * @return true if this plugin wants to change the bytes for the named type
	 */
	boolean accept(String slashedTypeName, ClassLoader classLoader, ProtectionDomain protectionDomain, byte[] bytes);

	/**
	 * Once accept has returned true for a type, the modify method will be called to make the actual change to the
	 * classfile bytes.
	 * 
	 * @param slashedClassName the class name, slashed form (e.g. java/lang/String)
	 * @param classLoader the classloader loading the type
	 * @param bytes the classfile contents for the type
	 * @return the new (modified) bytes for the class
	 */
	byte[] modify(String slashedClassName, ClassLoader classLoader, byte[] bytes);
}
