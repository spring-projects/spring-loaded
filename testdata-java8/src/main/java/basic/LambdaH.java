package basic;

public class LambdaH {
	
	public interface Foo { int m(); }

	public int fieldOne = 7;
	
	public int concatenator(int a, int b) {
		return a*b;
	}

	
	public static void main(String[] args) {
		run();
	}
	
	public static int run() {
		int count = 0;
		count += ((Foo)()->7).m();
		count += new LambdaH().x();
		count += ((Foo)()->21).m();
		return count;
	}
	
	public int x() {
 		int i = 4;
		Foo f = null;
		f = () -> { 
			return concatenator(i,fieldOne);
		};
		return f.m();
	}
}
