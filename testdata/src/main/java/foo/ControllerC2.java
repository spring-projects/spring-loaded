package foo;

public class ControllerC2 extends grails.TopC {

	public void foo() {
		super.foo();
		System.out.println("ControllerC.foo() running again!");
	}
}
