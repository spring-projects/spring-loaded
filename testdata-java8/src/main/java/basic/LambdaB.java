package basic;

public class LambdaB {

	public interface Foo { long m(int i); }
	
	public static void main(String[] args) {
		run();
	}
	
	public static long run() {
		Foo f = null;
		f = (i) -> i*33;
		return f.m(3);
	}
}
