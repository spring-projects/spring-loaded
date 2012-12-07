package subpkg;

import superpkg.TopB;

public class BottomB extends TopB {

	public void m() {
		System.out.println("BottomB.m() running");
	}

	public void run() {
		m();
	}

}
