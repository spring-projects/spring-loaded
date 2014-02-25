package basic;

public class LambdaD2 {

	public interface Boo { String m(int i,String s, int j, boolean b); }
	
	public static void main(String[] args) {
		run();
	}
	
	public static String run() {
		Boo f = null;
		f = (i,j,k,l) -> j+(i*k)+l;
		return f.m(3,"def",88,true);
	}
}
