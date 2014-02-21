package basic;

public class LambdaI {
	
	public interface Foo { String m(String in); }

	
	public static void main(String[] args) {
		run();
	}
	
	public static String run() {
		Foo f = (s) ->  s;
		return f.m("a");
	}
	
}
