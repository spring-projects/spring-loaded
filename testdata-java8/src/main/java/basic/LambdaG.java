package basic;

public class LambdaG {
	
	interface Boo { int m(); }
	
	public static void main(String[] args) {
		run();
	}
	
	public static int run() {
		Boo f = null;
		f = () -> 99;
		return f.m();
	}
}
