package reflection;

public class MethodTarget {

	public int methodOne() {
		return 35;
	}

	@AnnoT
	public void methodAnnotated() {
	}

	@AnnoT
	public void methodAnnotated2() {
	}

	public void methodAnnotated3(@AnnoT String s, @AnnoT2 int i, @AnnoT @AnnoT2 float f) {

	}

}
