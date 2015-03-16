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

import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.LoadtimeInstrumentationPlugin;


/**
 * The CGLIB plugin recognizes when elements of cglib are loaded and rewrites them to catch certain events occuring.
 * 
 * @author Andy Clement
 * @since 0.8.3
 */
public class CglibPlugin implements LoadtimeInstrumentationPlugin {

	private static Logger log = Logger.getLogger(CglibPlugin.class.getName());

	// implementing LoadtimeInstrumentationPlugin
	public boolean accept(String slashedTypeName, ClassLoader classLoader, ProtectionDomain protectionDomain,
			byte[] bytes) {
		if (slashedTypeName == null) {
			return false;
		}
		// Sometimes the package prefix for cglib types is changed, for example:
		// net/sf/cglib/core/AbstractClassGenerator
		// org/springframework/cglib/core/AbstractClassGenerator
		// This test will allow for both variants
		return slashedTypeName.endsWith("/cglib/core/AbstractClassGenerator");
		// || slashedTypeName.equals("net/sf/cglib/reflect/FastClass");
	}

	public byte[] modify(String slashedClassName, ClassLoader classLoader, byte[] bytes) {
		if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
			log.info("Modifying " + slashedClassName);
		}
		// if (slashedClassName.equals("net/sf/cglib/core/AbstractClassGenerator")) {
		return CglibPluginCapturing.catchGenerate(bytes);

		// Not currently worrying about FastClass:
		// } else {
		// net/sf/cglib/reflect/FastClass
		// We must empty the FastClass constructor.  Why?  Due to current limitations with
		// SpringLoaded we consider a constructor to have changed when the type is reloaded
		// (so regardless of whether the code did actually change).  Due to this the
		// 'new' constructors driven once reloaded call super.<init>().  The default FastClass
		// empty constructor throws an exception.  We are just removing that throw.
		// return bytes;//EmptyCtor.invoke(bytes, "()V");
		// }
	}

}
