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
package org.springsource.loaded.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springsource.loaded.ClassRenamer;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.test.infra.Result;


public class CodeGenerationTests extends SpringLoadedTests {

	TypeRegistry typeRegistry;

	@Before
	public void init() {
		typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		configureForTesting(typeRegistry, "codegen..*");
	}

	@Test
	public void simpleClass() {
		byte[] bs = loadBytesForClass("codegen.Simple");
		ReloadableType simpleClass = typeRegistry.addType("codegen.Simple", bs);
		assertNotNull(simpleClass);
		// just checking no crash for loading it!
	}

	//	@Test
	//	public void shapeOfLoadedType() {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		configureForTesting(typeRegistry, "data.SimpleClass");
	//
	//		byte[] bs = retrieveClass("data.SimpleClass");
	//		byte[] newbs = MethodExecutionRewriter.rewrite(simpleClass, bs);
	//		//		ClassPrinter.print(newbs);
	//	}
	//
	//	@Test
	//	public void shapeOfCallingType() {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		configureForTesting(typeRegistry, "data.SimpleClass");
	//
	//		byte[] bs = retrieveClass("data.SimpleClass");
	//		ReloadableType simpleClass = typeRegistry.recordType("data.SimpleClass", bs);
	//		byte[] newbs = MethodExecutionRewriter.rewrite(simpleClass, bs);
	//		byte[] caller = retrieveClass("data.SimpleClassCaller");
	//
	//		byte[] rewrittenBytes = MethodCallAndFieldAccessRewriter.rewrite(typeRegistry, caller);
	//		ClassPrinter.print(rewrittenBytes);

	//	}

	//	@Test
	//	public void shapeOfExtractedInterface() {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//		configureForTesting(typeRegistry, "data.TopType");
	//
	//		byte[] bs = retrieveClass("data.TopType");
	//		TypeDescriptor td = TypeDescriptorExtractor.extractFor(bs);
	//
	//		NewVersionTypeDescriptor nvtd = new NewVersionTypeDescriptor(td, td);
	//
	//		ReloadableType simpleClass = typeRegistry.recordType("data.TopType", bs);
	//		byte[] newbs = MethodExecutionRewriter.rewrite(simpleClass, bs);
	//		byte[] dispatcher = DispatcherBuilder.createFor(simpleClass, nvtd, "1");
	//		//		ClassPrinter.print(dispatcher);
	//		ClassPrinter.print(ExecutorCreator.createFor(simpleClass, bs));
	//		//		ExtractedInterface ibs = InterfaceExtractor.extract(bs);
	//		//		ClassPrinter.print(ibs.bytes);
	//		//		//		 CLASS: data/TopType$I v50 0x0601(public abstract interface) super java/lang/Object interfaces
	//		//		//		 METHOD: 0x0401(public abstract) methodOne(Ldata/TopType;[Ljava/lang/String;)I 
	//		//		//		 METHOD: 0x0401(public abstract) methodTwo(Ldata/TopType;I)Ljava/lang/String; 
	//		//		//		 METHOD: 0x0401(public abstract) s$execute(Ldata/TopType;[Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object; 
	//		//		ClassPrinter.print(simpleClass.getLatestDispatcherBytes());
	//	}

	//
	//	// more cases to think about - abstract methods in abstract classes, being filled in (made non-abstract) or removed
	//	// interfaces - adding new methods, removing existing methods

	//	@Test
	//	public void superCalls1() throws Exception {
	//		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
	//
	//		String top = "supercalls.Super";
	//		String bottom = "supercalls.Sub";
	//		String runner = "supercalls.Runner";
	//		configureForTesting(typeRegistry, top + "," + bottom);
	//
	//		ReloadableType reloadableSuper = typeRegistry.recordType(top, retrieveClass(top));
	//		ReloadableType reloadableSub = typeRegistry.recordType(bottom, retrieveClass(bottom));
	//
	//		Class<?> callerClazz = loadit(runner, retrieveClass(runner));
	//		Result result = null;
	//
	//		result = runUnguarded(callerClazz, "runSubMethod");
	//		Assert.assertEquals(42, result.returnValue);
	//
	//		reloadableSub.loadNewVersion("002", retrieveRename(bottom, bottom + "002"));
	//
	//		ClassPrinter.print("new sub executor", reloadableSub.getLatestExecutorBytes());
	//
	//		result = runUnguarded(callerClazz, "runSubMethod");
	//		Assert.assertEquals(42, result.returnValue);
	//
	//	}

