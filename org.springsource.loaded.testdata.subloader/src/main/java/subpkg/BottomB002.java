package subpkg;

import superpkg.TopB002;

public class BottomB002 extends TopB002 {

	public void m() {
		super.m();
	}

	public void run() {
		m();
	}

}
