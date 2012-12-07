package subpkg;

import superpkg.TargetD;

public class InvokerD {

	public void run() {
		System.out.println(new TargetD().getOne());
	}

}
