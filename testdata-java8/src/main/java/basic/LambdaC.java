package basic;

public class LambdaC {

	public interface Boo { long m(int i,int j); }
	
	public static void main(String[] args) {
		run();
	}
	
	public static long run() {
		Boo f = null;
		f = (i,j) -> i*j;
		return f.m(3,2);
	}
}
