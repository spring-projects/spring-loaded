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
 * Interface implemented by all dispatchers so code can be generated to call the dynamic executor regardless of the
 * dispatcher instance the code is actually working with. The method name here lines up with that defined in Constants -
 * see mDynamicDispatchName.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public interface __DynamicallyDispatchable {

	Object __execute(Object[] parameters, Object instance, String nameAndDescriptor);

}
