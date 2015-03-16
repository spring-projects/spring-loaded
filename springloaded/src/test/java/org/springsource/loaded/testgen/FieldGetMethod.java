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

/**
 * Some tests may need to parameterize over different ways we can obtain field references.
 * 
 * @author Kris De Volder
 */
public enum FieldGetMethod {

	GET_DECLARED_FIELD, GET_DECLARED_FIELDS, GET_FIELD, GET_FIELDS

}
