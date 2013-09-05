package subpkg;

import superpkg.TargetC002;
import superpkg.TargetImplC002;

public class InvokerC002 {

	public void run() {
		TargetC002 t = new TargetImplC002();
		t.m();
	}

}
