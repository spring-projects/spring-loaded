package subpkg;

import superpkg.TargetC002;
import superpkg.TargetImplC002;

public class InvokerC003 {

	public void run() {
		TargetC002 t = new TargetImplC002();
		t.n();
	}

}
