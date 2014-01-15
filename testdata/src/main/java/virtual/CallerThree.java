package virtual;

public class CallerThree {
	static CalleeThreeTop top = new CalleeThreeTop();
	static CalleeThreeBottom bottom = new CalleeThreeBottom();

	public String runTopToString() {
		return top.toString();
	}

	public String runBottomToString() {
		return bottom.toString();
	}

}
