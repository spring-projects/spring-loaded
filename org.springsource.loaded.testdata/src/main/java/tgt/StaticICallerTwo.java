package tgt;

public class StaticICallerTwo {
	public int run() {
		SimpleITwo si = new SimpleIClassTwo();
		return si.toInt("123");
	}
}
