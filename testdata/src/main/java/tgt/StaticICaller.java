package tgt;

public class StaticICaller {
	public int run() {
		SimpleI si = new SimpleIClass();
		return si.toInt("123");
	}

	public Object run2() {
		SimpleI si = new SimpleIClass();
		return si.changingReturnType();
	}
}
