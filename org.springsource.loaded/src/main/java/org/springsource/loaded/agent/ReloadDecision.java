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
package org.springsource.loaded.agent;

/**
 * 
 * @author Andy Clement
 * @since 0.7.1
 */
public enum ReloadDecision {
	/**
	 * YES means the plugin thinks it should definetly be reloadable
	 */
	YES,
	/**
	 * NO means the plugin thinks is should definetly not be reloadable
	 */
	NO,
	/**
	 * PASS means the plugin has no opinion, leave it to other plugins to decide
	 */
	PASS
}