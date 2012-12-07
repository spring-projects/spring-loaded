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

import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Encapsulates what has changed about a method when it is reloaded, compared to the original form.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class MethodDelta {
	public int changed;

	private final static int CHANGED_INSTRUCTIONS = 0x0001;
	private final static int CHANGED_ACCESS = 0x0002;
	private final static int CHANGED_ANNOTATIONS = 0x0004;
	private final static int CHANGED_INVOKESPECIAL = 0x0008;
	private final static int CHANGED_CODE = 0x0010;

	private final static int CHANGED_MASK = CHANGED_INSTRUCTIONS | CHANGED_ACCESS | CHANGED_ANNOTATIONS | CHANGED_INVOKESPECIAL
			| CHANGED_CODE;

	// o = original, n = new
	public final String name;
	public final String desc;
	String annotationChanges;
	int oAccess, nAccess;
	String oInvokespecialDescriptor, nInvokespecialDescriptor;
	AbstractInsnNode[] oInstructions, nInstructions;

	public MethodDelta(String name, String desc) {
		this.name = name;
		this.desc = desc;
	}

	public void setAnnotationsChanged(String annotationChanges) {
		this.annotationChanges = annotationChanges;
		this.changed |= CHANGED_ANNOTATIONS;
	}

	public boolean hasAnyChanges() {
		return (changed & CHANGED_MASK) != 0;
	}

	public boolean hasInvokeSpecialChanged() {
		return (changed & CHANGED_INVOKESPECIAL) != 0;
	}

	public boolean hasCodeChanged() {
		return (changed & CHANGED_CODE) != 0;
	}

	public void setAccessChanged(int oldAccess, int newAccess) {
		this.oAccess = oldAccess;
		this.nAccess = newAccess;
		this.changed |= CHANGED_ACCESS;
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("MethodDelta[method:").append(name).append(desc);
		if ((changed & CHANGED_ACCESS) != 0) {
			s.append(" access:").append(oAccess).append(">").append(nAccess);
		}
		if ((changed & CHANGED_ANNOTATIONS) != 0) {
			s.append(" annotations:").append(annotationChanges);
		}
		s.append("]");
		return s.toString();
	}

	public void setInstructionsChanged(AbstractInsnNode[] oInstructions, AbstractInsnNode[] nInstructions) {
		this.changed |= CHANGED_INSTRUCTIONS;
	}

	public void setInvokespecialChanged(String oInvokeSpecialDescriptor, String nInvokeSpecialDescriptor) {
		this.changed |= CHANGED_INVOKESPECIAL;
		this.oInvokespecialDescriptor = oInvokeSpecialDescriptor;
		this.nInvokespecialDescriptor = nInvokeSpecialDescriptor;
	}

	public void setCodeChanged(AbstractInsnNode[] oInstructions, AbstractInsnNode[] nInstructions) {
		this.changed |= CHANGED_CODE;
		this.oInstructions = oInstructions;
		this.nInstructions = nInstructions;
	}
}