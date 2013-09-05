package reflection.methodannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;

/**
 * Test class containing methods with some annotations on their params.
 */
public class ParamAnnotClass002 {
	
	@AnnoT
	public ParamAnnotClass002(@AnnoT String s) {
	}

	protected void noParams() {}

	public void noAnnotations(@AnnoT String a, boolean b) { }
	
	public int someAnnotations(@AnnoT int a, @AnnoT3("b002") boolean b) {
		return 654321;
	}
	
	public static int staticNoParams() { return 0; }
	public static int staticSomeParams(@AnnoT3("reveresed") @AnnoT2 int a, @AnnoT boolean b) { return 0; }
	
	public void addedMethodNoParams() {}
	public void addedMethodNoAnnots(String a, double b) {}
	public void addedMethodSomeAnnots(@AnnoT2 @AnnoT double a, @AnnoT3("boing") String b) {}

	public static int addedStaticNoParams() { return 0; }
	public static int addedStaticSomeParams(@AnnoT3("added") @AnnoT2 int a, @AnnoT boolean b) { return 0; }
}
