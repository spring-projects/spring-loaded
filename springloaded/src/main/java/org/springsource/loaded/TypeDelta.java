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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Encapsulates what has changed between two versions of a type - it is used to determine if a reload is possible and
 * also passed on events related to reloading so that the plugins can tailor their actions based on what prevented
 * reloading. The various <tt>hasXXX</tt> and <tt>getXXX</tt> methods should be used to query it.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class TypeDelta {

	private long changed;

	private final static long CHANGED_VERSION = 0x00000001;

	private final static long CHANGED_ACCESS = 0x00000002;

	private final static long CHANGED_SUPERNAME = 0x00000004;

	private final static long CHANGED_INTERFACES = 0x00000008;

	private final static long CHANGED_NAME = 0x00000010;

	private final static long CHANGED_SIGNATURE = 0x00000020;

	private final static long CHANGED_TYPE_MASK = CHANGED_VERSION | CHANGED_ACCESS | CHANGED_SUPERNAME
			| CHANGED_INTERFACES
			| CHANGED_NAME | CHANGED_SIGNATURE;

	private final static long CHANGED_NEWFIELDS = 0x00000040;

	private final static long CHANGED_LOSTFIELDS = 0x00000080;

	private final static long CHANGED_CHANGEDFIELDS = 0x00000100;

	private final static long CHANGED_FIELD_MASK = CHANGED_NEWFIELDS | CHANGED_LOSTFIELDS | CHANGED_CHANGEDFIELDS;

	private final static long CHANGED_NEWMETHODS = 0x00000200;

	private final static long CHANGED_LOSTMETHODS = 0x00000400;

	private final static long CHANGED_CHANGEDMETHODS = 0x0000800;

	private final static long CHANGED_METHOD_MASK = CHANGED_NEWMETHODS | CHANGED_LOSTMETHODS | CHANGED_CHANGEDMETHODS;

	private final static long CHANGES = CHANGED_TYPE_MASK | CHANGED_FIELD_MASK | CHANGED_METHOD_MASK;

	public int oAccess, nAccess;

	public int oVersion, nVersion;

	public String oName, nName;

	public String oSignature, nSignature;

	public String oSuperName, nSuperName;

	public List<String> oInterfaces, nInterfaces;

	Map<String, FieldNode> brandNewFields;

	Map<String, FieldNode> lostFields;

	Map<String, FieldDelta> changedFields;

	Map<String, MethodNode> brandNewMethods;

	Map<String, MethodNode> lostMethods;

	Map<String, MethodDelta> changedMethods;

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("TypeDelta Summary\n");
		// type declaration
		s.append("TypeDeclaration changes:\n");
		if (hasTypeVersionChanged()) {
			s.append("typeversion changed: o=" + oVersion + " n=" + nVersion + "\n");
		}
		if (hasTypeAccessChanged()) {
			s.append("typeaccess changed: o=" + oAccess + " n=" + nAccess + "\n");
		}
		if (hasTypeSupertypeChanged()) {
			s.append("typesupertype changed: o=" + oSuperName + " n=" + nSuperName + "\n");
		}
		if (hasTypeInterfacesChanged()) {
			s.append("typeinterfaces changed: o=" + oInterfaces + " n=" + nInterfaces + "\n");
		}
		if (hasTypeNameChanged()) {
			s.append("typename changed: o=" + oName + " n=" + nName + "\n");
		}
		if (hasTypeSignatureChanged()) {
			s.append("typesignature changed: o=" + oSignature + " n=" + nSignature + "\n");
		}
		// ...

		return s.toString();
	}

	void setTypeAccessChange(int oldAccess, int newAccess) {
		this.oAccess = oldAccess;
		this.nAccess = newAccess;
		this.changed |= CHANGED_ACCESS;
	}

	void setTypeNameChange(String oldName, String newName) {
		this.oName = oldName;
		this.nName = newName;
		this.changed |= CHANGED_NAME;
	}

	void setTypeSignatureChange(String oldSignature, String newSignature) {
		this.oSignature = oldSignature;
		this.nSignature = newSignature;
		this.changed |= CHANGED_SIGNATURE;
	}

	//	public void setTypeVersionChange(int oldVersion, int newVersion) {
	//		this.oVersion = oldVersion;
	//		this.nVersion = newVersion;
	//		this.changed |= CHANGED_VERSION;
	//	}

	void setTypeSuperNameChange(String oldSuperName, String newSuperName) {
		this.oSuperName = oldSuperName;
		this.nSuperName = newSuperName;
		this.changed |= CHANGED_SUPERNAME;
	}

	void setTypeInterfacesChange(List<String> oldInterfaces, List<String> newInterfaces) {
		this.oInterfaces = oldInterfaces;
		this.nInterfaces = newInterfaces;
		this.changed |= CHANGED_INTERFACES;
	}

	void addNewField(FieldNode nField) {
		if (brandNewFields == null) {
			brandNewFields = new HashMap<String, FieldNode>();
		}
		brandNewFields.put(nField.name, nField);
		this.changed |= CHANGED_NEWFIELDS;
	}

	void addLostField(FieldNode lField) {
		if (lostFields == null) {
			lostFields = new HashMap<String, FieldNode>();
		}
		lostFields.put(lField.name, lField);
		this.changed |= CHANGED_LOSTFIELDS;
	}

	void addChangedField(FieldDelta fd) {
		if (changedFields == null) {
			changedFields = new HashMap<String, FieldDelta>();
		}
		changedFields.put(fd.name, fd);
		this.changed |= CHANGED_CHANGEDFIELDS;
	}

	void addNewMethod(MethodNode nMethod) {
		if (brandNewMethods == null) {
			brandNewMethods = new HashMap<String, MethodNode>();
		}
		brandNewMethods.put(nMethod.name + nMethod.desc, nMethod);
		this.changed |= CHANGED_NEWMETHODS;
	}

	void addLostMethod(MethodNode nMethod) {
		if (lostMethods == null) {
			lostMethods = new HashMap<String, MethodNode>();
		}
		lostMethods.put(nMethod.name + nMethod.desc, nMethod);
		this.changed |= CHANGED_LOSTMETHODS;
	}

	void addChangedMethod(MethodDelta md) {
		if (changedMethods == null) {
			changedMethods = new HashMap<String, MethodDelta>();
		}
		changedMethods.put(md.name + md.desc, md);
		this.changed |= CHANGED_CHANGEDMETHODS;
	}

	public boolean hasTypeDeclarationChanged() {
		return (changed & CHANGED_TYPE_MASK) != 0;
	}

	public boolean hasTypeNameChanged() {
		return (changed & CHANGED_NAME) != 0;
	}

	public boolean hasTypeVersionChanged() {
		return (changed & CHANGED_VERSION) != 0;
	}

	public boolean hasTypeAccessChanged() {
		return (changed & CHANGED_ACCESS) != 0;
	}

	public boolean hasTypeSupertypeChanged() {
		return (changed & CHANGED_SUPERNAME) != 0;
	}

	/**
	 * @return true if the list of interfaces implemented by this type has changed
	 */
	public boolean hasTypeInterfacesChanged() {
		return (changed & CHANGED_INTERFACES) != 0;
	}

	public boolean hasTypeSignatureChanged() {
		return (changed & CHANGED_SIGNATURE) != 0;
	}

	public boolean hasAnythingChanged() {
		return (changed & CHANGES) != 0;
	}

	public boolean hasNewFields() {
		return (changed & CHANGED_NEWFIELDS) != 0;
	}

	public boolean hasLostFields() {
		return (changed & CHANGED_LOSTFIELDS) != 0;
	}

	public boolean haveFieldsChangedOrBeenAddedOrRemoved() {
		return (changed & CHANGED_FIELD_MASK) != 0;
	}

	public boolean haveFieldsChanged() {
		return (changed & CHANGED_CHANGEDFIELDS) != 0;
	}

	public boolean haveMethodsChanged() {
		return (changed & CHANGED_CHANGEDMETHODS) != 0;
	}

	public boolean haveMethodsChangedOrBeenAddedOrRemoved() {
		return (changed & CHANGED_METHOD_MASK) != 0;
	}

	public boolean haveMethodsBeenAdded() {
		return (changed & CHANGED_NEWMETHODS) != 0;
	}

	public boolean haveMethodsBeenDeleted() {
		return (changed & CHANGED_LOSTMETHODS) != 0;
	}

	public Map<String, FieldNode> getNewFields() {
		return brandNewFields;
	}

	public Map<String, FieldNode> getLostFields() {
		return lostFields;
	}

	public Map<String, FieldDelta> getChangedFields() {
		return changedFields;
	}

	public Map<String, MethodDelta> getChangedMethods() {
		return changedMethods;
	}

}
