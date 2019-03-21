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

package org.springsource.loaded.test;


import org.junit.Assert;
import org.junit.Test;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;


/**
 * Tests real scenarios, simulating what would happen at runtime for a JVM with the agent attached.
 * 
 * @author Andy Clement
 */
public class ScenarioTests extends SpringLoadedTests {

	// TODO either flesh out these scenarios or delete this class
	/**
	 * A class is run that returns a value. The class is modified and then called again - this time the new value should
	 * be returned.
	 */
	public void scenarioOne_methodBodyChange() {
		// actions:
		// agent starts, intercepts loading of classes in the domain of interest
		// (triggered by a property config file)
		// as well as instrumenting the affected type, it also instruments
		// callers of those types
	}

	// TODO More scenarios

	// new method
	// delete method
	// new parameter
	// exceptions thrown
	// exceptions added
	// exceptions removed
	// varargs method
	// generics
	// autoboxing

	// field type changed
	// field added
	// field removed

	@Test
	public void scenarioA() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("data.ScenarioA");
		ReloadableType scenarioA = typeRegistry.addType("data.ScenarioA", loadBytesForClass("data.ScenarioA"));

		Class<?> scenarioClazz = scenarioA.getClazz();
		Object o = scenarioClazz.newInstance();

		// foo() returns a string
		String result = (String) runOnInstance(scenarioClazz, o, "foo").returnValue;
		Assert.assertEquals("from ScenarioA", result);

		// new version of foo() returns different string
		scenarioA.loadNewVersion("002", retrieveRename("data.ScenarioA", "data.ScenarioA002"));
		result = (String) runOnInstance(scenarioClazz, o, "foo").returnValue;
		Assert.assertEquals("from ScenarioA 002", result);

		// new version of foo() calls a method to discover the string to return
		scenarioA.loadNewVersion("003", retrieveRename("data.ScenarioA", "data.ScenarioA003"));
		result = (String) runOnInstance(scenarioClazz, o, "foo").returnValue;
		Assert.assertEquals("from ScenarioA 003", result);

		scenarioA.loadNewVersion("004", retrieveRename("data.ScenarioA", "data.ScenarioA004"));
		result = (String) runOnInstance(scenarioClazz, o, "foo").returnValue;
		Assert.assertEquals("from ScenarioA 004", result);

		result = (String) runOnInstance(scenarioClazz, o, "getName").returnValue;
		Assert.assertEquals("004", result);
	}

	/**
	 * Scenario: A method is being discovered through reflection (getDeclaredMethods()). The method does not exist
	 * initially but is introduced later. Once found, an attempt is made to access annotations on this method (through
	 * getDeclaredAnnotations()) - these annotations do not exist initially but are then added.
	 */
	@Test
	public void scenarioB_methodReplacement() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		// Configure it directly such that data.Apple is considered reloadable
		configureForTesting(typeRegistry, "data..*");

		ReloadableType scenarioB = typeRegistry.addType("data.ScenarioB", loadBytesForClass("data.ScenarioB"));

		Class<?> scenarioClazz = scenarioB.getClazz();
		Object o = scenarioClazz.newInstance();

		String result = (String) runOnInstance(scenarioClazz, o, "methodAccessor").returnValue;
		Assert.assertEquals("method not found", result);

		result = (String) runOnInstance(scenarioClazz, o, "methodAccessor").returnValue;
		Assert.assertEquals("method not found", result);

		// load the version defining the method
		scenarioB.loadNewVersion("002", retrieveRename("data.ScenarioB", "data.ScenarioB002"));

		result = (String) runOnInstance(scenarioClazz, o, "methodAccessor").returnValue;
		Assert.assertEquals("method found", result);

		result = (String) runOnInstance(scenarioClazz, o, "methodAccessor").returnValue;
		Assert.assertEquals("method found", result);

		// now check for annotations
		result = (String) runOnInstance(scenarioClazz, o, "annoAccessor").returnValue;
		Assert.assertEquals("no annotations", result);

		result = (String) runOnInstance(scenarioClazz, o, "annoAccessor").returnValue;
		Assert.assertEquals("no annotations", result);

		// load the version where the method is annotated
		scenarioB.loadNewVersion("003", retrieveRename("data.ScenarioB", "data.ScenarioB003"));

		result = (String) runOnInstance(scenarioClazz, o, "annoAccessor").returnValue;
		Assert.assertEquals("found @data.Wiggle(value=default)", result);

		result = (String) runOnInstance(scenarioClazz, o, "annoAccessor").returnValue;
		Assert.assertEquals("found @data.Wiggle(value=default)", result);

	}

}
