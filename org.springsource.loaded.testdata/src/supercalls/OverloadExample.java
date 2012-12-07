package supercalls;

class OverloadSuperclass {
	protected String s = "s";

	//	public String toString() {
	//		return "instance of " + OverloadSuperclass.class.toString();
	//	}
}

public class OverloadExample extends OverloadSuperclass {

	class OverloadExampleInner {
		public String toString() {
			return OverloadExample.super.toString();
			// return OverloadExample.super.s;
		}
	}

	public String toString() {
		OverloadExampleInner oi = new OverloadExampleInner();
		return oi.toString();
	}

	public static void main(String[] args) {
		OverloadExample o = new OverloadExample();

		System.out.println(o);
	}

}