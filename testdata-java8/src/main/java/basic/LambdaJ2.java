package basic;

public class LambdaJ2 {
	
	public interface Foo { String m(String s, String t); }

	public String getFoo(String s, String t)  {
		return "foo"+s+t;
	}
	
	public static void main(String[] args) {
		run();
	}
	
	public static String run() {
		return new LambdaJ().run2();
	}
	
	public String run2() {
		Foo f = this::getFoo;
		return f.m("a","b");
	}
	
}
