package foo;

public class ControllerD extends grails.TopD {

	public void foo() {
		super.foo();
		System.out.println(getMessage());
	}
	
	private String getMessage() {
		return "ControllerD.foo() running";
	}
}
