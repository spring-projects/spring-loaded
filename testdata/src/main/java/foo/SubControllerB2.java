package foo;

public class SubControllerB2 extends ControllerB {

	public void foo() {
		super.foo();
		System.out.println("SubControllerB.foo() running again!");
	}
}
