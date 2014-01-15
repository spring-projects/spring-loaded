package tgt;

public class SimpleIClassTwo implements SimpleITwo {
	public int toInt(String s) {
		return Integer.parseInt(s);
	}

	public String fromLong(long l) {
		return Long.toString(l);
	}

}
