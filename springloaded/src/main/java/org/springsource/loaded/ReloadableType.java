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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.springsource.loaded.MethodInvokerRewriter.DontRewriteException;
import org.springsource.loaded.MethodInvokerRewriter.RewriteClassAdaptor;
import org.springsource.loaded.agent.CglibPluginCapturing;
import org.springsource.loaded.infra.UsedByGeneratedCode;
import org.springsource.loaded.ri.Invoker;
import org.springsource.loaded.ri.JavaMethodCache;

/**
 * Represents a type that has been processed such that it can be reloaded at runtime.
 *
 * @author Andy Clement
 * @since 0.5.0
 */
public class ReloadableType {

	// TODO when a field is shadowed or renamed and the old one never accessed again, it may be holding onto something and prevent it from GC.
	// Thinking about a solution that involves a tag in the FieldAccessor object so that we can
	// check whether a 'repair' is needed on a field accessor (because the type has been reloaded and
	// the map in the accessor hasnt been repaired yet)
	private static Logger log = Logger.getLogger(ReloadableType.class.getName());

	/** The registry maintaining this reloadable type */
	public TypeRegistry typeRegistry;

	/** The dotted typename */
	public final String dottedtypename;

	/** The slashed typename */
	public final String slashedtypename;

	/** The id number for this reloadable type, allocated by the registry */
	private int id;

	/** The bytes for the original implementation as first loaded, before rewriting */
	public byte[] bytesInitial;

	/** The bytes for the original implementation as first loaded, after rewriting */
	public byte[] bytesLoaded;

	/** The bytes for the interface representing the first loaded implementation */
	public final byte[] interfaceBytes;

	/** A type descriptor describing the shape of the type at first load */
	public TypeDescriptor typedescriptor;

	/** Holds the most recently loaded (and active) version. Null if original is still in use */
	private CurrentLiveVersion liveVersion;

	/** Map from member 'name' to a secondary map that is from 'descriptor' to real reloadable member */
	//	public final Map<String, Map<String, Member>> memberMap = new HashMap<String, Map<String, Member>>();

	/** Map from the member id (allocated during initial processing) to the relevant reloadable member */
	//	public final Map<Integer, AbstractMember> memberIntMap = new HashMap<Integer, Member>();

	/** The class object for the loaded rewritten (reloadable) form */
	private Class<?> clazz;

	/** The superclass */
	private Class<?> superclazz;

	private ReloadableType superRtype;

	private ReloadableType[] interfaceRtypes;

	List<Reference<ReloadableType>> associatedSubtypes = null;

	/**
	 * Caches Method objects for this reloadable type. This cache should be invalidated (set to null) when a type is
	 * reloaded!
	 */
	private JavaMethodCache javaMethodCache;

	private final static int IS_RESOLVED = 0x0001;

	// Indicates that this type or one in its hierarchy (super/sub) has been reloaded
	private final static int IMPACTED_BY_RELOAD = 0x0002;

	private int bits;

	/** Cache of the invokers used to answer getDeclaredMethods() call made on this type */
	public List<Invoker> invokersCache_getDeclaredMethods = null;

	// TODO clear these out on reload
	public Collection<Invoker> invokersCache_getMethods = null;

	public Map<String, Map<String, Invoker>> invokerCache_getMethod = new HashMap<String, Map<String, Invoker>>();

	public Map<String, Map<String, Invoker>> invokerCache_getDeclaredMethod = new HashMap<String, Map<String, Invoker>>();

	public Class<?> getClazz() {
		if (clazz == null) {
			// lazily looked up.  This path is used when running in an agent where we can't access the class until the calling
			// classloader has defined it.
			try {
				clazz = Class.forName(dottedtypename, false, typeRegistry.getClassLoader());
			}
			catch (ClassNotFoundException cnfe) {
				throw new ReloadException("Unexpectedly unable to find class " + this.dottedtypename
						+ ".  Asked classloader "
						+ typeRegistry.getClassLoader(), cnfe);
			}
		}
		return clazz;
	}

	@Override
	public String toString() {
		return dottedtypename;
	}

