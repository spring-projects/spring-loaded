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

import org.junit.Test;
import org.springsource.loaded.InterfaceExtractor;
import org.springsource.loaded.TypeDescriptor;
import org.springsource.loaded.TypeDescriptorExtractor;
import org.springsource.loaded.TypeRegistry;


/**
 * Tests for interface extraction.
 * 
 * @author Andy Clement
 * @since 1.0
 */
public class InterfaceExtractorTest extends SpringLoadedTests {

	/**
	 * Attempt simple interface extraction for a class with one void no-arg method
	 */
	@Test
	public void simpleExtraction() {
		TypeRegistry registry = getTypeRegistry(null);
		byte[] classBytes = loadBytesForClass("data.SimpleClass");
		TypeDescriptor td = new TypeDescriptorExtractor(registry).extract(classBytes, true);
		// @formatter:off
		checkType(classBytes,
				"CLASS: data/SimpleClass v50 0x0020(synchronized) super java/lang/Object\n" +
						"SOURCE: SimpleClass.java null\n" +
						"METHOD: 0x0000() <init>()V\n" +
						"    CODE\n" +
						" L0\n" +
						"    ALOAD 0\n" +
						"    INVOKESPECIAL java/lang/Object.<init>()V\n" +
						"    RETURN\n" +
						" L1\n" +
						"METHOD: 0x0001(public) foo()V\n" +
						"    CODE\n" +
						" L0\n" +
						"    RETURN\n" +
						" L1\n" +
						"\n");
		// @formatter:on
		byte[] bytes = InterfaceExtractor.extract(classBytes, registry, td);
		// @formatter:off
		checkType(
				bytes,
				"CLASS: data/SimpleClass__I v50 0x0601(public abstract interface) super java/lang/Object\n"
						+
						"METHOD: 0x0401(public abstract) ___init___(Ldata/SimpleClass;)V\n"
						+
						"METHOD: 0x0401(public abstract) foo(Ldata/SimpleClass;)V\n"
						+
						"METHOD: 0x0401(public abstract) __execute([Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;\n"
						+
						"METHOD: 0x0401(public abstract) ___clinit___()V\n"
						+
						"METHOD: 0x0401(public abstract) hashCode(Ldata/SimpleClass;)I\n"
						+
						"METHOD: 0x0401(public abstract) equals(Ldata/SimpleClass;Ljava/lang/Object;)Z\n"
						+
						"METHOD: 0x0401(public abstract) clone(Ldata/SimpleClass;)Ljava/lang/Object; java/lang/CloneNotSupportedException\n"
						+
						"METHOD: 0x0401(public abstract) toString(Ldata/SimpleClass;)Ljava/lang/String;\n" +
						"\n");
		// @formatter:on
	}

	@Test
	public void varietyOfMethods() {
		TypeRegistry registry = getTypeRegistry(null);
		byte[] classBytes = loadBytesForClass("data.SimpleClassFour");
		TypeDescriptor td = new TypeDescriptorExtractor(registry).extract(classBytes, true);
		byte[] bytes = InterfaceExtractor.extract(classBytes, registry, td);
		// @formatter:off
		checkType(
				bytes,
				"CLASS: data/SimpleClassFour__I v50 0x0601(public abstract interface) super java/lang/Object\n"
						+
						"METHOD: 0x0401(public abstract) ___init___(Ldata/SimpleClassFour;I)V\n"
						+
						"METHOD: 0x0401(public abstract) ___init___(Ldata/SimpleClassFour;Ljava/lang/String;)V\n"
						+
						"METHOD: 0x0401(public abstract) boo(Ldata/SimpleClassFour;)V\n"
						+
						"METHOD: 0x0401(public abstract) foo(Ldata/SimpleClassFour;)V\n"
						+
						"METHOD: 0x0401(public abstract) goo(Ldata/SimpleClassFour;IDLjava/lang/String;)Ljava/lang/String;\n"
						+
						"METHOD: 0x0401(public abstract) hoo(Ldata/SimpleClassFour;J)I\n"
						+
						"METHOD: 0x0401(public abstract) woo(Ldata/SimpleClassFour;)V java/lang/RuntimeException java/lang/IllegalStateException\n"
						+
						"METHOD: 0x0401(public abstract) __execute([Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;\n"
						+
						"METHOD: 0x0401(public abstract) ___clinit___()V\n"
						+
						"METHOD: 0x0401(public abstract) hashCode(Ldata/SimpleClassFour;)I\n"
						+
						"METHOD: 0x0401(public abstract) equals(Ldata/SimpleClassFour;Ljava/lang/Object;)Z\n"
						+
						"METHOD: 0x0401(public abstract) clone(Ldata/SimpleClassFour;)Ljava/lang/Object; java/lang/CloneNotSupportedException\n"
						+
						"METHOD: 0x0401(public abstract) toString(Ldata/SimpleClassFour;)Ljava/lang/String;\n" +
						"\n");
		// @formatter:on
	}

}
