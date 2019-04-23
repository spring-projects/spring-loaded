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

/**
 * Encapsulates what has changed about a field on a reload.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class FieldDelta {

	public int changed;

	private final static int CHANGED_TYPE = 0x0001;

	private final static int CHANGED_ACCESS = 0x0002;

	private final static int CHANGED_ANNOTATIONS = 0x0004;

	private final static int CHANGED_MASK = CHANGED_TYPE | CHANGED_ACCESS | CHANGED_ANNOTATIONS;

	public final String name;

	// o = original, n = new
	String oDesc, nDesc;

	String annotationChanges;

	int oAccess, nAccess;

	public FieldDelta(String name) {
		this.name = name;
	}

	public void setTypeChanged(String oldDesc, String newDesc) {
		this.oDesc = oldDesc;
		this.nDesc = newDesc;
		this.changed |= CHANGED_TYPE;
	}

	public void setAnnotationsChanged(String annotationChanges) {
		this.annotationChanges = annotationChanges;
		this.changed |= CHANGED_ANNOTATIONS;
	}

	public boolean hasAnyChanges() {
		return (changed & CHANGED_MASK) != 0;
	}

	public void setAccessChanged(int oldAccess, int newAccess) {
		this.oAccess = oldAccess;
		this.nAccess = newAccess;
		this.changed |= CHANGED_ACCESS;
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("FieldDelta[field:").append(name);
		if ((changed & CHANGED_TYPE) != 0) {
			s.append(" type:").append(oDesc).append(">").append(nDesc);
		}
		if ((changed & CHANGED_ACCESS) != 0) {
			s.append(" access:").append(oAccess).append(">").append(nAccess);
		}
		if ((changed & CHANGED_ANNOTATIONS) != 0) {
			s.append(" annotations:").append(annotationChanges);
		}
		s.append("]");
		return s.toString();
	}

}
