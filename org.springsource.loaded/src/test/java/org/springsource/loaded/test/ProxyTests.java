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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.agent.SpringLoadedPreProcessor;
import org.springsource.loaded.test.infra.Result;


/**
 * Testing the automatic reloading of proxies when interfaces change.
 * 
 * !!!THESE TESTS MUST BE RUN WITH -JAVAAGENT ON!!!
 * 
 * @author Andy Clement
 * @since 0.8.3
 */
public class ProxyTests extends SpringLoadedTests {

	ClassLoader oldCcl;

	@Before
	public void setup() throws Exception {
		super.setup();
		SpringLoadedPreProcessor.disabled = false;
		oldCcl = Thread.currentThread().getContextClassLoader();
	}

	@After
	public void teardown() throws Exception {
		super.teardown();
		SpringLoadedPreProcessor.disabled = true;
		Thread.currentThread().setContextClassLoader(oldCcl);
	}

	/*
	 * Some notes on proxies and reloading.  A proxy is created that implements some interface - the proxy
	 * generator will ask those interfaces what methods they have and for each one: create a field to
	 * hold the method object in the proxy, create an entry in the static initializer to initialize
	 * the method object, create a method in the proxy that will call the invocation handler to run it.
	 * 
	 * If the interfaces are modified to add new methods or delete existing ones, the proxy will
	 * be out of date (it will be missing the method field, the method itself that forwards to the
	 * invocation handler and the initialization logic).  If we attempt to regenerate the proxy, the proxy creation
	 * code will give us back the old one because it cached the one it had created!
	 */

	/*
	 * Some snippets from a proxy:
	 * CLASS: $Proxy61 v49 0x0031(public final synchronized) super java/lang/reflect/Proxy interfacescom/test/jaxb/HomeController org/springframework/aop/SpringProxy org/springframework/aop/framework/Advised 
	FIELD 0x000a(private static) m17 Ljava/lang/reflect/Method;
	FIELD 0x000a(private static) m12 Ljava/lang/reflect/Method;
	FIELD 0x000a(private static) m6 Ljava/lang/reflect/Method;
	
	METHOD: 0x0008(static) <clinit>()V
	CODE
	L0
	LDC org.springframework.aop.framework.Advised
	INVOKESTATIC java/lang/Class.forName(Ljava/lang/String;)Ljava/lang/Class;
	LDC isPreFiltered
	ICONST_0
	ANEWARRAY java/lang/Class
	INVOKEVIRTUAL java/lang/Class.getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
	PUTSTATIC $Proxy61.m17 Ljava/lang/reflect/Method;
	LDC org.springframework.aop.framework.Advised
	INVOKESTATIC java/lang/Class.forName(Ljava/lang/String;)Ljava/lang/Class;
	LDC isProxyTargetClass
	ICONST_0
	ANEWARRAY java/lang/Class
	INVOKEVIRTUAL java/lang/Class.getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
	PUTSTATIC $Proxy61.m12 Ljava/lang/reflect/Method;
	
	METHOD: 0x0011(public final) hashCode()I
	CODE
	L0
	ALOAD 0
	GETFIELD java/lang/reflect/Proxy.h Ljava/lang/reflect/InvocationHandler;
	ALOAD 0
	GETSTATIC $Proxy61.m0 Ljava/lang/reflect/Method;
	ACONST_NULL
	INVOKEINTERFACE java/lang/reflect/InvocationHandler.invoke(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;
	CHECKCAST java/lang/Integer
	INVOKEVIRTUAL java/lang/Integer.intValue()I
	IRETURN
	L1
	ATHROW
	L2
	ASTORE 1
	NEW java/lang/reflect/UndeclaredThrowableException
	DUP
	ALOAD 1
	INVOKESPECIAL java/lang/reflect/UndeclaredThrowableException.<init>(Ljava/lang/Throwable;)V
	ATHROW
	 */

