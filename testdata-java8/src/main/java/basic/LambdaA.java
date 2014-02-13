package basic;

public class LambdaA {

	public interface Foo { int m(); }
	
	public static void main(String[] args) {
		run();
	}
	
	public static int run() {
		Foo f = null;
		f = () -> 77;
		return f.m();
	}
}
