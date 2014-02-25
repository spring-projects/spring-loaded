package basic;

public class LambdaG2 {
	
	interface Boo { int m(); }
	
	public static void main(String[] args) {
		run();
	}
	
	public static int run() {
		Boo f = null;
		f = () -> 44;
		return f.m();
	}
}
