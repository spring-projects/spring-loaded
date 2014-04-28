package basic;

public class LambdaJ {
	
	public interface Foo { String m(String s); }

	public String getFoo(String s)  {
		return "foo"+s;
	}
	
	public static void main(String[] args) {
		run();
	}
	
	public static String run() {
		return new LambdaJ().run2();
	}
	
	public String run2() {
		Foo f = this::getFoo;
		return f.m("a");
	}
	
}
