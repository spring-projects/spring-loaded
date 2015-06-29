package basic;

public class StaticMethodReference2 {

	public static String staticMethod(String s1, String s2) {
		System.out.println("in 2nd static Method");
		return "static" + s1 + s2;
	}

	public interface Bar { String sm(String s, String s1);}

	public interface Foo { String m(String s); }

	public String getFoo(String s)  {
		return "foo"+s;
	}

	public static void main(String[] args) {
		run();
	}

	public static String run() {
		StaticMethodReference2 l = new StaticMethodReference2();
		String r = l.run3();
		System.out.println(r);
		return l.run2();
	}

	public String run2() {
		Foo f = this::getFoo;
		return f.m("a");
	}

	public String run3() {
		Bar b = StaticMethodReference2::staticMethod;
		return b.sm("sa", "sb");
	}

}