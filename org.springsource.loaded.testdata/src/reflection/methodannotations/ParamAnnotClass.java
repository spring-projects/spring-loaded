package reflection.methodannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;

/**
 * Test class containing methods with some annotations on their params.
 */
public class ParamAnnotClass {
	
	@AnnoT
	public ParamAnnotClass(@AnnoT String s) {
	}

	protected void noParams() {}

	public void noAnnotations(String a, boolean b) { }
	
	public int someAnnotations(@AnnoT int a, @AnnoT3("b") @AnnoT2 boolean b) {
		return 654321;
	}
	
	public static int staticNoParams() { return 0; }
	public static int staticSomeParams(@AnnoT int a, @AnnoT2 @AnnoT3("static") boolean b) { return 0; }

}
