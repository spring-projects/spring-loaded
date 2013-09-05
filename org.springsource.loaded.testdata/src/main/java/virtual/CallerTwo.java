package virtual;

public class CallerTwo {
	static CalleeTwoTop top = new CalleeTwoTop();
	static CalleeTwoBottom bottom = new CalleeTwoBottom();

	public String runTopToString() {
		return top.toString();
	}

	public String runBottomToString() {
		return bottom.toString();
	}

}