	// To run these tests you need to have -javaagent specified
	@Ignore
	@Test
	public void basicCaseSimpleInterface() throws Exception {

		// Set so that the Proxy generator can see the interface class
		Thread.currentThread().setContextClassLoader(binLoader);

		Class<?> clazz = Class.forName("proxy.TestA1", false, binLoader);
		Result r = runUnguarded(clazz, "createProxy");
		Class<?> clazzForInterface = Class.forName("proxy.TestIntfaceA1", false, binLoader);

		// Call a method through the proxy
		r = runUnguarded(clazz, "runM");
		assertContains("TestInvocationHandler1.invoke() for m", r.stdout);

		TypeRegistry tr = TypeRegistry.getTypeRegistryFor(binLoader);
		assertNotNull(tr);
		ReloadableType rt = tr.getReloadableType(clazzForInterface);
		assertNotNull(rt);

		// new version adds a method called n
		byte[] newVersionOfTestInterfaceA1 = retrieveRename("proxy.TestIntfaceA1", "proxy.TestIntfaceA2");
		rt.loadNewVersion(newVersionOfTestInterfaceA1);

		// running m() should still work
		r = runUnguarded(clazz, "runM");
		assertContains("TestInvocationHandler1.invoke() for m", r.stdout);

		// Now load new version of proxy.TestA1 that will enable us to call n on the new interface
		byte[] newVersionOfTestA2 = retrieveRename("proxy.TestA1", "proxy.TestA2", "proxy.TestIntfaceA2:proxy.TestIntfaceA1");
		tr.getReloadableType(clazz).loadNewVersion(newVersionOfTestA2);

		// running m() should still work
		r = runUnguarded(clazz, "runM");
		assertContains("TestInvocationHandler1.invoke() for m", r.stdout);

		// running n() should now work! (if the proxy was auto regen/reloaded)
		r = runUnguarded(clazz, "runN");
		assertContains("TestInvocationHandler1.invoke() for n", r.stdout);
	}

	/**
	 * For non public interfaces the proxies are generated in the same package as the interface.
	 */
	// To run these tests you need to have -javaagent specified
	@Ignore
	@Test
	public void xnonPublicInterface() throws Exception {

		// Set so that the Proxy generator can see the interface class
		Thread.currentThread().setContextClassLoader(binLoader);

		Class<?> clazz = Class.forName("proxy.two.TestA1", false, binLoader);
		Result r = runUnguarded(clazz, "createProxy");
		Class<?> clazzForInterface = Class.forName("proxy.two.TestIntfaceA1", false, binLoader);

		// Call a method through the proxy
		r = runUnguarded(clazz, "runM");
		assertContains("TestInvocationHandler1.invoke() for m", r.stdout);

		TypeRegistry tr = TypeRegistry.getTypeRegistryFor(binLoader);
		assertNotNull(tr);
		ReloadableType rt = tr.getReloadableType(clazzForInterface);
		assertNotNull(rt);

		// new version adds a method called n
		byte[] newVersionOfTestInterfaceA1 = retrieveRename("proxy.two.TestIntfaceA1", "proxy.two.TestIntfaceA2");
		rt.loadNewVersion(newVersionOfTestInterfaceA1);

		// running m() should still work
		r = runUnguarded(clazz, "runM");
		assertContains("TestInvocationHandler1.invoke() for m", r.stdout);

		// Now load new version of proxy.TestA1 that will enable us to call n on the new interface
		byte[] newVersionOfTestA2 = retrieveRename("proxy.two.TestA1", "proxy.two.TestA2",
				"proxy.two.TestIntfaceA2:proxy.two.TestIntfaceA1");
		tr.getReloadableType(clazz).loadNewVersion(newVersionOfTestA2);

		// running m() should still work
		r = runUnguarded(clazz, "runM");
		assertContains("TestInvocationHandler1.invoke() for m", r.stdout);

		// running n() should now work! (if the proxy was auto regen/reloaded)
		r = runUnguarded(clazz, "runN");
		assertContains("TestInvocationHandler1.invoke() for n", r.stdout);

		Set<ReloadableType> proxies = tr.getJDKProxiesFor("proxy/two/TestIntfaceA1");
		assertFalse(proxies.isEmpty());
		ReloadableType proxyRT = proxies.iterator().next();
		assertStartsWith("proxy.two.", proxyRT.getName());
	}

