package basic;

public class LambdaE {

	public interface Boo { String m(char s); }
	
	public static void main(String[] args) {
		run();
	}
	
	public static String run() {
 		int i = 4;
		Boo f = null;
		f = (c) -> { 
			StringBuilder buf = new StringBuilder();
  			for (int j=0;j<i;j++) {
				buf.append(c);
			}
			return buf.toString();
		};
		return f.m('a');
	}
}
