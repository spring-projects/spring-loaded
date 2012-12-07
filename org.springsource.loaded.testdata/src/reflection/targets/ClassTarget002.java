package reflection.targets;

import java.lang.reflect.Method;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;

@AnnoT2
public class ClassTarget002 {
	public int myField = 999;
	public static String myStaticField = "staticField";
	@SuppressWarnings("unused")
	private boolean myPrivateField = true;

	@AnnoT
	public ClassTarget002() {
	}

	public ClassTarget002(float f) {
		System.out.println(f);
	}

	@AnnoT3("can'tchange")
	public ClassTarget002(int f) {
		myField = f;
	}

	public int methodStays() {
		return 99;
	}

	public int methodChanged() {
		return 2;
	}

	public int lateMethod() {
		return 42;
	}

	public String doubleIt(String it) {
		return it + it;
	}

	public String changeIt(String it) {
		return it + " " + it + "!";
	}

	public int changeReturn(String it) {
		return it.length();
	}

	public String changeThem(String it, int repeat) {
		String result = "";
		for (int i = 0; i < repeat; i++) {
			result += it;
		}
		return result;
	}

	public String callPrivateMethod() throws Exception {
		Method privateOne = ClassTarget.class.getDeclaredMethod("privateMethod");
		return (String) privateOne.invoke(this);
	}

	@SuppressWarnings("unused")
	private String privateMethod() {
		return "new privateMethod result";
	}

	protected String protectedMethod() {
		return "new protectedMethod result";
	}

	public String overrideMethod() {
		return "ClassTarget002.overrideMethod";
	}

	String defaultMethod() {
		return "new defaultMethod result";
	}

	public static String staticMethod() {
		return "ClassTarget002.staticMethod";
	}

	public static int staticMethodAdded() {
		return 2;
	}

	public static String staticMethodAddedWithArgs(int i, String s) {
		return i + s + "002";
	}
}
