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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static State Manager. The top most class in every hierarchy of reloadable types gets a static state manager instance.
 * The static state manager is used to find the value of a field for a particular object instance. The FieldAccessor is
 * added to the top most type in a reloadable hierarchy and is accessible to all the subtypes. It maintains a map from
 * type names to fields (name/value pairs).
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class SSMgr {

	private static Logger log = Logger.getLogger(SSMgr.class.getName());

	Map<String, Map<String, Object>> values = new HashMap<String, Map<String, Object>>();

	public Object getValue(ReloadableType rtype, String name) throws IllegalAccessException {
		//		System.out.println("SSMgr.getValue(rtype=" + rtype + ",name=" + name + ")");
		Object result = null;
		// quick look to see if it is nearby (searches up supertype hierarchy, but only up to the topmost reloadabletype)
		FieldMember fieldmember = rtype.findStaticField(name);//InstanceField(name);

		// Why can fieldmember be null?
		// 1. Field really does not exist - shouldn't really be possible if the code is 'valid'
		// 2. Field is inherited from a supertype (usually because a reload has occurred)
		if (fieldmember == null) {
			FieldReaderWriter flr = rtype.locateField(name);
			if (flr == null) {
				log.info("Unexpectedly unable to locate static field " + name + " starting from type "
						+ rtype.dottedtypename
						+ ": clinit running late?");
				return null;
			}
			result = flr.getStaticFieldValue(rtype.getClazz(), this);
		}
		else {
			if (!fieldmember.isStatic()) {
				throw new IncompatibleClassChangeError("Expected static field " + rtype.dottedtypename + "."
						+ fieldmember.getName());
			}
			String declaringTypeName = fieldmember.getDeclaringTypeName();
			Map<String, Object> typeLevelValues = values.get(declaringTypeName);
			boolean knownField = false;
			if (typeLevelValues != null) {
				knownField = typeLevelValues.containsKey(name);
			}
			if (knownField) {
				result = typeLevelValues.get(name);
			}
			// If a field has been deleted it may 'reveal' a field in a supertype.  The revealed field may be in a type
			// not yet dealt with.  In this case typeLevelValues may be null (type not seen before) or the typelevelValues
			// may not have heard of our field name.  In these cases we need to go and find the field and 'relocate' it
			// into our map, where it will be processed from now on.

			// These revealed fields are not necessarily in the original form of the type so cannot always be accessed via reflection
			if (typeLevelValues == null || !knownField) {
				// TODO lookup performance?
				FieldMember fieldOnOriginalType = rtype.getTypeRegistry().getReloadableType(declaringTypeName).getTypeDescriptor()
						.getField(name);

				if (fieldOnOriginalType != null) {
					// Copy that field into the map... where it is going to live from now on
					ReloadableType rt = rtype.getTypeRegistry().getReloadableType(fieldmember.getDeclaringTypeName());
					try {
						Field f = rt.getClazz().getDeclaredField(name);
						f.setAccessible(true);
						result = f.get(null);
						if (typeLevelValues == null) {
							typeLevelValues = new HashMap<String, Object>();
							values.put(declaringTypeName, typeLevelValues);
						}
						typeLevelValues.put(name, result);
					}
					catch (Exception e) {
						throw new IllegalStateException("Unexpectedly unable to access field " + name + " on type "
								+ rt.getClazz().getName(), e);
					}
				}
				else {
					// The field was not on the original type.  As not seen before, can default it
					result = Utils.toResultCheckIfNull(null, fieldmember.getDescriptor());
					if (typeLevelValues == null) {
						typeLevelValues = new HashMap<String, Object>();
						values.put(declaringTypeName, typeLevelValues);
					}
					typeLevelValues.put(name, result);
					return result;
				}
			}

			if (result != null) {
				result = Utils.checkCompatibility(rtype.getTypeRegistry(), result, fieldmember.getDescriptor());
				if (result == null) {
					typeLevelValues.remove(fieldmember.getName());
				}
			}
			result = Utils.toResultCheckIfNull(result, fieldmember.getDescriptor());
		}
		//		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
		//			log.finer("<getValue() value of " + name + " is " + result);
		//		}
		return result;
	}

	// TODO ensure can't set field values on interfaces (constants)? (guess we should never encounter the code that tries it)
	public void setValue(ReloadableType rtype, Object newValue, String name) throws IllegalAccessException {
		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINEST)) {
			log.finest("Static field set: " + rtype.getName() + "." + name + " " + newValue);
		}
		// TODO can setValue for static fields ignore interfaces? since we know all those fields will be final - what about the clinit calls to setup the fields though?
		FieldMember fieldmember = rtype.findStaticField(name);//InstanceField(name);

		if (fieldmember == null) {
			// If the field is null, there are two possible reasons:
			// 1. The field does not exist in the hierarchy at all
			// 2. The field is on a type just above our topmost reloadable type
			FieldReaderWriter frw = rtype.locateField(name);
			if (frw == null) {
				// bad code redeployed?
				log.info("Unexpectedly unable to locate static field " + name + " starting from type "
						+ rtype.dottedtypename
						+ ": clinit running late?");
				return;
			}
			frw.setStaticFieldValue(rtype.getClazz(), newValue, this);
		}
		else {
			if (!fieldmember.isStatic()) {
				throw new IncompatibleClassChangeError("Expected static field " + rtype.dottedtypename + "."
						+ fieldmember.getName());
			}
			Map<String, Object> typeValues = values.get(fieldmember.getDeclaringTypeName());//rtype.getName());
			if (typeValues == null) {
				typeValues = new HashMap<String, Object>();
				values.put(fieldmember.getDeclaringTypeName(), typeValues);
			}
			typeValues.put(name, newValue);
		}
	}

	private String valuesToString() {
		StringBuilder s = new StringBuilder();
		s.append("FieldAccessor:" + System.identityHashCode(this)).append("\n");
		for (Map.Entry<String, Map<String, Object>> entry : values.entrySet()) {
			s.append("Type " + entry.getKey()).append("\n");
			for (Map.Entry<String, Object> entry2 : entry.getValue().entrySet()) {
				s.append(" " + entry2.getKey() + "=" + entry2.getValue()).append("\n");
			}
		}
		return s.toString();
	}

	public String toString() {
		return valuesToString();
	}

	Map<String, Map<String, Object>> getMap() {
		return values;
	}
}
