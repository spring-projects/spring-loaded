package tgt;

public class StaticICaller005 {
	public int run() {
		SimpleI005 si = new SimpleIClass005();
		return si.toInt(36);
	}

	public Object run2() {
		SimpleI005 si = new SimpleIClass005();
		return si.changingReturnType();
	}
}
