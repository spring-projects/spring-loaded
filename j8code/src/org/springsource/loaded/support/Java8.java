package j8code;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class J8Helper {

	public static Object simulateInvokeDynamic(Object lookup) {
		try {
			CallSite callsite = callLambdaMetaFactory(lookup);
			// java.lang.invoke.ConstantCallSite@1e965684
			// nameAndDescriptor at invokedynamic: m()Lbasic/LambdaA$Foo;
			MethodHandles.Lookup caller = (MethodHandles.Lookup)lookup; 
			return callsite.dynamicInvoker().invokeWithArguments((Object[])null);//asType(MethodType.methodType())invoke(new Object[]{"m", MethodType.methodType(Class.forName("basic.LambdaA$Foo",false,caller.lookupClass().getClassLoader()))});
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
	
	
	public static CallSite callLambdaMetaFactory(Object lookup) throws Exception {
		// At invokedynamic:
		// bsmId = 0
		// nameAndDescriptor = m()Lbasic/LambdaA$Foo;	

//			    0: #31 invokestatic java/lang/invoke/LambdaMetafactory.metafactory:
				// (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;
				//  Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
//			      Method arguments:
//			        #32 ()I
//			        #33 invokestatic basic/LambdaA.lambda$run$0:()I
//			        #32 ()I
		
		// First two stacked by VM when used with invokedynamic
		
//	    0: #31 invokestatic java/lang/invoke/LambdaMetafactory.metafactory:
		// (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;
		//  Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
//	      Method arguments:
//	        #32 ()I
//	        #33 invokestatic basic/LambdaA.lambda$run$0:()I
//	        #32 ()I
		
		// caller = basic.LambdaA (MethodHandles$Lookup)
		
		// invokedName = m (String)
		// invokedType = ()Foo (MethodType)
		
		// samMethodType=()int (MethodType)
		// implMethod=MethodHandle()int (DirectMethodHandle - membername in this object is "basic.LambdaA.lambda$run$0()int/invokeStatic")
		// form members is:
//		
//		DMH.invokeStatic__I=Lambda(a0:L)=>{
//		    t1:L=DirectMethodHandle.internalMemberName(a0:L);
//		    t2:I=MethodHandle.linkToStatic(t1:L);t2:I}
		// instantiatedMethodType=()int (MethodType)
		MethodHandles.Lookup caller = (MethodHandles.Lookup)lookup;
		MethodType invokedType = MethodType.methodType(Class.forName("basic.LambdaA$Foo",false,caller.lookupClass().getClassLoader()));
		
		MethodType samMethodType = MethodType.methodType(Integer.TYPE);
		// Cheating here by changing first param to pretend the original type is looking for it rather than the executor
		MethodHandle implMethod = caller.findStatic(caller.lookupClass(), "lambda$run$0",MethodType.methodType(Integer.TYPE));
		MethodType instantiatedMethodType = MethodType.methodType(Integer.TYPE);
		return LambdaMetafactory.metafactory(caller, "m", invokedType, samMethodType, implMethod, instantiatedMethodType);
	}
}
