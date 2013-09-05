package reflection.targets;

import java.lang.reflect.Method;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;

@AnnoT2
public class ClassTarget003 {

	@AnnoT2
	int myField = 10;

	@AnnoT
	public ClassTarget003() {
	}

	@AnnoT3("can'tchange")
	public ClassTarget003(int f) {
		myField = f;
		System.out.println("modified!");
	}

	public int methodStays() {
		return 99;
	}

	public int methodChanged() {
		return 3; //Changed from v002
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

	String defaultMethod() {
		return "new defaultMethod result";
	}

	public static int staticMethodAdded() {
		return 3;
	}

	public static String staticMethodAddedWithArgs(int i, String s) {
		return i + s + "003";
	}

	@Override
	public String toString() {
		return "ClassTarget003.toString";
	}
}
