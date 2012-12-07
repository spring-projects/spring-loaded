package subpkg;

import superpkg.Target;

public class Invoker {

	static Target t = new Target();

	public void run() {
		System.out.println("Invoker.run() running");
	}

}
