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

import org.objectweb.asm.ClassReader;

public class PluginUtils {

	/**
	 * If adding instance tracking, the classToCall must implement:
	 * <tt>public static void recordInstance(Object obj)</tt>.
	 * 
	 * @param bytes the bytes for the class to which instance tracking is being added
	 * @param classToCall the class to call when a new instance is created
	 * @return the modified bytes for the class
	 */
	public static byte[] addInstanceTracking(byte[] bytes, String classToCall) {
		ClassReader cr = new ClassReader(bytes);
		ClassVisitingConstructorAppender ca = new ClassVisitingConstructorAppender(classToCall, "recordInstance");
		cr.accept(ca, 0);
		byte[] newbytes = ca.getBytes();
		return newbytes;
	}
}
