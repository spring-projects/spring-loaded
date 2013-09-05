package reflection.targets;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import reflection.AnnoT;

@AnnoT
public class ClassTarget {

	public int myField = 999;
	public static String myStaticField = "staticField";
	@SuppressWarnings("unused")
	private boolean myPrivateField = true;

	public ClassTarget() {

	}

	public ClassTarget(float f) { // position on line20 important, must match in ClassTarget002
		System.out.println(f);
	}

	public int methodStays() {
		return 99;
	}

	public int methodDeleted() {
		return 37;
	}

	public int methodChanged() {
		return 1;
	}

	public String changeIt(String it) {
		return it + "ho!";
	}

	public String changeReturn(String it) {
		return it + "ho!";
	}

	public String changeThem(String it, int add) {
		return it + add;
	}

	public String deleteThem(String it, int add) {
		return it + add;
	}

	public String callPrivateMethod() throws Exception {
		Method privateOne = ClassTarget.class.getDeclaredMethod("privateMethod");
		return (String) privateOne.invoke(this);
	}

	@SuppressWarnings("unused")
	private String privateMethod() {
		return "privateMethod result";
	}

	protected String protectedMethod() {
		return "protectedMethod result";
	}

	String defaultMethod() {
		return "defaultMethod result";
	}

	public String overrideMethod() {
		return "ClassTarget.overrideMethod";
	}

	public String overrideMethodDeleted() {
		return "ClassTarget.overrideMethodDeleted";
	}

	public static String staticMethod() {
		return "ClassTarget.staticMethod";
	}

	public int callPublicMethodOnDefaultClass() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		Method publicOne = DefaultClass.class.getDeclaredMethod("publicMethod");
		return (Integer) publicOne.invoke(new DefaultClass());
	}

	/**
	 * This main method is just here to have some place to put 'test' code so we can try what *should* happen when we run this
	 * normally without springloaded.
	 */
	public static void main(String[] args) throws Exception {
		System.out.println(new ClassTarget().callPrivateMethod()); //Works!!!
	}
}
