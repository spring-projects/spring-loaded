package foo;

public class ControllerD2 extends grails.TopD {

	public void foo() {
		super.foo();
		System.out.println(getMessage());
	}
	
	private String getMessage() {
		return "ControllerD.foo() running again!";
	}
}
