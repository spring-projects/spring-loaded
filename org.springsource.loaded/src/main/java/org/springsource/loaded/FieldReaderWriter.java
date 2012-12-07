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
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Able to read or write a particular field in a type. Knows nothing about the instance upon which the read/write may be getting
 * done.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class FieldReaderWriter {

	private static Logger log = Logger.getLogger(FieldReaderWriter.class.getName());

	/**
	 * The type descriptor for the type that defines the field we want to access
	 */
	protected TypeDescriptor typeDescriptor;

	protected FieldMember theField;

	public FieldReaderWriter(FieldMember theField, TypeDescriptor typeDescriptor) {
		this.theField = theField;
		this.typeDescriptor = typeDescriptor;
		assert theField.typename == typeDescriptor.typename;
	}

	protected FieldReaderWriter() {
	}

	/**
	 * Set the value of an instance field on the specified instance to the specified value. If a state manager is passed in things
	 * can be done in a more optimal way, otherwise the state manager has to be discovered from the instance.
	 * 
	 * @param instance the object instance upon which to set the field
	 * @param newValue the new value for that field
	 * @param the optional state manager for this instance, which will be looked up (expensive) if not passed in
	 */
	public void setValue(Object instance, Object newValue, ISMgr stateManager) throws IllegalAccessException {
		if (typeDescriptor.isReloadable()) {
			if (stateManager == null) {
				// Look it up using reflection
				stateManager = findInstanceStateManager(instance);
			}
			String declaringTypeName = typeDescriptor.getName();
			Map<String, Object> typeLevelValues = stateManager.getMap().get(declaringTypeName);
			if (typeLevelValues == null) {
				// first time we've accessed this type for an instance field
				typeLevelValues = new HashMap<String, Object>();
				stateManager.getMap().put(declaringTypeName, typeLevelValues);
			}
			typeLevelValues.put(theField.getName(), newValue);
		} else { // the type is not reloadable, must use reflection to access the value
			// TODO generate get/set in the topmost reloader for these kinds of field and use them?
			if (typeDescriptor.isInterface()) {
				// field resolution has left us with an interface field, those can't be set like this
				throw new IncompatibleClassChangeError("Expected non-static field " + instance.getClass().getName() + "."
						+ theField.getName());
			} else {
				findAndSetFieldValueInHierarchy(instance, newValue);
			}
		}
	}

	public void setStaticFieldValue(Class<?> clazz, Object newValue, SSMgr stateManager) throws IllegalAccessException {
		if (clazz == null) {
			throw new IllegalStateException();
		}
		// First decision - is the field part of a reloadable type or not?  The typeDescriptor here is the actual owner
		// of the field, at this point we *know* this class has this field.
		if (typeDescriptor.isReloadable()) {
			if (stateManager == null) {
				// need to go and find it, there *will* be one but it will be slow to retrieve (reflection)
				stateManager = findStaticStateManager(clazz);
			}
			String declaringTypeName = typeDescriptor.getName();
			Map<String, Object> typeLevelValues = stateManager.getMap().get(declaringTypeName);
			if (typeLevelValues == null) {
				typeLevelValues = new HashMap<String, Object>();
				stateManager.getMap().put(declaringTypeName, typeLevelValues);
			}
			typeLevelValues.put(theField.getName(), newValue);
		} else { // the type is not reloadable, must use reflection to access the value
			try {
				Field f = locateFieldByReflection(clazz, typeDescriptor.getDottedName(), typeDescriptor.isInterface(),
						theField.getName());
				f.setAccessible(true);
				f.set(null, newValue);
				// cant cache result - we dont control the sets so won't know it is happening anyway
			} catch (Exception e) {
				throw new IllegalStateException("Unexpectedly unable to reflectively set the field " + theField.getName()
						+ " on the type " + clazz.getName());
			}
		}
	}

	/**
	 * Return the value of the field for which is reader-writer exists. To improve performance a fieldAccessor can be supplied but
	 * if it is missing the code will go and discover it.
	 * 
	 * @param instance the instance for which the field should be fetched
	 * @param stateManager an optional state manager containing the map of values (will be discovered if not supplied)
	 */
	public Object getValue(Object instance, ISMgr stateManager) throws IllegalAccessException, IllegalArgumentException {
		Object result = null;
		String fieldname = theField.getName();
		if (typeDescriptor.isReloadable()) {
			if (stateManager == null) {
				// find it using reflection
				stateManager = findInstanceStateManager(instance);
			}
			String declaringTypeName = typeDescriptor.getName();
			Map<String, Object> typeLevelValues = stateManager.getMap().get(declaringTypeName);
			boolean knownField = false;
			if (typeLevelValues != null) {
				knownField = typeLevelValues.containsKey(fieldname);
			}
			if (knownField) {
				result = typeLevelValues.get(fieldname);
			}

			// If a field has been deleted it may 'reveal' a field in a supertype.  The revealed field may be in a type
			// not yet dealt with.  In this case typeLevelValues may be null (type not seen before) or the typelevelValues
			// may not have heard of our field name.  In these cases we need to go and find the field and 'relocate' it
			// into our map, where it will be processed from now on.
			if (typeLevelValues == null || !knownField) {

				FieldMember fieldOnOriginalType = typeDescriptor.getReloadableType().getTypeRegistry()
						.getReloadableType(declaringTypeName).getTypeDescriptor().getField(fieldname);

				if (fieldOnOriginalType != null) {
					// Copy the field into the map - that is where it will live from now on
					ReloadableType rt = typeDescriptor.getReloadableType();
					try {
						Field f = rt.getClazz().getDeclaredField(fieldname);
						f.setAccessible(true);
						result = f.get(instance);
						if (typeLevelValues == null) {
							typeLevelValues = new HashMap<String, Object>();
							stateManager.getMap().put(declaringTypeName, typeLevelValues);
						}
						typeLevelValues.put(fieldname, result);
					} catch (Exception e) {
						throw new IllegalStateException("Unexpectedly unable to access field " + fieldname + " on class "
								+ rt.getClazz(), e);
					}
				} else {
					// The field was not on the original type.  As not seen before, can default it
					result = Utils.toResultCheckIfNull(null, theField.getDescriptor());
					if (typeLevelValues == null) {
						typeLevelValues = new HashMap<String, Object>();
						stateManager.getMap().put(declaringTypeName, typeLevelValues);
					}
					typeLevelValues.put(fieldname, result);
					return result;
				}
			}
			if (result != null) {
				result = Utils.checkCompatibility(typeDescriptor.getTypeRegistry(), result, theField.getDescriptor());
				if (result == null) {
					// Was not compatible, forget it
					typeLevelValues.remove(fieldname);
				}
			}
			result = Utils.toResultCheckIfNull(result, theField.getDescriptor());
		} else {
			// the type is not reloadable, must use reflection to access the value.
			// TODO measure how often we hit the reflection path, should never happen unless reflection is already on the frame

			if (typeDescriptor.isInterface()) { // cant be an instance field if it is found to be on an interface
				throw new IncompatibleClassChangeError("Expected non-static field " + instance.getClass().getName() + "."
						+ fieldname);
			} else {
				result = findAndGetFieldValueInHierarchy(instance);
			}
		}

		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
			log.finer("<getValue() value of " + theField + " is " + result);
		}
		return result;
	}

	public Object getStaticFieldValue(Class<?> clazz, SSMgr stateManager) throws IllegalAccessException, IllegalArgumentException {
		Object result = null;
		if (clazz == null) {
			throw new IllegalStateException();
		}
		// First decision - is the field part of a reloadable type or not?  The typeDescriptor here is the actual owner
		// of the field, at this point we *know* this class has this field.
		if (typeDescriptor.isReloadable()) {
			if (stateManager == null) {
				// need to go and find it, there *will* be one but it will be slow to retrieve (reflection)
				stateManager = findStaticStateManager(clazz);
				if (stateManager == null) {
					return Utils.toResultCheckIfNull(null, theField.descriptor);
				}
			}
			String declaringTypeName = typeDescriptor.getName();
			Map<String, Object> typeLevelValues = stateManager.getMap().get(declaringTypeName);
			String fieldname = theField.getName();
			boolean knownField = false;
			if (typeLevelValues != null) {
				knownField = typeLevelValues.containsKey(fieldname);
			}
			if (knownField) {
				result = typeLevelValues.get(fieldname);
			}
			// If a field has been deleted it may 'reveal' a field in a supertype.  The revealed field may be in a type
			// not yet dealt with.  In this case typeLevelValues may be null (type not seen before) or the typelevelValues
			// may not have heard of our field name.  In these cases we need to go and find the field and 'relocate' it
			// into our map, where it will be processed from now on.

			// These revealed fields are not necessarily in the original form of the type so cannot always be accessed via reflection
			if (typeLevelValues == null || !knownField) {

				FieldMember fieldOnOriginalType = typeDescriptor.getReloadableType().getTypeRegistry()
						.getReloadableType(declaringTypeName).getTypeDescriptor().getField(fieldname);

				if (fieldOnOriginalType != null) { // && fieldOnOriginalType.isStatic()
					// can use reflection
					ReloadableType rt = typeDescriptor.getReloadableType();
					try {
						Field f = rt.getClazz().getDeclaredField(theField.getName());
						if (!Modifier.isStatic(f.getModifiers())) {
							// need to default it anyway, cant see that original value
							// TODO  this is a dup of the code below, refactor
							result = Utils.toResultCheckIfNull(null, theField.getDescriptor());
							if (typeLevelValues == null) {
								typeLevelValues = new HashMap<String, Object>();
								stateManager.getMap().put(declaringTypeName, typeLevelValues);
							}
							typeLevelValues.put(fieldname, result);
							return result;
						}
						f.setAccessible(true);
						// TODO can fail on this next line if the field we've found is non-static
						result = f.get(null);
						if (typeLevelValues == null) {
							typeLevelValues = new HashMap<String, Object>();
							stateManager.getMap().put(declaringTypeName, typeLevelValues);
						}
						typeLevelValues.put(theField.getName(), result);
					} catch (Exception e) {
						throw new IllegalStateException("Unexpectedly unable to read field " + theField.getName() + " on type "
								+ rt.getClazz(), e);
					}
				} else {
					// The field was not on the original type.  As not seen before, can default it
					result = Utils.toResultCheckIfNull(null, theField.getDescriptor());
					if (typeLevelValues == null) {
						typeLevelValues = new HashMap<String, Object>();
						stateManager.getMap().put(declaringTypeName, typeLevelValues);
					}
					typeLevelValues.put(fieldname, result);
					return result;
				}
			}
			// A problem that can occur is if a fields type is changed on a reload.  If the field
			// was previously written to we can then retrieve it and attempt to pass it back to the caller
			// and they'll get something unexpected.  
			if (result != null) {
				result = Utils.checkCompatibility(typeDescriptor.getTypeRegistry(), result, theField.getDescriptor());
				if (result == null) {
					typeLevelValues.remove(theField.getName());
				}
			}
			result = Utils.toResultCheckIfNull(result, theField.getDescriptor());
		} else { // the type is not reloadable, must use reflection to access the value
			// TODO measure how often this code gets hit - ensure it does not in the common (non reflective) case
			try {
				Field f = locateFieldByReflection(clazz, typeDescriptor.getDottedName(), typeDescriptor.isInterface(),
						theField.getName());
				f.setAccessible(true);
				result = f.get(null);
				// cant cache result - we dont control the sets so won't know it is happening anyway
			} catch (Exception e) {
				throw new IllegalStateException("Unexpectedly unable to set static field " + theField.getName() + " on type "
						+ typeDescriptor.getDottedName());
			}

		}

		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.FINER)) {
			log.finer("<getValue() value of " + theField + " is " + result);
		}
		return result;
	}

	/**
	 * Discover the named field in the hierarchy using the standard rules of resolution.
	 * 
	 * @param clazz the class upon which to start looking
	 * @param typeWanted where the field is!
	 * @param name the name of the field to find
	 * @return the jlrField object representing that field, or null if not found
	 */
	private Field locateFieldByReflection(Class<?> clazz, String typeWanted, boolean isInterface, String name) {
		if (clazz.getName().equals(typeWanted)) {
			Field[] fs = clazz.getDeclaredFields();
			if (fs != null) {
				for (Field f : fs) {
					if (f.getName().equals(name)) {
						return f;
					}
				}
			}
		}
		// Check interfaces
		if (!isInterface) { // not worth looking!
			Class<?>[] interfaces = clazz.getInterfaces();
			if (interfaces != null) {
				for (Class<?> intface : interfaces) {
					Field f = locateFieldByReflection(intface, typeWanted, isInterface, name);
					if (f != null) {
						return f;
					}
				}
			}
		}
		// Check superclass
		Class<?> superclass = clazz.getSuperclass();
		if (superclass == null) {
			return null;
		} else {
			return locateFieldByReflection(superclass, typeWanted, isInterface, name);
		}
	}

	/**
	 * Discover the instance state manager for the specific object instance. Will fail by exception rather than returning null.
	 * 
	 * @param instance the object instance on which to look
	 * @return the discovered state manager
	 */
	private ISMgr findInstanceStateManager(Object instance) {
		Class<?> clazz = typeDescriptor.getReloadableType().getClazz();
		try {
			Field fieldAccessorField = clazz.getField(Constants.fInstanceFieldsName);
			if (fieldAccessorField == null) {
				throw new IllegalStateException("Cant find field accessor for type " + clazz.getName());
			}
			ISMgr stateManager = (ISMgr) fieldAccessorField.get(instance);
			if (stateManager == null) {
				throw new IllegalStateException("The class '" + clazz.getName()
						+ "' has a null instance state manager object, instance is " + instance);
			}
			return stateManager;
		} catch (Exception e) {
			throw new IllegalStateException("Unexpectedly unable to find instance state manager on class " + clazz.getName(), e);
		}
	}

	/**
	 * Discover the static state manager on the specified class and return it. Will fail by exception rather than returning null.
	 * 
	 * @param clazz the class on which to look
	 * @return the discovered state manager
	 */
	private SSMgr findStaticStateManager(Class<?> clazz) {
		try {
			Field stateManagerField = clazz.getField(Constants.fStaticFieldsName);
			if (stateManagerField == null) {
				throw new IllegalStateException("Cant find field accessor for type " + typeDescriptor.getReloadableType().getName());
			}
			SSMgr stateManager = (SSMgr) stateManagerField.get(null);
			// Field should always have been initialized - it is done at the start of the top most reloadable type <clinit>
			if (stateManager == null) {
				throw new IllegalStateException("Instance of this class has no state manager: " + clazz.getName());
			}
			return stateManager;
		} catch (Exception e) {
			throw new IllegalStateException("Unexpectedly unable to find static state manager on class " + clazz.getName(), e);
		}
	}

	public boolean isStatic() {
		return theField.isStatic();
	}

	/**
	 * Walk up the instance hierarchy looking for the field, and when it is found access it and return the result. Will exit via
	 * exception if it cannot find the field or something goes wrong when accessing it.
	 * 
	 * @param instance the object instance upon which the field is being accessed
	 * @return the value of the field
	 */
	private Object findAndGetFieldValueInHierarchy(Object instance) {
		Class<?> clazz = instance.getClass();
		String fieldname = theField.getName();
		String searchName = typeDescriptor.getName().replace('/', '.');
		while (clazz != null && !clazz.getName().equals(searchName)) {
			clazz = clazz.getSuperclass();
		}
		if (clazz == null) {
			throw new IllegalStateException("Failed to find " + searchName + " in hierarchy of " + instance.getClass());
		}
		try {
			Field f = clazz.getDeclaredField(fieldname);
			f.setAccessible(true);
			return f.get(instance);
		} catch (Exception e) {
			throw new IllegalStateException("Unexpectedly could not access field named " + fieldname + " on class "
					+ clazz.getName());
		}
	}

	/**
	 * Walk up the instance hierarchy looking for the field, and when it is found set it. Will exit via exception if it cannot find
	 * the field or something goes wrong when accessing it.
	 * 
	 * @param instance the object instance upon which the field is being set
	 * @param newValue the new value for the field
	 */
	private void findAndSetFieldValueInHierarchy(Object instance, Object newValue) {
		Class<?> clazz = instance.getClass();
		String fieldname = theField.getName();
		String searchName = typeDescriptor.getName().replace('/', '.');
		while (clazz != null && !clazz.getName().equals(searchName)) {
			clazz = clazz.getSuperclass();
		}
		if (clazz == null) {
			throw new IllegalStateException("Failed to find " + searchName + " in hierarchy of " + instance.getClass());
		}
		try {
			Field f = clazz.getDeclaredField(fieldname);
			f.setAccessible(true);
			f.set(instance, newValue);
		} catch (Exception e) {
			throw new IllegalStateException("Unexpectedly could not access field named " + fieldname + " on class "
					+ clazz.getName());
		}

	}
}