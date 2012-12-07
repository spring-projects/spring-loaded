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
 * Implementation of filed lookup algorithm for Class.getDeclaredField.
 * 
 * @author Kris De Volder
 * @since 0.5.0
 */
public class GetDeclaredFieldLookup extends FieldLookup {

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
			if (f.getName().equals(name)) {
				return f;
			}
		}
		return null;
	}

}
