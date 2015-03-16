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

/**
 * Represents a double dotted type pattern. For example: com.foo.bar..* - this has the same meaning as in AspectJ.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class PrefixTypePattern extends TypePattern {

	private String pattern;

	/**
	 * @param pattern the prefix pattern of the form 'com.foo.bar..*'
	 */
	public PrefixTypePattern(String pattern) {
		this.pattern = pattern.substring(0, pattern.length() - 2); // chop off the '.*'
	}

	protected boolean internalMatches(String input) {
		boolean b = input.startsWith(pattern);
		return b;
	}

	public String toString() {
		return "text:" + pattern + ".*";
	}

}
