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
 * Every reloadable hierarchy gets an Instance State Manager (ISMgr). The instance state manager is used to find the value of a
 * field for a particular object instance. The manager is added to the top most type in a reloadable hierarchy and is accessible to
 * all the subtypes. It maintains a map from types to secondary maps. The secondary maps record name,value pairs for each field. The
 * maps are only used if something has happened to mean we cannot continue to store the values in the original fields.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class ISMgr {

	private static Logger log = Logger.getLogger(ISMgr.class.getName());

	Map<String, Map<String, Object>> values = new HashMap<String, Map<String, Object>>();

	// TODO rtype in here means no need to have it on the getValue calls
	public ISMgr(Object instance, ReloadableType rtype) {
		//		System.out.println("Instance passed to ISMgr " + instance + " rtype=" + rtype);
		if (rtype.getTypeDescriptor().isGroovyType()) {
			rtype.trackLiveInstance(instance);
		}
	}

	/**
	 * Get the value of a instance field - this will use 'any means necessary' to get to it.
	 * 
	 * @param rtype the reloadabletype
	 * @param instance the object instance on which the field is being accessed (whose type may not be that which declares the
	 *        field)
	 * @param name the name of the field
	 * 
	 * @return the value of the field
	 * @throws IllegalAccessException if there is a problem accessing the field
	 */
	public Object getValue(ReloadableType rtype, Object instance, String name) throws IllegalAccessException {
		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
			log.finer(">getValue(rtype=" + rtype + ",instance=" + instance + ",name=" + name + ")");
		}
		Object result = null;

		// Quick look from here to top most reloadable type:
		FieldMember field = rtype.findInstanceField(name);

		if (field == null) {
			// If the field is now null, there are two possible reasons:
			// 1. The field does not exist in the hierarchy at all
			// 2. The field is on a type just above our topmost reloadable type
			FieldReaderWriter frw = rtype.locateField(name);
			if (frw == null) {
				// Used to be caused because we were not reloading constructors - so when a new version of the type was
				// loaded, maybe a field was removed, but the constructor may still be referring to it.  Should no longer
				// happen but what about static initializers that aren't run straightaway?
				log.info("Unexpectedly unable to locate instance field " + name + " starting from type " + rtype.dottedtypename
						+ ": clinit running late?");
				return null;
			}
			result = frw.getValue(instance, this);
		} else {
			if (field.isStatic()) {
				throw new IncompatibleClassChangeError("Expected non-static field " + rtype.dottedtypename + "." + field.getName());
			}
			String declaringTypeName = field.getDeclaringTypeName();
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

			// These revealed fields are not necessarily in the original form of the type so cannot be accessed via reflection
			if (typeLevelValues == null || !knownField) {
				// Determine whether we need to use reflection or not:
				// 'field' tells us if we know about it now, it doesn't tell us if we've always known about it

				// TODO lookup performance
				FieldMember fieldOnOriginalType = rtype.getTypeRegistry().getReloadableType(field.getDeclaringTypeName())
						.getTypeDescriptor().getField(name);

				if (fieldOnOriginalType != null) {
					// The field was on the original type, use reflection
					ReloadableType rt = rtype.getTypeRegistry().getReloadableType(field.getDeclaringTypeName());
					try {
						Field f = rt.getClazz().getDeclaredField(name);
						f.setAccessible(true);
						result = f.get(instance);
						if (typeLevelValues == null) {
							typeLevelValues = new HashMap<String, Object>();
							values.put(declaringTypeName, typeLevelValues);
						}
						typeLevelValues.put(name, result);
					} catch (Exception e) {
						throw new IllegalStateException("Unexpectedly unable to access field " + name + " on type "
								+ rt.getClazz().getName(), e);
					}
				} else {
					// The field was not on the original type.  As not seen before, can default it
					result = Utils.toResultCheckIfNull(null, field.getDescriptor());
					if (typeLevelValues == null) {
						typeLevelValues = new HashMap<String, Object>();
						values.put(declaringTypeName, typeLevelValues);
					}
					typeLevelValues.put(name, result);
					return result;
				}
			}

			if (result != null) {
				result = Utils.checkCompatibility(rtype.getTypeRegistry(), result, field.getDescriptor());
				if (result == null) {
					typeLevelValues.remove(field.getName());
				}
			}
			result = Utils.toResultCheckIfNull(result, field.getDescriptor());
		}
		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
			log.finer("<getValue() value of " + name + " is " + result);
		}
		return result;
	}

	/**
	 * Set the value of a field.
	 * 
	 * @param rtype the reloadabletype
	 * @param instance the instance upon which to set the field
	 * @param value the value to put into the field
	 * @param name the name of the field
	 * @throws IllegalAccessException if there is a problem setting the field value
	 */
	public void setValue(ReloadableType rtype, Object instance, Object value, String name) throws IllegalAccessException {
		//		System.err.println(">setValue(rtype=" + rtype + ",instance=" + instance + ",value=" + value + ",name=" + name + ")");

		// Look up through our reloadable hierarchy to find it
		FieldMember fieldmember = rtype.findInstanceField(name);

		if (fieldmember == null) {
			// If the field is null, there are two possible reasons:
			// 1. The field does not exist in the hierarchy at all
			// 2. The field is on a type just above our topmost reloadable type
			FieldReaderWriter frw = rtype.locateField(name);
			if (frw == null) {
				// bad code redeployed?
				log.info("Unexpectedly unable to locate instance field " + name + " starting from type " + rtype.dottedtypename
						+ ": clinit running late?");
				return;
			}
			frw.setValue(instance, value, this);
		} else {
			if (fieldmember.isStatic()) {
				throw new IncompatibleClassChangeError("Expected non-static field " + rtype.dottedtypename + "."
						+ fieldmember.getName());
			}
			Map<String, Object> typeValues = values.get(fieldmember.getDeclaringTypeName());
			if (typeValues == null) {
				typeValues = new HashMap<String, Object>();
				values.put(fieldmember.getDeclaringTypeName(), typeValues);
			}
			typeValues.put(name, value);
		}
	}

	private String valuesToString() {
		StringBuilder s = new StringBuilder();
		s.append("InstanceState:" + System.identityHashCode(this)).append("\n");
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
