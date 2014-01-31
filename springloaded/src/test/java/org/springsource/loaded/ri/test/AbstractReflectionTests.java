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
package org.springsource.loaded.ri.test;

import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.springsource.loaded.MethodInvokerRewriter;
import org.springsource.loaded.ReloadException;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.ri.ReflectiveInterceptor;
import org.springsource.loaded.test.SpringLoadedTests;
import org.springsource.loaded.test.infra.ResultException;


/**
 * Abstract root test class containing helper functions. Used mainly by tests for the reflection API.
 * 
 * @author kdvolder
 */
public class AbstractReflectionTests extends SpringLoadedTests {

	public static Object newInstance(Class<?> clazz) throws IllegalArgumentException, SecurityException, InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return ReflectiveInterceptor.jlClassNewInstance(clazz);
	}

	@Override
	protected void reload(ReloadableType reloadableType, String versionstamp) {
		throw new Error("Shouldn't call this, call 'reloadType' instead.");
	}

	@Override
	public void setup() throws Exception {
		super.setup();
	}

	protected ReloadableType reloadableClass(String className) {
		return registry.addType(className, loadBytesForClass(className));
	}

	protected Class<?> nonReloadableClass(String className) {
		byte[] rewrittenBytes = MethodInvokerRewriter.rewrite(registry, loadBytesForClass(className));
		return loadit(className, rewrittenBytes);
	}

	protected void reloadType(ReloadableType target, String version) {
		String targetClassName = target.getClazz().getName();
		Assert.assertTrue("Reloading of " + target + " failed.",
				target.loadNewVersion(version, retrieveRename(targetClassName, targetClassName + version)));
	}

	/**
	 * NoSuchMethodException should be thrown when trying to call a Method object when its modifiers forbid it (and access flag is
	 * not set)
	 * 
	 * @param expectMsg
	 */
	protected void assertIllegalAccess(String expectMsg, ResultException e) {
		Throwable cause = e.getDeepestCause();
		Assert.assertTrue(cause instanceof IllegalAccessException);
		Assert.assertEquals(expectMsg, cause.getMessage());
	}

	public void reloadTypeShouldFail(ReloadableType targetClass, String version, String expectedErrorMessage) {
		try {
			reloadType(targetClass, version);
			fail("An error: '" + expectedErrorMessage + "' was expected.");
		} catch (ReloadException e) {
			assertContains(expectedErrorMessage, e.getMessage());
		}
	}

}
