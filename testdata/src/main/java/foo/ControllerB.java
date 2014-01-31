package foo;

public class ControllerB extends grails.TopB {

	public void foo() {
		super.foo();
		System.out.println("ControllerB.foo() running");
	}
}
