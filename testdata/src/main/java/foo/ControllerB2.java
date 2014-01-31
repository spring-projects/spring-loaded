package foo;

public class ControllerB2 extends grails.TopB {

	public void foo() {
		super.foo();
		System.out.println("ControllerB.foo() running again!");
	}
}
