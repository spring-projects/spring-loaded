package reflection.methodannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;

/**
 * Test class containing methods with some annotations on their params.
 */
public interface ParamAnnotInterface002 {
	
	void noParams();
	public void noAnnotations(@AnnoT String a, boolean b);
	
	int someAnnotations(int a, @AnnoT3("b002_itf") boolean b);
	
	public void addedMethodNoParams();
	public void addedMethodNoAnnots(String a, double b);
	public void addedMethodSomeAnnots(@AnnoT2 @AnnoT double a, @AnnoT3("boing_itf") String b);

}
