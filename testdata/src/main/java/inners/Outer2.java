package inners;

public class Outer2 {

	public static class Inner2 {
		public String foo() {
			return "bar!";
		}
		
		public int getModifiers() {
			return this.getClass().getModifiers();
		}
	}
}
