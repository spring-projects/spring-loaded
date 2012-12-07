package reflection.methodannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;

/**
 * Test class containing methods with some annotations on their params.
 */
public interface ParamAnnotInterface {
	
	void noParams();
	public void noAnnotations(String a, boolean b);
	public int someAnnotations(@AnnoT int a, @AnnoT3("b") @AnnoT2 boolean b);

}