	/**
	 * Proxying with multiple interfaces, changed independently.
	 */
	// To run these tests you need to have -javaagent specified
	@Ignore
	@Test
	public void xmultipleInterfaces() throws Exception {

		// Set so that the Proxy generator can see the interface class
		Thread.currentThread().setContextClassLoader(binLoader);

		Class<?> clazz = Class.forName("proxy.three.TestA1", false, binLoader);
		runUnguarded(clazz, "createProxy");
		Class<?> clazzForInterface = Class.forName("proxy.three.TestIntfaceA1", false, binLoader);
		Class<?> clazzForInterfaceB1 = Class.forName("proxy.three.TestIntfaceB1", false, binLoader);

		// Call a method through the proxy
		assertContains("TestInvocationHandler1.invoke() for ma", runUnguarded(clazz, "runMA").stdout);

		TypeRegistry tr = TypeRegistry.getTypeRegistryFor(binLoader);
		assertNotNull(tr);
		ReloadableType rt = tr.getReloadableType(clazzForInterface);
		assertNotNull(rt);
		ReloadableType rt2 = tr.getReloadableType(clazzForInterfaceB1);
		assertNotNull(rt2);

		// new version adds a method called na
		byte[] newVersionOfTestInterfaceA1 = retrieveRename("proxy.three.TestIntfaceA1", "proxy.three.TestIntfaceA2");
		rt.loadNewVersion(newVersionOfTestInterfaceA1);

		// running m() should still work
		assertContains("TestInvocationHandler1.invoke() for ma", runUnguarded(clazz, "runMA").stdout);

		// Now load new version of proxy.TestA1 that will enable us to call n on the new interface
		byte[] newVersionOfTestA2 = retrieveRename("proxy.three.TestA1", "proxy.three.TestA2",
				"proxy.three.TestIntfaceA2:proxy.three.TestIntfaceA1", "proxy.three.TestIntfaceB2:proxy.three.TestIntfaceB1");
		tr.getReloadableType(clazz).loadNewVersion(newVersionOfTestA2);

		// running ma() should still work
		assertContains("TestInvocationHandler1.invoke() for ma", runUnguarded(clazz, "runMA").stdout);

		// running na() should now work! (if the proxy was auto regen/reloaded)
		assertContains("TestInvocationHandler1.invoke() for na", runUnguarded(clazz, "runNA").stdout);

		// should be OK - mb() was in from the start
		assertContains("TestInvocationHandler1.invoke() for mb", runUnguarded(clazz, "runMB").stdout);

		// TestIntfaceB1 hasn't been reloaded yet, nb() isnt on the interface (nor proxy)
		try {
			runUnguarded(clazz, "runNB");
			fail();
		} catch (InvocationTargetException re) {
			assertTrue(re.getCause() instanceof NoSuchMethodError);
			assertEquals("proxy.three.TestIntfaceB1.nb()V", re.getCause().getMessage());
		}

		// new version adds a method called nb
		byte[] newVersionOfTestInterfaceB1 = retrieveRename("proxy.three.TestIntfaceB1", "proxy.three.TestIntfaceB2");
		rt2.loadNewVersion("3", newVersionOfTestInterfaceB1);

		// running nb() should now work! (if the proxy was auto regen/reloaded)
		assertContains("TestInvocationHandler1.invoke() for nb", runUnguarded(clazz, "runNB").stdout);

		Set<ReloadableType> proxies = tr.getJDKProxiesFor("proxy/three/TestIntfaceA1");
		assertFalse(proxies.isEmpty());
		ReloadableType proxyRT = proxies.iterator().next();
		assertStartsWith("proxy.three.", proxyRT.getName());
	}
}
