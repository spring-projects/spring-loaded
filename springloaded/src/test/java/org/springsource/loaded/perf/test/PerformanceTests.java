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

package org.springsource.loaded.perf.test;

import org.springsource.loaded.test.SpringLoadedTests;

/**
 * Check the performance of reloaded code.
 * 
 * @author Andy Clement
 * @since 0.8.3
 */
public class PerformanceTests extends SpringLoadedTests {

	/**
	 * A few invokevirtuals in the same class, some inside a loop, what is the damage? Check:
	 * <ul>
	 * <li>the case when nothing reloaded
	 * <li>the case when the type itself is reloaded
	 * <li>the case when an unrelated type is reloaded
	 * </ul>
	 */
	//	@Test
	//	public void testInvokeVirtual() throws Exception {
	// TypeRegistry.resetConfiguration(); // TODO into setup
	//		binLoader = new TestClassloaderWithRewriting("", false);
	//		String caller = "perf.one.Caller";
	//		String target = "perf.one.Target";
	//
	//		TypeRegistry r = getTypeRegistry(caller + "," + target);
	//
	//		ReloadableType rtype = r.addType(caller, loadBytesForClass(caller));
	//		ReloadableType rtypeTarget = r.addType(target, loadBytesForClass(target));
	//
	//		//		ClassPrinter.print(rtype.bytesLoaded);
	//		Object callerInstance = rtype.getClazz().newInstance();
	//
	//		// when run directly as a Java app (no reloading involved)
	//		//13.816ms
	//		//12.657ms
	//		//11.509ms
	//		//11.833ms
	//		//11.788ms
	//
	//		// 16-Jul
	//		//		3013.712ms
	//		//		3016.476ms
	//		//		2996.665ms
	//		//		2947.038ms
	//		//		3216.198ms
	//		//		3213.829ms
	//
	//		// 2nd run
	//		//		2277.763ms
	//		//		2402.249ms
	//		//		2308.843ms
	//		//		2265.576ms
	//		//		2278.301ms
	//		//		2277.452ms
	//
	//		// remove trace logic from start and end of ivicheck (just looking at globalflag and tracing is OFF)
	//		//		2356.014ms
	//		//		2367.017ms
	//		//		2361.576ms
	//		//		2800.522ms
	//		//		2404.417ms
	//		//		2355.538ms
	//
	//		// added 'nothingReloaded' global static to TypeRegistry, checked in ivicheck and instanceFieldInterceptionRequired
	//		//		101.68ms
	//		//		112.616ms
	//		//		104.583ms
	//		//		103.981ms
	//		//		103.509ms
	//		//		103.949ms
	//
	//		//125.758ms
	//		//120.896ms
	//		//122.741ms
	//		//118.937ms
	//		//113.751ms
	//		//112.599ms
	//
	//		ClassPrinter.print(rtypeTarget.bytesLoaded);
	//		// warmup
	//		runOnInstance(rtype.getClazz(), callerInstance, "warmup");
	//		System.out.println("warmup complete");
	//		result = runOnInstance(rtype.getClazz(), callerInstance, "execute");
	//		System.out.println(result.stdout);
	//		result = runOnInstance(rtype.getClazz(), callerInstance, "execute");
	//		System.out.println(result.stdout);
	//		result = runOnInstance(rtype.getClazz(), callerInstance, "execute");
	//		System.out.println(result.stdout);
	//		result = runOnInstance(rtype.getClazz(), callerInstance, "execute");
	//		System.out.println(result.stdout);
	//		result = runOnInstance(rtype.getClazz(), callerInstance, "execute");
	//		System.out.println(result.stdout);
	//		result = runOnInstance(rtype.getClazz(), callerInstance, "execute");
	//		System.out.println(result.stdout);
	//	}

	// optimizing the generated code.  Here is something for 'j=5':
	//	  ALOAD 0
	//    ICONST_5
	//    LDC 1
	//    LDC j
	//    INVOKESTATIC org/springsource/loaded/TypeRegistry.instanceFieldInterceptionRequired(ILjava/lang/String;)Z
	//    IFEQ L6
	//    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
	//    SWAP
	//    DUP_X1
	//    LDC j
	//    INVOKESPECIAL perf/one/Target.r$set(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V
	//    GOTO L7
	// L6
	//    PUTFIELD perf/one/Target.j I
	// L7
	// observations: 
	// - 'LDC 1' 'LDC j' could be combined, just use an index (convert j to a numeric)
	// - shame about boxing call, can we have set variant for int? (is it going to bloat the code too much? Can't recall - does only the topmost get these methods?)

	// TODO [perf] brainwave: keep a copy of the original method that runs with no checks for the situation where nothing reloaded?
	// or possibly where we know nothing it refers to has changed (much more difficult)

	// 20-Jul: Here is the entire code for foo() method (the one called in a tight loop from the caller):
	//METHOD: 0x0001(public) foo()V
	//    CODE

	// >>> code to check if this method has changed	
	//    GETSTATIC perf/one/Target.r$type Lorg/springsource/loaded/ReloadableType;
	//    LDC 0
	//    INVOKEVIRTUAL org/springsource/loaded/ReloadableType.changed(I)I
	//    DUP
	//    IFEQ L0
	//    ICONST_1
	//    IF_ICMPEQ L1
	//    NEW java/lang/NoSuchMethodError
	//    DUP
	//    LDC perf.one.Target.foo()V
	//    INVOKESPECIAL java/lang/NoSuchMethodError.<init>(Ljava/lang/String;)V
	//    ATHROW
	// L1
	//    GETSTATIC perf/one/Target.r$type Lorg/springsource/loaded/ReloadableType;
	//    INVOKEVIRTUAL org/springsource/loaded/ReloadableType.fetchLatest()Ljava/lang/Object;
	//    CHECKCAST perf/one/Target__I
	//    ALOAD 0
	//    INVOKEINTERFACE perf/one/Target__I.foo(Lperf/one/Target;)V
	//    RETURN
	// L0
	//    POP
	// <<< code to check if this method has changed

	// L2
	//    ICONST_0
	//    ISTORE 1
	// L3
	//    GOTO L4
	// L5
	//    ALOAD 0
	//    ICONST_5
	//    LDC 1
	//    LDC j
	//    INVOKESTATIC org/springsource/loaded/TypeRegistry.instanceFieldInterceptionRequired(ILjava/lang/String;)Z
	//    IFEQ L6
	//    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
	//    SWAP
	//    DUP_X1
	//    LDC j
	//    INVOKESPECIAL perf/one/Target.r$set(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V
	//    GOTO L7
	// L6
	//    PUTFIELD perf/one/Target.j I
	// L7
	// L4
	//    ILOAD 1
	//    BIPUSH 100
	//    IF_ICMPLT L5
	// L8
	//    RETURN
	// L9
}
