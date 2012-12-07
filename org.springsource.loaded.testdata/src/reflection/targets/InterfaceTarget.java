package reflection.targets;

public interface InterfaceTarget {
	
	static final String CONSTANT = "Hello Constrant";
	static final double PI = 3.14;
	
	String interfaceMethod();
	int deletedInterfaceMethod();

}
