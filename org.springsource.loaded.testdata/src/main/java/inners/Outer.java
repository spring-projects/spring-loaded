package inners;

public class Outer {

	public static class Inner {
		public String foo() {
			return "foo!";
		}
		public int getModifiers() {
			return this.getClass().getModifiers();
		}
	}
}
