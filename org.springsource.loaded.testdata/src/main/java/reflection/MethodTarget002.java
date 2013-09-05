package reflection;

public class MethodTarget002 {

	public int methodOne() {
		return 37;
	}

	public int lateMethod() {
		return 42;
	}

	@AnnoT2
	@AnnoT
	public void methodAnnotated() {

	}

	@AnnoT2
	public void methodAnnotated2() {
	}

	// changed, deleted, reordered
	public void methodAnnotated3(@AnnoT2 String s, int i, @AnnoT2 @AnnoT float f) {
	}

}
