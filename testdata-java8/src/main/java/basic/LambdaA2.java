package basic;

public class LambdaA2 {

	public interface Foo { int m(); }
	
	public static void main(String[] args) {
		run();
	}
	
	public static int run() {
		Foo f = null;
		f = () -> 88;
		return f.m();
	}
}
