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
package org.springsource.loaded.testgen;

public interface IClassProvider {

	/**
	 * Returns a class object representing the given type from the .class file with the given version number.
	 * <p>
	 * This method should only be called once, for a given typename, and should take care to ensure none of the types that may be
	 * loaded implicitly (because they are referred from the loaded type, are later loaded again with 'loadClassVersion'.
	 */
	Class<?> loadClassVersion(String typeName, String version);

	/**
	 * More lightweight mechanism for retrieving already loaded classes or classes that are not reloadable. Works similar to
	 * Class.forName
	 * 
	 * @throws ClassNotFoundException
	 */
	Class<?> classForName(String typeName) throws ClassNotFoundException;

}
