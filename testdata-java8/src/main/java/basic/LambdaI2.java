package basic;

public class LambdaI2 {
	
	public interface Foo { String m(String in, String in2); }

	
	public static void main(String[] args) {
		run();
	}
	
	public static String run() {
		Foo f = (s,t) -> s+t;
		return f.m("a", "b");
	}

}
