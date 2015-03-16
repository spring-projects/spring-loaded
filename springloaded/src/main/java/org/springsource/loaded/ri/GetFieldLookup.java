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

package org.springsource.loaded.ri;

import java.lang.reflect.Field;
import java.util.List;

import org.springsource.loaded.ReloadableType;


/**
 * Implementation of FieldLookup algorithm for "Class.getField".
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class GetFieldLookup extends FieldLookup {

	public static Field lookup(ReloadableType rtype, String name) {
		FieldRef ref = lookup(FieldProvider.create(rtype), name);
		if (ref == null) {
			return null;
		}
		return ref.getField();
	}

	private static FieldRef lookup(FieldProvider provider, String name) {
		List<FieldRef> fields = provider.getFields();
		for (FieldRef f : fields) {
			if (f.isPublic()) {
				if (f.getName().equals(name)) {
					return f;
				}
			}
		}
		// Didn't find in this type. Check interfaces.
		FieldProvider[] itfs = provider.getInterfaces();
		for (FieldProvider itf : itfs) {
			FieldRef f = lookup(itf, name);
			if (f != null) {
				return f;
			}
		}
		// Still didn't find... Check superclass but only if we are not an interface
		if (!provider.isInterface()) {
			FieldProvider supr = provider.getSuper();
			if (supr != null) {
				FieldRef f = lookup(supr, name);
				if (f != null) {
					return f;
				}
			}
		}
		//Not found
		return null;
	}

}
