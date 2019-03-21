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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * A FieldReaderWriter implementation that simply uses reflection to set/get the fields.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class ReflectionFieldReaderWriter extends FieldReaderWriter {

	private Field field;

	public ReflectionFieldReaderWriter(Field findField) {
		super();
		this.field = findField;
	}

	@Override
	public Object getStaticFieldValue(Class<?> type, SSMgr fieldAccessor) throws IllegalAccessException,
			IllegalArgumentException {
		field.setAccessible(true);
		return field.get(null);
	}

	@Override
	public void setStaticFieldValue(Class<?> clazz, Object newValue, SSMgr fieldAccessor) throws IllegalAccessException {
		field.setAccessible(true);
		field.set(null, newValue);
	}

	@Override
	public void setValue(Object instance, Object newValue, ISMgr fieldAccessor) throws IllegalAccessException {
		field.setAccessible(true);
		field.set(instance, newValue);
	}

	@Override
	public Object getValue(Object instance, ISMgr fieldAccessor) throws IllegalAccessException,
			IllegalArgumentException {
		field.setAccessible(true);
		return field.get(instance);
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(field.getModifiers());
	}

}
