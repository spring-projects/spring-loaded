package subpkg;

import superpkg.Target002;

public class Invoker002 {

	static Target002 t = new Target002();

	public void run() {
		t.m();
	}

}
