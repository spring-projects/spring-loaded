package subpkg;

public class Controller002 extends grails.Top {
	public void foo() {
		super.foo();
		System.out.println("subpkg.ControllerB.foo() running again!");
	}
}
