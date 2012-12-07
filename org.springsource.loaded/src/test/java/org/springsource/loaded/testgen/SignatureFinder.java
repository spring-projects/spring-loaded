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

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.springsource.loaded.MethodMember;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypeDescriptorExtractor;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.UnableToLoadClassException;
import org.springsource.loaded.test.SpringLoadedTests;


/**
 * A class to help discover all method signatures that exist in all version of a reloadable type.
 * <p>
 * For this utility to work reloadable versions of the type must have names following the following pattern:
 * 
 * some.package.SomeClass (original name) some.package.SomeClass001 (version 1) some.package.SomeClass002 (version 2)
 * 
 * @author kdvolder
 */
public class SignatureFinder extends SpringLoadedTests {

	public SignatureFinder() throws Exception {
		setup();
	}

	/**
	 * Gathers up all method signatures in all versions of a type. Signatures are returned in the form of methodName +
	 * methodDescriptor.
	 * 
	 * @param typeName
	 * @param string
	 * @throws Exception
	 */
	public void gatherSignatures(String typeName, Set<String> sigs) {
		gatherSignatures(typeName, "", sigs);
		boolean oneSkipped = false;
		try {
			for (int i = 1; true; i++) {
				String version = String.format("%03d", i);
				try {
					gatherSignatures(typeName, version, sigs);
					oneSkipped = false;
				} catch (UnableToLoadClassException e) {
					if (oneSkipped)
						throw e;
					else
						oneSkipped = true;
				}
			}
		} catch (UnableToLoadClassException e) {
			//No more versions
		}
	}

	/**
	 * Gathers up all method signatures in a specific version
	 * 
	 * @throws Exception
	 */
	private void gatherSignatures(String typeName, String version, Set<String> sigs) {
		TypeRegistry tr = getTypeRegistry("");
		byte[] bytes = null;
		if (version.equals("")) {
			bytes = loadBytesForClass(typeName);
		} else {
			bytes = retrieveRename(typeName, typeName + version);
		}
		TypeDescriptor typeDescriptor = new TypeDescriptorExtractor(tr).extract(bytes, true);
		for (MethodMember method : typeDescriptor.getMethods()) {
			sigs.add(method.getNameAndDescriptor());
		}
	}

	/**
	 * Like gather signatures, but gathers signatures of constructors instead of methods.
	 */
	public void gatherConstructorSignatures(String typeName, Set<String> sigs) {
		gatherConstructorSignatures(typeName, "", sigs);
		boolean oneSkipped = false;
		try {
			for (int i = 1; true; i++) {
				String version = String.format("%03d", i);
				try {
					gatherConstructorSignatures(typeName, version, sigs);
					oneSkipped = false;
				} catch (UnableToLoadClassException e) {
					if (oneSkipped)
						throw e;
					else
						oneSkipped = true;
				}
			}
		} catch (UnableToLoadClassException e) {
			//No more versions
		}
	}

	private void gatherConstructorSignatures(String typeName, String version, Set<String> sigs) {
		TypeRegistry tr = getTypeRegistry("");
		byte[] bytes = null;
		if (version.equals("")) {
			bytes = loadBytesForClass(typeName);
		} else {
			bytes = retrieveRename(typeName, typeName + version);
		}
		TypeDescriptor typeDescriptor = new TypeDescriptorExtractor(tr).extract(bytes, true);
		for (MethodMember method : typeDescriptor.getConstructors()) {
			sigs.add(method.getDescriptor());
		}
	}

	//This class is more of a utility class, but since is already subclassed from SpringLoadedTests, we might
	// as well have its own tests embedded in it.

	@Test
	public void simpleTest() {
		Set<String> sigs = new HashSet<String>();
		gatherSignatures("reflection.targets.SimpleClass", sigs);

		for (String string : sigs) {
			System.out.println(string);
		}

		//Got some catchers?
		Assert.assertTrue(sigs.contains("hashCode()I"));
		Assert.assertTrue(sigs.contains("toString()Ljava/lang/String;"));

		//Got methods from the original version?
		Assert.assertTrue(sigs.contains("method(Ljava/lang/String;)V"));
		Assert.assertTrue(sigs.contains("method()I"));

		//Got method from v002?
		Assert.assertTrue(sigs.contains("added(Lreflection/targets/SimpleClass;)V"));
	}

	//TODO: [...] add tests for gatherConstructorsignatures

}