	/**
	 * Testing that when we load a rewritable interface and its implementation, the dynamic dispatch method is created.
	 */
	@Test
	public void testInterfaceWriting() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);
		String theInterface = "interfacerewriting.TheInterface";
		String theImpl = "interfacerewriting.TheImpl";
		configureForTesting(typeRegistry, theInterface + "," + theImpl);
		//		ReloadableType rInterface = 
		typeRegistry.addType(theInterface, loadBytesForClass(theInterface));
		//		ClassPrinter.print(rInterface.bytesLoaded);
		//		ReloadableType rImpl =
		typeRegistry.addType(theImpl, loadBytesForClass(theImpl));
	}

	/**
	 * Like the previous test but now we invoke a new method on the interface.
	 */
	@Test
	public void testInterfaceWriting2() throws Exception {
		TypeRegistry typeRegistry = getTypeRegistry("");

		String theInterface = "interfacerewriting.TheInterface";
		String theImpl = "interfacerewriting.TheImpl";
		String runner = "interfacerewriting.TheRunner";

		configureForTesting(typeRegistry, theInterface + "," + theImpl + "," + runner);
		ReloadableType rInterface = typeRegistry.addType(theInterface, loadBytesForClass(theInterface));
		ReloadableType rImpl = typeRegistry.addType(theImpl, loadBytesForClass(theImpl));
		ReloadableType rRunner = typeRegistry.addType(runner, loadBytesForClass(runner));

		String[] retargets = new String[] { theInterface + "002:" + theInterface, theImpl + "002:" + theImpl,
				runner + "002:" + runner };

		Result result = runUnguarded(rRunner.getClazz(), "run");
		assertNull(result.returnValue);

		rInterface.loadNewVersion("002", ClassRenamer.rename(theInterface, loadBytesForClass(theInterface + "002"), retargets));
		rImpl.loadNewVersion("002", ClassRenamer.rename(theImpl, loadBytesForClass(theImpl + "002"), retargets));
		rRunner.loadNewVersion("002", ClassRenamer.rename(runner, loadBytesForClass(runner + "002"), retargets));

		result = runUnguarded(rRunner.getClazz(), "run");
		assertEquals("abc", result.returnValue);
	}

	@Test
	public void interfaceCalls() throws Exception {
		TypeRegistry typeRegistry = TypeRegistry.getTypeRegistryFor(binLoader);

		String theInterface = "interfaces.TheInterface";
		String theImplementation = "interfaces.TheImplementation";
		String theRunner = "interfaces.Runner";
		configureForTesting(typeRegistry, theInterface + "," + theImplementation);

		ReloadableType rInterface = typeRegistry.addType(theInterface, loadBytesForClass(theInterface));
		ReloadableType rImpl = typeRegistry.addType(theImplementation, loadBytesForClass(theImplementation));

		Class<?> theRunnerClazz = loadClass(theRunner);

		Assert.assertEquals(35, runUnguarded(theRunnerClazz, "runGetValue").returnValue);

		// Just load a new version of the getValue() method and see if it is picked up:
		loadNewVersion(rImpl, 2);
		Assert.assertEquals(23, runUnguarded(theRunnerClazz, "runGetValue").returnValue);
		// Now a new version with toString()
		loadNewVersion(rImpl, 3);
		Assert.assertEquals("i am version 3", runUnguarded(theRunnerClazz, "runToString").returnValue);

		// Now change the interface - add a method
		//		loadNewVersion(rInterface, 4);
		//		loadNewVersion(rImpl, 4);

		byte[] impl4 = ClassRenamer.rename("interfaces.TheImplementation", loadBytesForClass("interfaces.TheImplementation004"),
				"interfaces.TheInterface004:interfaces.TheInterface");
		byte[] interface4 = ClassRenamer.rename("interfaces.TheInterface", loadBytesForClass("interfaces.TheInterface004"),
				"interfaces.TheInterface004:interfaces.TheInterface");
		rImpl.loadNewVersion("004", impl4);
		rInterface.loadNewVersion("004", interface4);
		//			ClassPrinter.print(rImpl.getLatestExecutorBytes());
		Assert.assertEquals("oranges", runUnguarded(theRunnerClazz, "doit").returnValue);

		//		reloadableSub.loadNewVersion("002", retrieveRename(bottom, bottom + "002"));
		//		ClassPrinter.print("new sub executor", reloadableSub.getLatestExecutorBytes());
		//		result = runUnguarded(theRunnerClazz, "runSubMethod");
		//		Assert.assertEquals(42, result.returnValue);
	}

	private void loadNewVersion(ReloadableType rtype, int version) {
		String v = "000" + version;
		v = v.substring(v.length() - 3);
		rtype.loadNewVersion(v, retrieveRename(rtype.getName(), rtype.getName() + v));
	}
}
