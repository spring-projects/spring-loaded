package virtual;

public class CallerOne {
	static CalleeOne cone = new CalleeOne();

	public String run() {
		return cone.toString();
	}

}
