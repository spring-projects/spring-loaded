package foo;

public class SubControllerB extends ControllerB {

	public void foo() {
		super.foo();
		System.out.println("SubControllerB.foo() running");
	}
}
