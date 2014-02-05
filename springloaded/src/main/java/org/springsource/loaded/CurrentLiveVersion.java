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
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

/**
 * Captures the information about the reloaded parts of a type that vary each time a new version is loaded.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class CurrentLiveVersion {

	private static Logger log = Logger.getLogger(CurrentLiveVersion.class.getName());

	// Which reloadable type this represents the live version of
	final ReloadableType reloadableType;

	// Type descriptor for this live version
	final TypeDescriptor typeDescriptor;

	// 'stamp' (i.e. suffix) for this version
	final String versionstamp;

	public final IncrementalTypeDescriptor incrementalTypeDescriptor;

	String dispatcherName;
	byte[] dispatcher;
	Class<?> dispatcherClass;
	Object dispatcherInstance;

	String executorName;
	byte[] executor;
	Class<?> executorClass;

	TypeDelta typeDelta;

	private Method staticInitializer;
	private boolean haveLookedForStaticInitializer;

	public boolean staticInitializedNeedsRerunningOnDefine = false;

	public CurrentLiveVersion(ReloadableType reloadableType, String versionstamp, byte[] newbytedata) {
		if (GlobalConfiguration.logging && log.isLoggable(Level.FINER)) {
			log.entering("CurrentLiveVersion", "<init>", " new version of " + reloadableType.getName() + " loaded, version stamp '"
					+ versionstamp + "'");
		}
		this.reloadableType = reloadableType;
		this.typeDescriptor = reloadableType.getTypeRegistry().getExtractor().extract(newbytedata, true);
		this.versionstamp = versionstamp;

		if (GlobalConfiguration.assertsMode) {
			if (!this.typeDescriptor.getName().equals(reloadableType.typedescriptor.getName())) {
				throw new IllegalStateException("New version has wrong name.  Expected " + reloadableType.typedescriptor.getName()
						+ " but was " + typeDescriptor.getName());
			}
		}

		newbytedata = GlobalConfiguration.callsideRewritingOn ? MethodInvokerRewriter.rewrite(reloadableType.typeRegistry,
				newbytedata) : newbytedata;

		this.incrementalTypeDescriptor = new IncrementalTypeDescriptor(reloadableType.typedescriptor);
		this.incrementalTypeDescriptor.setLatestTypeDescriptor(this.typeDescriptor);

		// Executors for interfaces simply hold annotations
		this.executor = reloadableType.getTypeRegistry().executorBuilder.createFor(reloadableType, versionstamp, typeDescriptor,
				newbytedata);

		if (GlobalConfiguration.classesToDump != null
				&& GlobalConfiguration.classesToDump.contains(reloadableType.getSlashedName())) {
			Utils.dump(Utils.getExecutorName(reloadableType.getName(), versionstamp).replace('.', '/'), this.executor);
		}
		if (!typeDescriptor.isInterface()) {
			this.dispatcherName = Utils.getDispatcherName(reloadableType.getName(), versionstamp);
			this.executorName = Utils.getExecutorName(reloadableType.getName(), versionstamp);
			this.dispatcher = DispatcherBuilder.createFor(reloadableType, incrementalTypeDescriptor, versionstamp);
		}
		reloadableType.typeRegistry.checkChildClassLoader(reloadableType);
		define();
	}

	/**
	 * Defines this version. Called up front but can also be called later if the ChildClassLoader in a type registry is discarded
	 * and recreated.
	 */
	public void define() {
		staticInitializer = null;
		haveLookedForStaticInitializer = false;
		if (!typeDescriptor.isInterface()) {
			try {
				dispatcherClass = reloadableType.typeRegistry.defineClass(dispatcherName, dispatcher, false);
			} catch (RuntimeException t) {
				// TODO check for something strange.  something to do with the file detection misbehaving, see the same file attempted to be reloaded twice...
				if (t.getMessage().indexOf("duplicate class definition") == -1) {
					throw t;
				} else {
					t.printStackTrace();
				}
			}
		}
		try {
			executorClass = reloadableType.typeRegistry.defineClass(executorName, executor, false);
		} catch (RuntimeException t) {
			// TODO check for something strange.  something to do with the file detection misbehaving, see the same file attempted to be reloaded twice...
			if (t.getMessage().indexOf("duplicate class definition") == -1) {
				throw t;
			} else {
				t.printStackTrace();
			}
		}
		if (!typeDescriptor.isInterface()) {
			try {
				dispatcherInstance = dispatcherClass.newInstance();
			} catch (InstantiationException e) {
				throw new RuntimeException("Unable to build dispatcher class instance", e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Unable to build dispatcher class instance", e);
			}
		}
	}

	public MethodMember getReloadableMethod(String name, String descriptor) {
		// Look through the methods on the latest loaded version and find the method we want
		MethodMember[] methods = incrementalTypeDescriptor.getLatestTypeDescriptor().getMethods();
		for (MethodMember rmethod : methods) {
			if (rmethod.getName().equals(name)) {
				if (descriptor.equals(rmethod.getDescriptor())) {
					return rmethod;
				}
			}
		}
		return null;
	}

	// TODO should be caching the result in the MethodMember objects for speed
	public Method getExecutorMethod(MethodMember methodMember) {
		String executorDescriptor;
		String name;

		//What to search for:
		if (methodMember.isConstructor()) {
			name = Constants.mInitializerName;
		} else {
			name = methodMember.getName();
		}
		executorDescriptor = getExecutorDescriptor(methodMember);

		//Search for it:
		if (executorClass != null) {
			Method[] executorMethods = executorClass.getDeclaredMethods();
			for (Method executor : executorMethods) {
				if (executor.getName().equals(name) && Type.getMethodDescriptor(executor).equals(executorDescriptor)) {
					return executor;
				}
			}
		}
		return null;
	}

	private String getExecutorDescriptor(MethodMember methodMember) {
		Type[] params = Type.getArgumentTypes(methodMember.getDescriptor());
		Type[] newParametersArray = params;
		if (!methodMember.isStatic()) {
			newParametersArray = new Type[params.length + 1];
			System.arraycopy(params, 0, newParametersArray, 1, params.length);
			newParametersArray[0] = Type.getType(reloadableType.getClazz());
		}
		String executorDescriptor = Type.getMethodDescriptor(Type.getReturnType(methodMember.getDescriptor()), newParametersArray);
		return executorDescriptor;
	}

	@Override
	public String toString() {
		return "CurrentLiveVersion [reloadableType=" + reloadableType + ", typeDescriptor=" + typeDescriptor + ", versionstamp="
				+ versionstamp + ", dispatcherName=" + dispatcherName + ", executorName=" + executorName + "]";
	}

	public Class<?> getExecutorClass() {
		return executorClass;
	}

	public String getVersionStamp() {
		return versionstamp;
	}

	public Field getExecutorField(String name) throws SecurityException, NoSuchFieldException {
		return executorClass.getDeclaredField(name);
	}

	public TypeDelta getTypeDelta() {
		return typeDelta;
	}

	public void setTypeDelta(TypeDelta td) {
		typeDelta = td;
	}

	public boolean hasClinit() {
		return typeDescriptor.hasClinit();
	}

	public boolean hasConstructorChanged(String descriptor) {
		MethodMember mm = typeDescriptor.getConstructor(descriptor);
		return hasConstructorChanged(mm);
	}

	public boolean hasConstructorChanged(MethodMember mm) {
		if (mm == null) {
			return true;
		}
		// need to look at the delta
		if (typeDelta.haveMethodsChangedOrBeenAddedOrRemoved()) {
			if (typeDelta.haveMethodsChanged()) {
				MethodDelta md = typeDelta.changedMethods.get(mm.name + mm.descriptor);
				if (md != null) {
					return true;
				}
			}
			if (typeDelta.haveMethodsBeenAdded()) {
				MethodNode mn = typeDelta.brandNewMethods.get(mm.name + mm.descriptor);
				if (mn != null) {
					return true;
				}
			}
			if (typeDelta.haveMethodsBeenDeleted()) {
				MethodNode mn = typeDelta.lostMethods.get(mm.name + mm.descriptor);
				if (mn != null) {
					return true;
				}
			}
		}
		return false;
	}

	// TODO can we speed this up?
	public boolean hasConstructorChanged(int ctorId) {
		// need to find the constructor that id is for
		MethodMember mm = typeDescriptor.getConstructor(ctorId);
		return hasConstructorChanged(mm);
	}

	public void clearClassloaderLinks() {
		this.executorClass = null;
		this.dispatcherClass = null;
	}

	public void reloadMostRecentDispatcherAndExecutor() {
		define();
	}

	public Object getDispatcherInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	public void runStaticInitializer() {
		if (!haveLookedForStaticInitializer) {
			try {
				staticInitializer = this.getExecutorClass().getDeclaredMethod(Constants.mStaticInitializerName);
			} catch (NoSuchMethodException e) {
				// some types don't have a static initializer, that is OK
			}
			haveLookedForStaticInitializer = true;
		}
		if (staticInitializer != null) {
			try {
				staticInitializer.invoke(null);
			} catch (Exception e) {
				log.severe("Unexpected exception whilst trying to call the static initializer on " + this.reloadableType.getName());
				e.printStackTrace(); // TODO remove when happy
			}
		}
	}
}
