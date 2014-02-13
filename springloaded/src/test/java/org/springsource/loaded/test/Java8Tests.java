package org.springsource.loaded.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;
import org.springsource.loaded.test.infra.ClassPrinter;
import org.springsource.loaded.test.infra.Result;

/**
 * Test reloading of Java 8.
 * 
 * @author Andy Clement
 * @since 1.1.5
 */
public class Java8Tests extends SpringLoadedTests {

	@Test
	public void theBasics() {
		String t = "basic.FirstClass";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = new ReloadableType(t, sc, 1, typeRegistry, null);

		assertEquals(1, rtype.getId());
		assertEquals(t, rtype.getName());
		assertEquals(slashed(t), rtype.getSlashedName());
		assertNotNull(rtype.getTypeDescriptor());
		assertEquals(typeRegistry, rtype.getTypeRegistry());
	}
	
	@Test
	public void callBasicType() throws Exception {
		String t = "basic.FirstClass";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals(8, r.returnValue);

		rtype.loadNewVersion("002", rtype.bytesInitial);

		r = runUnguarded(simpleClass, "run");
		assertEquals(8, r.returnValue);
	}
	
	@Test
	public void lambdaA() throws Exception {
		String t = "basic.LambdaA";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals(77, r.returnValue);
		ClassPrinter.print(rtype.bytesLoaded);

		rtype.loadNewVersion("002", rtype.bytesInitial);
		r = runUnguarded(simpleClass, "run");
		assertEquals(77, r.returnValue);
	}
	
	@Test
	public void changingALambda() throws Exception {
		String t = "basic.LambdaA";
		TypeRegistry typeRegistry = getTypeRegistry(t);
		byte[] sc = loadBytesForClass(t);
		ReloadableType rtype = typeRegistry.addType(t, sc);

		Class<?> simpleClass = rtype.getClazz();
		ClassPrinter.print(rtype.bytesLoaded);
		Result r = runUnguarded(simpleClass, "run");

		r = runUnguarded(simpleClass, "run");
		assertEquals(77, r.returnValue);

		rtype.loadNewVersion("002", retrieveRename(t,t+"2"));
		ClassPrinter.print(rtype.getLatestExecutorBytes());
		r = runUnguarded(simpleClass, "run");
		assertEquals(77, r.returnValue);
	}
	
	// TODO changing a lambda body
	// TODO changing a lambda signature
	// TODO adding a lambda that wasn't there before
	// TODO deleting a lambda
	// TODO make that inner interface non-public in LambdaA - seems to break things.

	// Bytecode for LambdaA
	/*
	  BootstrapMethods:
    0: #31 invokestatic java/lang/invoke/LambdaMetafactory.metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
      Method arguments:
        #32 ()I
        #33 invokestatic basic/LambdaA.lambda$run$0:()I
        #32 ()I

	public static int run();
    descriptor: ()I
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=1, locals=1, args_size=0
         0: aconst_null   
         1: astore_0      
         2: invokedynamic #3,  0              // InvokeDynamic #0:m:()Lbasic/LambdaA$Foo;
         7: astore_0      
         8: aload_0       
         9: invokeinterface #4,  1            // InterfaceMethod basic/LambdaA$Foo.m:()I
        14: ireturn       
	 */
	/*
	static void test() throws Throwable {
	    // THE FOLLOWING LINE IS PSEUDOCODE FOR A JVM INSTRUCTION
	    InvokeDynamic[#bootstrapDynamic].baz("baz arg", 2, 3.14);
	}
	private static void printArgs(Object... args) {
	  System.out.println(java.util.Arrays.deepToString(args));
	}
	private static final MethodHandle printArgs;
	static {
	  MethodHandles.Lookup lookup = MethodHandles.lookup();
	  Class thisClass = lookup.lookupClass();  // (who am I?)
	  printArgs = lookup.findStatic(thisClass,
	      "printArgs", MethodType.methodType(void.class, Object[].class));
	}
	private static CallSite bootstrapDynamic(MethodHandles.Lookup caller, String name, MethodType type) {
	  // ignore caller and name, but match the type:
	  return new ConstantCallSite(printArgs.asType(type));
	}
	}
*/

	

	
	// --
	
	private String slashed(String dotted) {
		return dotted.replaceAll("\\.", "/");
	}
}