	/**
	 * Construct a new ReloadableType with the specified name and the specified initial bytecode.
	 *
	 * @param dottedtypename the dotted name
	 * @param initialBytes the bytecode for the initial version
	 * @param id for this reloadable type, allocated by the registry
	 * @param typeRegistry the registry managing this type
	 * @param typeDescriptor the type descriptor (if it has already been worked out), otherwise null
	 */
	public ReloadableType(String dottedtypename, byte[] initialBytes, int id, TypeRegistry typeRegistry,
			TypeDescriptor typeDescriptor) {
		if (GlobalConfiguration.assertsMode) {
			Utils.assertDotted(dottedtypename);
		}
		this.id = id;
		if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
			log.info("New reloadable type: " + dottedtypename + " (allocatedId=" + id + ") " + typeRegistry.toString());
		}
		this.typeRegistry = typeRegistry;
		this.dottedtypename = dottedtypename;
		this.slashedtypename = dottedtypename.replace('.', '/');
		this.typedescriptor = (typeDescriptor != null ? typeDescriptor
				: typeRegistry.getExtractor().extract(
						initialBytes, true));
		this.interfaceBytes = InterfaceExtractor.extract(initialBytes, typeRegistry, this.typedescriptor);
		this.bytesInitial = initialBytes;
		rewriteCallSitesAndDefine();
	}

	private ReloadableType() {
		slashedtypename = null;
		dottedtypename = null;
		interfaceBytes = null;
	}

	public final static ReloadableType NOT_RELOADABLE_TYPE = new ReloadableType();

	public final static WeakReference<ReloadableType> NOT_RELOADABLE_TYPE_REF = new WeakReference<ReloadableType>(
			NOT_RELOADABLE_TYPE);

	public TypeDescriptor getTypeDescriptor() {
		return typedescriptor;
	}

	/**
	 * Gets the 'orignal' method corresponding to given name and method descriptor. This only considers methods that
	 * exist in the first (non-reloaded) version of the type.
	 *
	 * @param name method name
	 * @param descriptor method descriptor (e.g (Ljava/lang/String;)I)
	 * @return the MethodMember or an exception if not found
	 */
	// TODO introduce a cache for people trolling through the methods array? same for fields?
	public MethodMember getMethod(String name, String descriptor) {
		for (MethodMember method : typedescriptor.getMethods()) {
			if (method.getName().equals(name) && method.getDescriptor().equals(descriptor)) {
				return method;
			}
		}
		throw new IllegalStateException("Unable to find member '" + name + descriptor + "' on type "
				+ this.dottedtypename);
	}

	public MethodMember getConstructor(String descriptor) {
		for (MethodMember ctor : typedescriptor.getConstructors()) {
			if (ctor.getDescriptor().equals(descriptor)) {
				return ctor;
			}
		}
		throw new IllegalStateException("Unable to find constructor '<init>" + descriptor + "' on type "
				+ this.dottedtypename);
	}

	// TODO what about 'regular' spring load time weaving?
	/**
	 * This method will attempt to apply any pre-existing transforms to the provided bytecode, if it is thought to be
	 * necessary. Currently 'necessary' is determined by finding ourselves running under tcServer and Spring Insight
	 * being turned on.
	 *
	 * @param bytes the new bytes to be possibly transformed.
	 * @return either the original bytes or a transformed set of bytes
	 */
	private byte[] retransform(byte[] bytes) {
		if (!determinedNeedToRetransform) {
			try {
				String s = System.getProperty("insight.enabled", "false");
				if (s.equals("true")) {
					// Access the weavingTransformer field, of type WeavingTransformer
					ClassLoader cl = typeRegistry.getClassLoader();
					Field f = cl.getClass().getSuperclass().getDeclaredField("weavingTransformer");
					if (f != null) {
						f.setAccessible(true);
						retransformWeavingTransformer = f.get(cl);
						// Stash the weavingtransformer instance and transformIfNecessaryMethod
						// byte[] transformIfNecessary(String className, byte[] bytes) {
						retransformWeavingTransformMethod = retransformWeavingTransformer.getClass().getDeclaredMethod(
								"transformIfNecessary", String.class, byte[].class);
						retransformNecessary = true;
					}
					if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
						log.info("Determining if retransform necessary, result = " + retransformNecessary);
					}
				}
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "Unexpected exception when determining if Spring Insight enabled", e);
				retransformNecessary = false;
			}
			determinedNeedToRetransform = true;
		}
		if (retransformNecessary) {
			try {
				retransformWeavingTransformMethod.setAccessible(true);
				byte[] newdata = (byte[]) retransformWeavingTransformMethod.invoke(retransformWeavingTransformer,
						this.slashedtypename, bytes);
				//				System.err.println("RETRANSFORMATION RUNNING.  oldsize=" + bytes.length + " newsize=" + newdata.length);
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
					log.info("retransform was attempted, oldsize=" + bytes.length + " newsize=" + newdata.length);
				}
				return newdata;
			}
			catch (Exception e) {
				if (GlobalConfiguration.isRuntimeLogging) {
					log.log(Level.SEVERE, "Unexpected exception when trying to run other weaving transformers", e);
				}
			}
		}
		return bytes;
	}

	private boolean determinedNeedToRetransform = false;

	private boolean retransformNecessary = false;

	private Object retransformWeavingTransformer;

	private java.lang.reflect.Method retransformWeavingTransformMethod;

	// for lazy tests that are only loading one new version, fill in the versionsuffix for them
	public boolean loadNewVersion(byte[] newbytedata) {
		return loadNewVersion("2", newbytedata);
	}

	public boolean simulateReload() {
		return loadNewVersion("0", bytesInitial);
	}

	public boolean loadNewVersion(String versionsuffix, byte[] newbytedata, boolean shouldRerunStaticInitializer) {
		boolean reloadedOK = loadNewVersion(versionsuffix, newbytedata);
		if (reloadedOK) {
			runStaticInitializer();
		}
		return reloadedOK;
	}

	/**
	 * Load a new version of this type, using the specified suffix to tag the newly generated artifact class names.
	 *
	 * @param versionsuffix the String suffix to append to classnames being created for the reloaded class
	 * @param newbytedata the class bytes for the new version of this class
	 * @return true if the reload succeeded
	 */
	public boolean loadNewVersion(String versionsuffix, byte[] newbytedata) {
		javaMethodCache = null;
		if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
			log.info("Loading new version of " + slashedtypename + ", identifying suffix " + versionsuffix
					+ ", new data length is " + newbytedata.length + "bytes");
		}

		// If we find our parent classloader has a weavingTransformer
		newbytedata = retransform(newbytedata);

		// TODO how slow is this? something to worry about? make it conditional on a setting?
		boolean reload = true;
		TypeDelta td = null;
		if (GlobalConfiguration.verifyReloads) {
			td = TypeDiffComputer.computeDifferences(bytesInitial, newbytedata);
			if (td.hasAnythingChanged()) {
				// need to check it isn't anything we do not yet support
				boolean cantReload = false;
				StringBuilder s = null;
				if (td.hasTypeDeclarationChanged()) {
					// Not allowed to change the type
					reload = false;

					s = new StringBuilder("Spring Loaded: Cannot reload new version of ").append(
							this.dottedtypename).append(
									"\n");
					if (td.hasTypeAccessChanged()) {
						s.append(" Reason: Type modifiers changed from=0x" + Integer.toHexString(td.oAccess) + " to=0x"
								+ Integer.toHexString(td.nAccess) + "\n");
						cantReload = true;
					}
					if (td.hasTypeSupertypeChanged()) {
						s.append(" Reason: Supertype changed from ").append(td.oSuperName).append(" to ").append(
								td.nSuperName).append("\n");
						cantReload = true;
					}
					if (td.hasTypeInterfacesChanged()) {
						// This next bit of code is to deal with the situation
						// Peter saw where on a full build some type implements
						// GroovyObject
						// but on an incremental build of just that one file, it
						// no longer implements it (presumably - and we could go
						// checking
						// for this - a supertype already implements the
						// interface but the full build wasn't smart enough to
						// work that out)
						boolean justGroovyObjectMoved = false;
						if (!cantReload && getTypeDescriptor().isGroovyType()) {
							// Is it just GroovyObject that has been lost?
							List<String> interfaceChanges = new ArrayList<String>();
							interfaceChanges.addAll(td.oInterfaces);
							interfaceChanges.removeAll(td.nInterfaces);
							// If ifaces is just GroovyObject now then that
							// means it has been removed from the interface list
							// - which can unfortunately happen on an
							// incremental compile
							if (this.getTypeDescriptor().isGroovyType() && interfaceChanges.size() == 1
									&& interfaceChanges.get(0).equals("groovy/lang/GroovyObject")) {
								// just let it go... needs fixing in Groovy
								// really
								justGroovyObjectMoved = true;
								s = null;
								reload = true;
							}
						}
						if (!justGroovyObjectMoved) {
							s.append(" Reason: Interfaces changed from ").append(td.oInterfaces).append(" to ").append(
									td.nInterfaces).append("\n");
							cantReload = true;
						}
					}
				}
				//				if (td.haveFieldsChangedOrBeenAddedOrRemoved()) {
				//					reload = false;
				//					if (s == null) {
				//						s = new StringBuilder("Spring-Loaded: Cannot reload new version of ").append(this.dottedtypename).append(
				//								"\n");
				//					}
				//					if (td.hasNewFields()) {
				//						s.append(" Reason: New version has new fields:\n" + Utils.fieldNodeFormat(td.getNewFields().values()));
				//					}
				//					if (td.hasLostFields()) {
				//						s.append(" Reason: New version has removed some fields: \n"
				//								+ Utils.fieldNodeFormat(td.getLostFields().values()));
				//					}
				//				}
				boolean somethingCalled = false;
				if (cantReload && td.hasAnythingChanged()) {

					somethingCalled = typeRegistry.fireUnableToReloadEvent(this, td, versionsuffix);
				}
				if (cantReload && s == null && td.hasAnythingChanged()) {
					if (!somethingCalled) {
						System.out.println("Something has changed preventing reload");
					}
				}
				if (!somethingCalled && s != null) {
					System.out.println(s);
				}
			}
		}
		if (reload) {

			TypeRegistry.nothingReloaded = false;
			invokersCache_getDeclaredMethods = null; // will no longer use this cache
			if (GlobalConfiguration.reloadMessages) {
				// Only put out the message when running in limit mode (under tc Server)
				System.out.println("Reloading: Loading new version of " + this.dottedtypename + " [" + versionsuffix
						+ "]");
			}
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				log.info("Reloading: Loading new version of " + this.dottedtypename + " [" + versionsuffix + "]");
			}
			liveVersion = new CurrentLiveVersion(this, versionsuffix, newbytedata);
			liveVersion.setTypeDelta(td);
			typeRegistry.reloadableTypeDescriptorCache.put(this.slashedtypename, liveVersion.typeDescriptor);
			if (typedescriptor.isGroovyType()) {
				fixupGroovyType();
			}
			if (typedescriptor.isEnum()) {
				resetEnumRelatedState();
			}
			if (typeRegistry.shouldRerunStaticInitializer(this, versionsuffix) || typedescriptor.isEnum()) {
				liveVersion.staticInitializedNeedsRerunningOnDefine = true;
				liveVersion.runStaticInitializer();
			}
			else {
				liveVersion.staticInitializedNeedsRerunningOnDefine = false;
			}
			// For performance:
			// - tag the relevant types that may have been affected by this being reloaded, i.e. this type and any reloadable types in the same hierachy
			tagAsAffectedByReload();
			tagSupertypesAsAffectedByReload();
			tagSubtypesAsAffectedByReload();

			typeRegistry.fireReloadEvent(this, versionsuffix);

			reloadProxiesIfNecessary(versionsuffix);
		}

		// dump(newbytedata);
		return reload;
	}

	private void tagSupertypesAsAffectedByReload() {
		ReloadableType superRtype = getSuperRtype();
		if (superRtype != null) {
			superRtype.tagAsAffectedByReload();
			// need to recurse up with the tagging
			superRtype.tagSupertypesAsAffectedByReload();
		}

		// need to recurse through super interfaces too
		ReloadableType[] superinterfaceRtypes = getInterfacesRtypes();
		if (superinterfaceRtypes != null) {
			for (ReloadableType superinterfaceRtype : superinterfaceRtypes) {
				superinterfaceRtype.tagAsAffectedByReload();
				superinterfaceRtype.tagSupertypesAsAffectedByReload();
			}
		}
	}

	// TODO who is clearing up dead entries?
	private void tagSubtypesAsAffectedByReload() {
		if (associatedSubtypes != null) {
			for (Reference<ReloadableType> ref : associatedSubtypes) {
				ReloadableType rsubtype = ref.get();
				if (rsubtype != null) {
					rsubtype.tagAsAffectedByReload();
					rsubtype.tagSubtypesAsAffectedByReload();
				}
			}
		}
	}

	private void tagAsAffectedByReload() {
		bits |= IMPACTED_BY_RELOAD;
		invokersCache_getMethods = null;
		invokersCache_getDeclaredMethods = null;
	}

	public boolean isAffectedByReload() {
		return (bits & IMPACTED_BY_RELOAD) != 0;
	}


	// TODO cache these field objects to avoid digging for them every time?
	/**
	 * When an enum type is reloaded, two caches need to be cleared out from the Class object for the enum type.
	 */
	private void resetEnumRelatedState() {
		if (clazz == null) {
			// the reloadabletype exists but the class hasn't been loaded yet!
			return;
		}
		try {
			Field f = clazz.getClass().getDeclaredField("enumConstants");
			f.setAccessible(true);
			f.set(clazz, null);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Field f = clazz.getClass().getDeclaredField("enumConstantDirectory");
			f.setAccessible(true);
			f.set(clazz, null);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void dump(byte[] newbytedata) {
		//	 TODO turn into general dumping mechanism for reloaded data?
		String slashedName = getSlashedName();
		if (slashedName.contains("BookController")) {
			GlobalConfiguration.dumpFolder = "/Users/aclement/Downloads/grails8344";
			Utils.dump(slashedName + "O", bytesInitial);
			Utils.dump(slashedName + "L", bytesLoaded);
			Utils.dump(slashedName + "N", newbytedata);
			Utils.dump(slashedName + "E", liveVersion.executor);
			Utils.dump(slashedName + "D", liveVersion.dispatcher);
		}
	}

	// TODO Subclassloader lookups (via subregistries) when the cglib proxies are being loaded below this registry
	// TODO caching discovered Method objects
	/**
	 * Go through proxies we know about in this registry and see if any of them are for the type we have just reloaded.
	 * If they are, regenerate them and reload them.
	 *
	 * @param versionsuffix the suffix to use when reloading the proxies (it matches what is being used to reload the
	 *            type)
	 */
	private void reloadProxiesIfNecessary(String versionsuffix) {
		ReloadableType proxy = typeRegistry.cglibProxies.get(this.slashedtypename);
		if (proxy != null) {
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Attempting reload of cglib proxy for type " + this.slashedtypename);
			}

			Object[] strategyAndGeneratorPair = CglibPluginCapturing.clazzToGeneratorStrategyAndClassGeneratorMap.get(
					getClazz());
			if (strategyAndGeneratorPair == null) {
				if (log.isLoggable(Level.SEVERE)) {
					log.severe(
							"Unable to find regeneration methods for cglib proxies - proxies will be out of date for type: "
									+ getClazz());
				}
				return;
			}
			Object a = strategyAndGeneratorPair[0];
			Object b = strategyAndGeneratorPair[1];
			// want to call a.generate(b)
			try {
				Method[] ms = a.getClass().getMethods();
				Method found = null;
				for (Method m : ms) {
					if (m.getName().equals("generate")) {
						found = m;// TODO cache
						break;
					}
				}
				found.setAccessible(true);
				byte[] bs = (byte[]) found.invoke(a, b);
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Proxy regenerate successful for " + this.slashedtypename);
				}
				proxy.loadNewVersion(versionsuffix, bs);
				proxy.runStaticInitializer();
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}

		proxy = typeRegistry.cglibProxiesFastClass.get(this.slashedtypename);
		if (proxy != null) {
			Object[] strategyAndFCGeneratorPair = CglibPluginCapturing.clazzToGeneratorStrategyAndFastClassGeneratorMap.get(
					getClazz());
			strategyAndFCGeneratorPair = CglibPluginCapturing.clazzToGeneratorStrategyAndFastClassGeneratorMap.get(
					getClazz());
			//				System.out.println("need to reload fastclass " + proxy + " os=" + os);
			if (strategyAndFCGeneratorPair != null) {
				Object a = strategyAndFCGeneratorPair[0];
				Object b = strategyAndFCGeneratorPair[1];
				// want to call a.generate(b)
				try {
					Method[] ms = a.getClass().getMethods();
					Method found = null;
					for (Method m : ms) {
						if (m.getName().equals("generate")) {
							found = m;
							break;
						}
					}
					byte[] bs = (byte[]) found.invoke(a, b);
					if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
						log.log(Level.INFO, "Proxy (fastclass) regenerate successful for " + this.slashedtypename);
					}
					proxy.loadNewVersion(versionsuffix, bs);
					proxy.runStaticInitializer();
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}

		try {
			if (this.clazz.isInterface()) {
				// JDK Proxy reloading
				Set<ReloadableType> relevantProxies = typeRegistry.jdkProxiesForInterface.get(this.slashedtypename);
				if (relevantProxies != null) {
					for (ReloadableType relevantProxy : relevantProxies) {
						Class<?>[] interfacesImplementedByProxy = relevantProxy.getClazz().getInterfaces();
						// TODO confirm slashedname correct
						byte[] newProxyBytes = Utils.generateProxyClass(relevantProxy.getSlashedName(),
								interfacesImplementedByProxy);
						relevantProxy.loadNewVersion(versionsuffix, newProxyBytes, true);
					}
				}
			}
		}
		catch (Throwable t) {
			new RuntimeException("Unexpected problem trying to reload proxy for interface " + this.dottedtypename,
					t).printStackTrace();
		}
	}

	Object[] reflectiveTargets;

	private final static int INDEX_SWAPINIT_METHOD = 0;

	private final static int INDEX_CALLSITEARRAY_FIELD = 1;

	private final static int INDEX_METACLASS_FIELD = 2;

	/**
	 * Groovy types need some extra fixup:
	 * <ul>
	 * <li>they contain a callsite array that caches destinations for calls. It needs clearing (it will be reinitialized
	 * when required)
	 * <li>not quite sure about the two: $staticClassInfo and GroovySystem removeMetaClass
	 * <li>ClassScope.getClassInfo(Foo.class).cachedClassRef.clear()
	 * </ul>
	 */
	public void fixupGroovyType() {

		StringBuilder s = new StringBuilder();
		if (reflectiveTargets == null) {
			reflectiveTargets = new Object[5];
			try {
				reflectiveTargets[INDEX_SWAPINIT_METHOD] = clazz.getDeclaredMethod("__$swapInit");
			}
			catch (Exception e) {
				s.append("cannot discover __$swapInit " + e.toString() + "  --  ");
			}
			try {
				reflectiveTargets[INDEX_CALLSITEARRAY_FIELD] = clazz.getDeclaredField("$callSiteArray");
			}
			catch (Exception e) {
				s.append("cannot discover $callSiteArray " + e.toString() + "  --  ");
			}
			try {
				reflectiveTargets[INDEX_METACLASS_FIELD] = clazz.getDeclaredField("$class$groovy$lang$MetaClass");
			}
			catch (Exception e) {
				s.append("cannot discover $class$groovy$lang$MetaClass " + e.toString() + "  --  ");
			}
			try {
				reflectiveTargets[3] = clazz.getDeclaredField("$staticClassInfo");
			}
			catch (Exception e) {
				s.append("cannot discover $staticClassInfo " + e.toString() + "  --  ");
			}
		}

		try {
			Method m = null;
			Field f = null;

			if (reflectiveTargets[INDEX_SWAPINIT_METHOD] != null) {
				m = (Method) reflectiveTargets[0];
				m.setAccessible(true);
				m.invoke(null);
			}
			if (reflectiveTargets[INDEX_CALLSITEARRAY_FIELD] != null) {
				f = (Field) reflectiveTargets[1];
				f.setAccessible(true);
				f.set(null, null);
			}

			if (reflectiveTargets[INDEX_METACLASS_FIELD] != null) {
				f = (Field) reflectiveTargets[2];
				f.setAccessible(true);
				f.set(null, null);
			}
			if (reflectiveTargets[3] != null) {
				f = (Field) reflectiveTargets[3];
				f.setAccessible(true);
				f.set(null, null);
			}
		}
		catch (Exception e) {
			s.append("cannot reset state" + e.toString() + "  --  ");
			//			new RuntimeException("Unable to fix up groovy state for " + this.dottedtypename, e);
		}

		try {
			Class<?> clazz = typeRegistry.getClass_GroovySystem();
			Field metaClassRegistryField = clazz.getDeclaredField("META_CLASS_REGISTRY");
			metaClassRegistryField.setAccessible(true);
			Object metaClassRegistry = metaClassRegistryField.get(null);
			Method metaClassRegistryMethod = metaClassRegistry.getClass().getDeclaredMethod("removeMetaClass",
					Class.class);
			metaClassRegistryMethod.setAccessible(true);
			metaClassRegistryMethod.invoke(metaClassRegistry, getClazz());
		}
		catch (Exception e) {
			s.append("Unable to remove meta class for groovy type " + this.dottedtypename + ": " + e.toString()
					+ "  --  ");
			//			new RuntimeException("Unable to remove meta class for groovy type " + this.dottedtypename, e)
			//					.printStackTrace(System.err);
		}

		// Implements: ClassInfo.getClassInfo(Class).cachedClassRef.clear()
		try {
			Method getClassInfoMethod = typeRegistry.getMethod_ClassInfo_getClassInfo();
			Object classInfoObject = getClassInfoMethod.invoke(null, this.clazz);
			Field cachedClassRefField = typeRegistry.getField_ClassInfo_cachedClassRef();
			cachedClassRefField.setAccessible(true);
			Object cachedClassRefObject = cachedClassRefField.get(classInfoObject);
			Class<?> lazyReferenceClass = cachedClassRefObject.getClass();// Class.forName("org.codehaus.groovy.util.LazyReference");
			// java.lang.NoSuchMethodException: org.codehaus.groovy.reflection.ClassInfo$LazyCachedClassRef.clear()
			Method clearMethod = lazyReferenceClass.getMethod("clear");//DeclaredMethod("clear");
			clearMethod.invoke(cachedClassRefObject);
		}
		catch (Exception e) {
			s.append("1 Unable to clear ClassInfo CachedClass data for groovy type " + this.dottedtypename + ": "
					+ e.toString()
					+ "  --  ");
			//			new RuntimeException("Unable to clear ClassInfo CachedClass data for groovy type " + this.dottedtypename, e)
			//					.printStackTrace(System.err);
		}

		try {

			//		    private static final ClassInfoSet globalClassSet = new ClassInfoSet(softBundle);
			Class<?> class_ClassInfo = typeRegistry.getClass_ClassInfo();
			Field field_globalClassSet = class_ClassInfo.getDeclaredField("globalClassSet");
			field_globalClassSet.setAccessible(true);
			Object/*ClassInfoSet*/ instance_classInfoSet = field_globalClassSet.get(null);
			Method method_ClassInfoSetRemove = instance_classInfoSet.getClass().getMethod("remove", Object.class);
			Object retval = method_ClassInfoSetRemove.invoke(instance_classInfoSet, this.clazz);

			//			Method getClassInfoMethod = typeRegistry.getMethod_ClassInfo_getClassInfo();
			//			Object classInfoObject = getClassInfoMethod.invoke(null, this.clazz);
			//			Field cachedClassRefField = typeRegistry.getField_ClassInfo_cachedClassRef();
			//			cachedClassRefField.setAccessible(true);
			//			Object cachedClassRefObject = cachedClassRefField.get(classInfoObject);
			//			Class<?> lazyReferenceClass = cachedClassRefObject.getClass();// Class.forName("org.codehaus.groovy.util.LazyReference");
			//			// java.lang.NoSuchMethodException: org.codehaus.groovy.reflection.ClassInfo$LazyCachedClassRef.clear()
			//			Method clearMethod = lazyReferenceClass.getMethod("clear");//DeclaredMethod("clear");
			//			clearMethod.invoke(cachedClassRefObject);
		}
		catch (Exception e) {
			s.append("2 Unable to clear ClassInfo CachedClass data for groovy type " + this.dottedtypename + ": "
					+ e.toString()
					+ "  --  ");
			//			new RuntimeException("Unable to clear ClassInfo CachedClass data for groovy type " + this.dottedtypename, e)
			//					.printStackTrace(System.err);
		}

		try {
			Set<WeakReference<Object>> deadInstances = null;
			Field f = getClazz().getDeclaredField("metaClass");
			for (WeakReference<Object> instance : liveInstances) {
				Object o = instance.get();
				if (o == null) {
					if (deadInstances == null) {
						deadInstances = new HashSet<WeakReference<Object>>();
					}
					deadInstances.add(instance);
				}
				else {
					f.setAccessible(true);
					f.set(o, null);
				}
			}
			if (deadInstances != null) {
				liveInstances.removeAll(deadInstances);
			}
		}
		catch (Exception e) {
			s.append("2 Unable to clear metaClass for groovy object instance (class=" + this.dottedtypename + ") "
					+ e.toString()
					+ "  --  ");
			//			new RuntimeException("Unable to clear metaClass for groovy object instance (class=" + this.dottedtypename + ")", e)
			//					.printStackTrace(System.err);
		}

	}

	public byte[] getLatestDispatcherBytes() {
		return (liveVersion == null ? null : liveVersion.dispatcher);
	}

	public Class<?> getLatestDispatcherClass() {
		return (liveVersion == null ? null : liveVersion.dispatcherClass);
	}

	public byte[] getInterfaceBytes() {
		return interfaceBytes;
	}

	public Object getLatestDispatcherInstance() {
		return (liveVersion == null ? null : liveVersion.dispatcherInstance);
	}

	public Object getLatestDispatcherInstance(boolean b) {
		if (b) {
			// TODO architect a real way to cause this to happen with a sensible name
			if (liveVersion == null) {
				loadNewVersion("0", bytesInitial);
			}
			return liveVersion.dispatcherInstance;
		}
		else {
			// Same as getLatestDispatcherInstance()
			return (liveVersion == null ? null : liveVersion.dispatcherInstance);
		}
	}

	public String getLatestDispatcherName() {
		return (liveVersion == null ? null : liveVersion.dispatcherName);
	}

	public byte[] getBytesInitial() {
		return bytesInitial;
	}

	public byte[] getBytesLoaded() {
		return bytesLoaded;
	}

	public byte[] getLatestExecutorBytes() {
		return (liveVersion == null ? null : liveVersion.executor);
	}

	public Class<?> getLatestExecutorClass() {
		return (liveVersion == null ? null : liveVersion.getExecutorClass());
	}

	public String getLatestExecutorName() {
		return (liveVersion == null ? null : liveVersion.executorName);
	}

	/**
	 * Gets the method corresponding to given name and descriptor, taking into consideration changes that have happened
	 * by reloading.
	 *
	 * @param name the member name
	 * @param descriptor the member descriptor (e.g. (Ljava/lang/String;)I)
	 * @return the MethodMember for that name and descriptor. Null if not found on a live version, or an exception if
	 *         there is no live version and it cannot be found.
	 */
	public MethodMember getCurrentMethod(String name, String descriptor) {
		if (liveVersion == null) {
			return getMethod(name, descriptor);
		}
		else {
			return liveVersion.getReloadableMethod(name, descriptor);
		}
	}

	/**
	 * Gets the method corresponding to given name and descriptor, from the original type descriptor.
	 *
	 * @param nameAndDescriptor the method name and descriptor (e.g. foo(Ljava/lang/String;)I)
	 * @return the MethodMember for the name and descriptor if it exists, otherwise null
	 */
	public MethodMember getOriginalMethod(String nameAndDescriptor) {
		return getMethod(nameAndDescriptor);
	}

	/**
	 * @return the dotted type name, eg. java.lang.String
	 */
	public String getName() {
		return dottedtypename;
	}

	/**
	 * @return the slashed type name, eg. java/lang/String
	 */
	public String getSlashedName() {
		return slashedtypename;
	}

	/**
	 * @return the type registry responsible for this type
	 */
	public TypeRegistry getTypeRegistry() {
		return typeRegistry;
	}

	/**
	 * @return the reference number for the type registry responsible for this type
	 */
	public int getTypeRegistryId() {
		return typeRegistry.getId();
	}

	public int getId() {
		return id;
	}

	/**
	 * Rewrite the code for this reloadable type. This involves:
	 * <ul>
	 * <li>rewriting the method bodies to add the condition check as to whether they are the most up to date version
	 * <li>rewriting the call sites for target methods to check they are there
	 * <li>filling in catcher methods
	 * </ul>
	 */
	public void rewriteCallSitesAndDefine() {
		// System.out.println(">rewriteCallSitesAndDefine(" + getName() + ")");

		//		byte[] rewrittenCallSites = GlobalConfiguration.callsideRewritingOn ? MethodInvokerRewriter.rewrite(typeRegistry,
		//				bytesInitial) : bytesInitial;
		//		this.bytesLoaded = TypeRewriter.rewrite(this, rewrittenCallSites);

		// This call replaces the two steps above (should do less bytecode unpacking/repacking)
		this.bytesLoaded = MergedRewrite.rewrite(this, bytesInitial);

		// TODO needs configurable debug that dumps loaded byte data at this point
		// Define the permanent piece
		// DEFAULT METHODS - remove the if
		if (!typedescriptor.isInterface()) {
			typeRegistry.defineClass(Utils.getInterfaceName(dottedtypename), interfaceBytes, true);
		}
		if (typeRegistry.shouldDefineClasses()) {
			/**
			 * Define the actual class. This is a separate call because it doesn't need doing when the ReloadableType is
			 * built during agent processing, because that agent will define the class.
			 */
			//			ClassPrinter.print(bytesLoaded);
			clazz = typeRegistry.defineClass(dottedtypename, bytesLoaded, true);
			//			System.out.println("is " + dottedtypename + " public? " + Modifier.isPublic(clazz.getModifiers()));
		}
	}

	/**
	 * This merges the two steps: method invocation rewriting and type rewriting
	 */
	static class MergedRewrite {

		public static byte[] rewrite(ReloadableType rtype, byte[] bytes) {
			try {
				ClassReader fileReader = new ClassReader(bytes);
				ChainedAdapters classAdaptor = new ChainedAdapters(rtype);
				fileReader.accept(classAdaptor, 0);
				return classAdaptor.getBytes();
			}
			catch (DontRewriteException drex) {
				return bytes;
			}
		}

		static class ChainedAdapters extends ClassVisitor implements Constants {

			public ChainedAdapters(ReloadableType rtype) {
				super(ASM5, new RewriteClassAdaptor(rtype.typeRegistry, new TypeRewriter.RewriteClassAdaptor(rtype,
						new ClassWriter(
								ClassWriter.COMPUTE_MAXS))));
			}

			public byte[] getBytes() {
				RewriteClassAdaptor rca = (RewriteClassAdaptor) cv;
				if (rca.isEnum && rca.fieldcount > GlobalConfiguration.enumLimit) {
					// that is too many fields, marking this as not reloadable
					// TODO ...
				}
				TypeRewriter.RewriteClassAdaptor a = (TypeRewriter.RewriteClassAdaptor) rca.getClassVisitor();
				return ((ClassWriter) a.getClassVisitor()).toByteArray();
			}
		}

	}

	/**
	 * @return the most recent dispatcher
	 */
	public Object fetchLatest() {
		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "fetchLatest called on " + this.getName());
		}
		return liveVersion.dispatcherInstance;
	}

	public boolean hasBeenReloaded() {
		return liveVersion != null;
	}

	public CurrentLiveVersion getLiveVersion() {
		return liveVersion;
	}

	// methods below are called from generated code

	// TODO optimize to numeric rather than string
	public boolean cchanged(String descriptor) {
		if (liveVersion != null) {
			boolean b = liveVersion.hasConstructorChanged(descriptor);
			return b;
		}
		return false;
	}

	@UsedByGeneratedCode
	public Object cchanged(int ctorId) {
		if (liveVersion != null) {
			boolean b = liveVersion.hasConstructorChanged(ctorId);
			if (b) {
				return getLatestDispatcherInstance();
			}
			// TODO need some intelligence here for recognizing constructor changes
			//			if (b) {
			//				return getLatestDispatcherInstance();
			//			}
		}
		return null;
	}

	/**
	 * Check if the specified method is different to the original form from the type as loaded.
	 *
	 * @param methodId the ID of the method currently executing
	 * @return 0 if the method cannot have changed. 1 if the method has changed. 2 if the method has been deleted in a
	 *         new version.
	 */
	@UsedByGeneratedCode
	public int changed(int methodId) {
		if (liveVersion == null) {
			return 0;
		}
		else {
			int retval = 0;
			// First check if a new version of the type was loaded:
			if (liveVersion != null) {
				if (GlobalConfiguration.logging && log.isLoggable(Level.FINER)) {
					log.info("MethodId=" + methodId + " method=" + typedescriptor.getMethod(methodId));
				}
				// TODO [perf] could be faster to return the executor here and if one isn't returned, do the original thing.
				// the reason for 3 ret vals here is due to catching methods that have been deleted early - lets let the
				// executor throw that exception, then this side we don't have to worry so much and instead of 2 check calls (changed then getexecutor) we can
				// just have one.  Will increase speed and reduce generated code (speeding up loadtime!)
				// was the method deleted?
				boolean b = liveVersion.incrementalTypeDescriptor.hasBeenDeleted(methodId);
				if (b) {
					retval = 2;
				}
				else {
					retval = liveVersion.incrementalTypeDescriptor.mustUseExecutorForThisMethod(methodId) ? 1 : 0;
				}
			}
			// TODO could be extremely fine grained and consider individual method changes
			//		return memberIntMap.get(methodId).hasChanged();
			return retval;
		}
	}

	@UsedByGeneratedCode
	public int clinitchanged() {
		if (GlobalConfiguration.logging && log.isLoggable(Level.FINER)) {
			log.entering("ReloadableType", "clinitchanged", null);
		}
		int retval = 0;
		// First check if a new version of the type was loaded:
		if (liveVersion != null) {
			retval = liveVersion.hasClinit() ? 1 : 0;
		}
		if (GlobalConfiguration.logging && log.isLoggable(Level.FINER)) {
			log.exiting("ReloadableType", "clinitchanged", retval);
		}
		return retval;
	}

	@UsedByGeneratedCode
	public Object fetchLatestIfExists(int methodId) {
		if (TypeRegistry.nothingReloaded) {
			return null;
		}
		if (changed(methodId) == 0) {
			return null;
		}
		return fetchLatest();
	}

	public String getSlashedSupertypeName() {
		return getTypeDescriptor().getSupertypeName();
	}

	public String[] getSlashedSuperinterfacesName() {
		return getTypeDescriptor().getSuperinterfacesName();
	}

	@UsedByGeneratedCode
	public __DynamicallyDispatchable getDispatcher() {
		// TODO need to handle when this hasn't been reloaded? or should the caller of this method
		__DynamicallyDispatchable dd = null;
		// TODO check for null?
		if (liveVersion == null) {
			simulateReload(); // TODO performance a bit sucky if we are taking this way out, and call stacks a bit deeper than we'd like
			// Problem we need to solve is that callers to getDispatcher() have an object and a name+descriptor and they
			// want the dispatcher that can answer their question
		}
		dd = (__DynamicallyDispatchable) liveVersion.dispatcherInstance;
		return dd;
	}

	/**
	 * Intended to handle dynamic dispatch. This will determine the right type to handle the specified method and return
	 * a dispatcher that can handle it.
	 *
	 * @param instance the target instance for the invocation
	 * @param nameAndDescriptor an encoded method name and descriptor, e.g. foo(Ljava/langString;)V
	 * @return a dispatcher that can handle the method indicated
	 */
	@UsedByGeneratedCode
	public __DynamicallyDispatchable determineDispatcher(Object instance, String nameAndDescriptor) {

		if (nameAndDescriptor.startsWith("<init>")) {
			// its a ctor, no dynamic lookup required
			if (!hasBeenReloaded()) {
				// TODO evaluate whether this is too naughty.  it forces creation of the dispatcher so we can return it
				loadNewVersion("0", bytesInitial);
			}
			return (__DynamicallyDispatchable) getLiveVersion().dispatcherInstance;
		}
		String dynamicTypeName = instance.getClass().getName();
		// iterate up the hierarchy finding the first person that can satisfy that method from a virtual dispatch perspective
		ReloadableType rtype = typeRegistry.getReloadableType(dynamicTypeName.replace('.', '/'));
		if (rtype == null) {
			throw new ReloadException("ReloadableType.determineDispatcher(): expected " + dynamicTypeName
					+ " to be reloadable");
		}
		boolean found = false;
		while (rtype != null && !found) {
			if (rtype.hasBeenReloaded()) {
				// Does the type now define it:
				// TODO not sure if we should be looking at deleted methods here.  It is possible they are
				// handled by catchers/executors delegating as appropriate - and in those cases we never
				// end up in determineDispatcher
				List<MethodMember> mms = rtype.getLiveVersion().incrementalTypeDescriptor.getNewOrChangedMethods();
				for (MethodMember mm : mms) {
					// boolean wd = IncrementalTypeDescriptor.wasDeleted(mm);
					if (mm.isPrivate()) { // TODO is skipping of private methods correct thing to do
						continue;
					}
					if (mm.getNameAndDescriptor().equals(nameAndDescriptor)) {
						// the reloaded version does implement this method
						found = true;
						break;
					}
				}
			}
			else {
				// Did the type originally define it:
				MethodMember[] mms = rtype.getTypeDescriptor().getMethods();
				for (MethodMember mm : mms) {
					// TODO don't need superdispatcher check, name won't match will it...
					if (mm.getNameAndDescriptor().equals(nameAndDescriptor) && !MethodMember.isCatcher(mm)
							&& !MethodMember.isSuperDispatcher(mm)) {
						// the original version does implement it
						found = true;
						break;
					}
				}
			}
			if (!found) {
				String slashedSupername = rtype.getTypeDescriptor().getSupertypeName();
				rtype = typeRegistry.getReloadableType(slashedSupername);
			}
		}
		if (found) {
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "appears that type " + rtype.getName() + " implements " + nameAndDescriptor);
			}
		}
		if (rtype == null) {
			return null;
		}

		if (!rtype.hasBeenReloaded()) {
			// TODO evaluate whether this is too naughty.  it forces creation of the dispatcher so we can return it
			rtype.loadNewVersion("0", rtype.bytesInitial);
		}
		return (__DynamicallyDispatchable) rtype.getLiveVersion().dispatcherInstance;
	}

	/**
	 * @return type name without the package prefix
	 */
	public String getBaseName() {
		int dotIndex = dottedtypename.lastIndexOf(".");
		if (dotIndex == -1) {
			return dottedtypename;
		}
		else {
			return dottedtypename.substring(dotIndex + 1);
		}
	}

	public TypeDescriptor getLatestTypeDescriptor() {
		if (liveVersion == null) {
			return typedescriptor;
		}
		else {
			return liveVersion.incrementalTypeDescriptor.getLatestTypeDescriptor();
		}
	}

	public MethodMember getFromLatestByDescriptor(String nameAndDescriptor) {
		return getLiveVersion().incrementalTypeDescriptor.getFromLatestByDescriptor(nameAndDescriptor);
	}

	public MethodMember getMethod(String nameAndDescriptor) {
		for (MethodMember method : typedescriptor.getMethods()) {
			if (nameAndDescriptor.startsWith(method.getName()) && nameAndDescriptor.endsWith(method.getDescriptor())) {
				return method;
			}
		}
		return null;
	}

	// TODO: [perf] cache this?
	public MethodMember getCurrentConstructor(String desc) {
		TypeDescriptor typeDesc = getLatestTypeDescriptor();
		return typeDesc.getConstructor(desc);
	}

	public MethodMember getOriginalConstructor(String desc) {
		for (MethodMember method : typedescriptor.getConstructors()) {
			if (method.getDescriptor().equals(desc)) {
				return method;
			}
		}
		return null;
	}

	public JavaMethodCache getJavaMethodCache() {
		if (javaMethodCache == null) {
			javaMethodCache = new JavaMethodCache();
		}
		return javaMethodCache;
	}

	/**
	 * Find the named instance field either on this reloadable type or on a reloadable supertype - it will not go into
	 * the non-reloadable types. This method also avoids interfaces because it is looking for instance fields. This is
	 * slightly naughty but if we assume the code we are reloading is valid code, it should never be referring to
	 * interface fields.
	 *
	 * @param name the name of the field to locate
	 * @return the FieldMember or null if the field is not found
	 */
	public FieldMember findInstanceField(String name) {
		FieldMember found = getLatestTypeDescriptor().getField(name);
		if (found != null) {
			return found;
		}
		// Walk up the supertypes - this is looking for instance fields so no need to search interfaces
		String slashedSupername = getTypeDescriptor().getSupertypeName();
		ReloadableType rtype = typeRegistry.getReloadableType(slashedSupername);

		while (rtype != null) {
			found = rtype.getLatestTypeDescriptor().getField(name);
			if (found != null) {
				break;
			}
			slashedSupername = rtype.getTypeDescriptor().getSupertypeName();
			rtype = typeRegistry.getReloadableType(slashedSupername);
		}
		return found;
	}

	/**
	 * Search for a static field from this type upwards, as far as the topmost reloadable types. This is searching for a
	 * field, it is not checking the result. It is up to the caller to check they have not ended up with an instance
	 * field and throw the appropriate exception.
	 *
	 * @param name the name of the field to look for
	 * @return a FieldMember for the named field or null if not found
	 */
	public FieldMember findStaticField(String name) {
		return searchType(this, name);
	}

	private FieldMember searchType(ReloadableType rtype, String fieldname) {
		if (rtype != null) {
			TypeDescriptor td = rtype.getLatestTypeDescriptor();
			FieldMember field = td.getField(fieldname);
			if (field != null) {
				return field;
			}
			String[] interfaces = td.getSuperinterfacesName();
			if (interfaces != null) {
				for (String intface : interfaces) {
					ReloadableType itype = typeRegistry.getReloadableType(intface);
					if (intface != null) {
						field = searchType(itype, fieldname);
						if (field != null) {
							return field;
						}
					}
				}
			}
			ReloadableType stype = typeRegistry.getReloadableType(td.getSupertypeName());
			if (stype != null) {
				return searchType(stype, fieldname);
			}
		}
		return null;
	}

	/**
	 * Attempt to set the value of a field on an instance to the specified value.
	 *
	 * @param instance the object upon which to set the field (maybe null for static fields)
	 * @param fieldname the name of the field
	 * @param isStatic whether the field is static
	 * @param newValue the new value to put into the field
	 * @throws IllegalAccessException if there is a problem setting the field value
	 */
	public void setField(Object instance, String fieldname, boolean isStatic, Object newValue)
			throws IllegalAccessException {
		FieldReaderWriter fieldReaderWriter = locateField(fieldname);
		if (isStatic && !fieldReaderWriter.isStatic()) {
			throw new IncompatibleClassChangeError("Expected static field "
					+ fieldReaderWriter.theField.getDeclaringTypeName()
					+ "." + fieldReaderWriter.theField.getName());
		}
		else if (!isStatic && fieldReaderWriter.isStatic()) {
			throw new IncompatibleClassChangeError("Expected non-static field "
					+ fieldReaderWriter.theField.getDeclaringTypeName()
					+ "." + fieldReaderWriter.theField.getName());
		}

		if (fieldReaderWriter.isStatic()) {
			fieldReaderWriter.setStaticFieldValue(getClazz(), newValue, null);
		}
		else {
			fieldReaderWriter.setValue(instance, newValue, null);
		}
	}

	private Set<WeakReference<Object>> liveInstances = Collections.synchronizedSet(
			new HashSet<WeakReference<Object>>());

	private ReferenceQueue<Object> liveInstancesRQ = new ReferenceQueue<Object>();

	// reflective state caching
	public Reference<Method[]> jlClassGetDeclaredMethods_cache = new WeakReference<Method[]>(null);

	public Reference<Method[]> jlClassGetMethods_cache = new WeakReference<Method[]>(null);

	/**
	 * Attempt to set the value of a field on an instance to the specified value. Simply locate the field, which returns
	 * an object capable of reading/writing it, then use that to retrieve the value.
	 *
	 * @param instance the object upon which to set the field (maybe null for static fields)
	 * @param fieldname the name of the field
	 * @param isStatic whether the field is static or not
	 * @return the field value
	 * @throws IllegalAccessException if there is a problem accessing the field
	 */
	public Object getField(Object instance, String fieldname, boolean isStatic) throws IllegalAccessException {
		FieldReaderWriter fieldReaderWriter = locateField(fieldname);
		if (isStatic && !fieldReaderWriter.isStatic()) {
			throw new IncompatibleClassChangeError("Expected static field "
					+ fieldReaderWriter.theField.getDeclaringTypeName()
					+ "." + fieldReaderWriter.theField.getName());
		}
		else if (!isStatic && fieldReaderWriter.isStatic()) {
			throw new IncompatibleClassChangeError("Expected non-static field "
					+ fieldReaderWriter.theField.getDeclaringTypeName()
					+ "." + fieldReaderWriter.theField.getName());
		}
		Object o = null;
		if (fieldReaderWriter.isStatic()) {
			o = fieldReaderWriter.getStaticFieldValue(getClazz(), null);
		}
		else {
			o = fieldReaderWriter.getValue(instance, null);
		}

		return o;
	}

	/*
	 * Find the field according to the rules of section 5.4.3.2 of the spec.
	 */
	// TODO [perf] performance sucks as we walk multiple times!
	public FieldReaderWriter locateField(String name) {
		if (hasFieldChangedInHierarchy(name)) {
			return walk(name, getLatestTypeDescriptor());
		}
		else {
			return getFieldInHierarchy(name);
		}
	}

	public FieldReaderWriter walk(String name, TypeDescriptor typeDescriptor) {
		FieldMember theField = typeDescriptor.getField(name);
		if (theField != null) {
			// Found it
			return new FieldReaderWriter(theField, typeDescriptor);
		}
		else {
			String[] superinterfaceNames = typeDescriptor.getSuperinterfacesName();
			for (String superinterfaceName : superinterfaceNames) {
				TypeDescriptor interfaceTypeDescriptor = getTypeRegistry().getLatestDescriptorFor(superinterfaceName);
				// may or may not be a reloadable type!
				FieldReaderWriter locator = walk(name, interfaceTypeDescriptor);
				if (locator != null) {
					return locator;
				}
			}
			String supertypename = typeDescriptor.getSupertypeName();
			if (supertypename != null) {
				TypeDescriptor superTypeDescriptor = getTypeRegistry().getLatestDescriptorFor(supertypename);
				FieldReaderWriter locator = walk(name, superTypeDescriptor);
				return locator;
			}
		}
		return null;
	}

	enum FieldWalkDiscoveryResult {
		CHANGED_STOPNOW, UNCHANGED_STOPWALKINGNOW, DONTKNOW;
	}

	private FieldWalkDiscoveryResult hasFieldChangedInHierarchy(String fieldname, String slashedName) {

		ReloadableType rtype = typeRegistry.getReloadableType(slashedName);
		if (rtype == null) {
			return FieldWalkDiscoveryResult.UNCHANGED_STOPWALKINGNOW; // it is in a supertype, we can let regular resolution proceed
		}
		TypeDescriptor originalTypeDescriptor = rtype.getTypeDescriptor();
		FieldMember originalField = originalTypeDescriptor.getField(fieldname);

		TypeDescriptor typedescriptor = rtype.getLatestTypeDescriptor();
		FieldMember field = typedescriptor.getField(fieldname);
		if (originalField != null && field == null) {
			// Field got removed from this type, going to have to resort to indirection logic
			// or we'll trip over the original version when letting the field instruction run
			return FieldWalkDiscoveryResult.CHANGED_STOPNOW;
		}

		if (originalField != null && field != null) {
			if (originalField.equals(field)) {
				return FieldWalkDiscoveryResult.UNCHANGED_STOPWALKINGNOW;
			}
			else {
				return FieldWalkDiscoveryResult.CHANGED_STOPNOW;
			}
		}

		if (originalField == null && field != null) {
			// has been introduced here
			return FieldWalkDiscoveryResult.CHANGED_STOPNOW;
		}

		// TODO [perf] could avoid super interface walk for instance fields? or do we need to be sure?
		// guess if cache about nothing changing is higher up, this cost doesn't matter

		// try the superinterfaces
		String[] interfaces = originalTypeDescriptor.superinterfaceNames;
		if (interfaces != null) {
			for (String intface : interfaces) {
				FieldWalkDiscoveryResult b = hasFieldChangedInHierarchy(fieldname, intface);
				if (b != FieldWalkDiscoveryResult.DONTKNOW) {
					return b;
				}
			}
		}

		// try the superclass
		return hasFieldChangedInHierarchy(fieldname, originalTypeDescriptor.supertypeName);
	}

	/*
	 * Want to check if this field looks the same as originally declared and is on the same type as it was
	 */
	public boolean hasFieldChangedInHierarchy(String name) {
		// Find the field:
		ReloadableType rtype = this;
		FieldMember field = null;
		// Did it exist on this type originally?
		TypeDescriptor originalTypeDescriptor = rtype.getTypeDescriptor();
		FieldMember originalField = originalTypeDescriptor.getField(name);

		TypeDescriptor typedescriptor = rtype.getLatestTypeDescriptor();
		field = typedescriptor.getField(name);
		if (originalField != null && field == null) {
			// Field got removed from this type, going to have to resort to indirection logic or we'll trip over the original version when
			// letting the field instruction run
			return true;
		}

		if (originalField != null && field != null) {
			return !originalField.equals(field);
		}
		if (originalField == null && field != null) {
			return true;
		}

		FieldWalkDiscoveryResult b = hasFieldChangedInHierarchy(name, rtype.getTypeDescriptor().getSupertypeName());
		switch (b) {
			case CHANGED_STOPNOW:
				return true;
			case UNCHANGED_STOPWALKINGNOW:
				return false;
			case DONTKNOW:
				throw new IllegalStateException();
		}
		throw new IllegalStateException();
	}

	private Field findField(Class<?> clazz, String name) {
		Field field = null;
		try {
			Field[] fields = clazz.getDeclaredFields();
			if (fields != null) {
				for (Field field2 : fields) {
					if (field2.getName().equals(name)) {
						field = field2;
					}
				}
			}
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		if (field != null) {
			return field;
		}
		Class<?>[] interfaces = clazz.getInterfaces();
		if (interfaces != null) {
			for (Class<?> intface : interfaces) {
				field = findField(intface, name);
				if (field != null) {
					return field;
				}
			}
		}
		Class<?> supertype = clazz.getSuperclass();
		if (supertype != null) {
			return findField(supertype, name);
		}
		return null;
	}

	public FieldReaderWriter getFieldInHierarchy(String name) {
		return new ReflectionFieldReaderWriter(findField(this.getClazz(), name));
	}

	public void clearClassloaderLinks() {
		if (hasBeenReloaded()) {
			this.liveVersion.clearClassloaderLinks();
		}
	}

	public void reloadMostRecentDispatcherAndExecutor() {
		if (hasBeenReloaded()) {
			this.liveVersion.reloadMostRecentDispatcherAndExecutor();
		}
	}

	@SuppressWarnings("unchecked")
	public void trackLiveInstance(Object instance) {
		while (true) {
			Reference<Object> r = (Reference<Object>) liveInstancesRQ.poll();
			if (r != null) {
				liveInstances.remove(r);
			}
			else {
				break;
			}
		}
		liveInstances.add(new WeakReference<Object>(instance, liveInstancesRQ));
	}

	public void runStaticInitializer() {
		if (hasBeenReloaded()) {
			this.liveVersion.runStaticInitializer();
		}
	}

	public boolean isResolved() {
		return (bits & IS_RESOLVED) != 0;
	}

	public void setResolved() {
		bits |= IS_RESOLVED;
	}

	public void setSuperclass(Class<?> superclazz) {
		this.superclazz = superclazz;
	}

	/**
	 * Return the ReloadableType representing the superclass of this type. If the supertype is not reloadable, this
	 * method will return null. The ReloadableType that is returned may not be within the same type registry, if the
	 * supertype was loaded by a different classloader.
	 *
	 * @return the ReloadableType for the supertype or null if it is not reloadable
	 */
	public ReloadableType getSuperRtype() {
		if (superRtype != null) {
			return superRtype;
		}
		if (superclazz == null) {
			// Not filled in yet? Why is this code different to the interface case?
			String name = this.getSlashedSupertypeName();
			if (name == null) {
				return null;
			}
			else {
				ReloadableType rtype = typeRegistry.getReloadableSuperType(name);
				superRtype = rtype;
				return superRtype;
			}
		}
		else {
			ClassLoader superClassLoader = superclazz.getClassLoader();
			TypeRegistry superTypeRegistry = TypeRegistry.getTypeRegistryFor(superClassLoader);
			superRtype = superTypeRegistry.getReloadableType(superclazz);
			return superRtype;
		}
	}

	public ReloadableType[] getInterfacesRtypes() {
		if (interfaceRtypes != null) {
			return interfaceRtypes;
		}
		if (this.getSlashedSuperinterfacesName() == null) {
			return null;
		}
		else {
			List<ReloadableType> reloadableInterfaces = new ArrayList<ReloadableType>();
			String[] names = this.getSlashedSuperinterfacesName();
			for (String name : names) {
				ReloadableType interfaceRtype = typeRegistry.getReloadableSuperType(name);
				if (interfaceRtype != null) { // If null then that interface is not reloadable
					reloadableInterfaces.add(interfaceRtype);
				}
			}
			interfaceRtypes = reloadableInterfaces.toArray(new ReloadableType[reloadableInterfaces.size()]);
			return interfaceRtypes;
		}
	}


	public boolean hasStaticInitializer() {
		return this.typedescriptor.hasClinit();
	}

	/**
	 * @param child the new reloadable subtype to record
	 */
	public void recordSubtype(ReloadableType child) {
		if (associatedSubtypes == null) {
			associatedSubtypes = new ArrayList<Reference<ReloadableType>>();
		}
		associatedSubtypes.add(new WeakReference<ReloadableType>(child));
		if (this.isAffectedByReload()) {
			child.tagAsAffectedByReload();
			child.tagSubtypesAsAffectedByReload();
		}
	}

	public List<Reference<ReloadableType>> getAssociatedSubtypes() {
		return associatedSubtypes;
	}

	/**
	 * For this specified reloadable type, records the type with its parent types (super class and super interfaces).
	 * With this information the system can run faster when reloading has occurred.
	 */
	public void createTypeAssociations() {
		// Connect the child to the parent rtype and interface rtypes
		ClassLoader classLoader = getClazz().getClassLoader();
		if (classLoader == null) {
			return;
		}
		ReloadableType srtype = getSuperRtype();
		if (srtype != null) {
			srtype.recordSubtype(this);
		}
		ReloadableType[] irtypes = getInterfacesRtypes();
		if (irtypes != null) {
			for (ReloadableType irtype : irtypes) {
				irtype.recordSubtype(this);
			}
		}
	}
}
