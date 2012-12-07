package data;

public aspect AnAspect {

	before(): execution(!static * AspectReceiver.*(..)) {
		System.out.println("Foo");
	}
	
	pointcut boo(String foo,String bar): execution(* *(..)) && args(foo,bar);
	before(String foo): boo(foo,*) {
		
	}
}
