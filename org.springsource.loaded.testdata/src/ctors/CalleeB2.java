package ctors;

public class CalleeB2 extends CalleeSuperB2 {

	CalleeB2() {

	}

	CalleeB2(String theString) {
		super(32768);
		System.out.println(theString);
	}

	public String toString() {
		return "callee";
	}

}
