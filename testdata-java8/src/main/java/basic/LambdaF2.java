package basic;

public class LambdaF2 {
	
	public int fieldOne = 3;
	
	public String concatenator(char ch, int number) {
		StringBuilder buf = new StringBuilder();
			for (int j=0;j<number;j++) {
			buf.append(ch);
			buf.append(':');
		}
		return buf.toString();		
	}

	public interface Boo { String m(char s); }
	
	public static void main(String[] args) {
		run();
	}
	
	public static String run() {
		return new LambdaF2().x();
	}
	
	public String x() {
 		int i = 4;
		Boo f = null;
		f = (c) -> { 
			return concatenator(c,fieldOne);
		};
		return f.m('a');
	}
}
