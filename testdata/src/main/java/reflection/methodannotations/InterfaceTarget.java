package reflection.methodannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;

@AnnoT3(value = "Itf")
public interface InterfaceTarget {
	
	@AnnoT @AnnoT3("Boo")
	static final String myConstant = "Boohoo";

	@AnnoT @AnnoT2
	void pubMethod();
	
	@AnnoT3(value = "Foo")
	void privMethod();
	
	boolean defaultMethod(String a, int b);

}
