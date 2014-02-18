package basic;

public class LambdaD {

	public interface Boo { String m(int i,String s, int j, boolean b); }
	
	public static void main(String[] args) {
		run();
	}
	
	public static String run() {
		Boo f = null;
		f = (i,j,k,l) -> ""+l+i+k+j;
		return f.m(3,"abc",42,true);
	}
}
