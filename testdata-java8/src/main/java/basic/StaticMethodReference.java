package basic;

public class StaticMethodReference {

	public static String staticMethod(String s1) {
		System.out.println("in 1st static Method");
		return "static" + s1;
	}

	public interface Bar { String sm(String s);}

	public interface Foo { String m(String s); }

	public String getFoo(String s)  {
		return "foo"+s;
	}

	public static void main(String[] args) {
		run();
	}

	public static String run() {
		StaticMethodReference l = new StaticMethodReference();
		String r = l.run3();
		System.out.println(r);
		return l.run2();
	}

	public String run2() {
		Foo f = this::getFoo;
		return f.m("a");
	}

	public String run3() {
		Bar b = StaticMethodReference::staticMethod;
		return b.sm("sa");
	}

}