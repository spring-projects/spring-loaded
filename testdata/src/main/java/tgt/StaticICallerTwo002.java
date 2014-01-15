package tgt;

public class StaticICallerTwo002 {
	public String run() {
		SimpleITwo002 si = (SimpleITwo002) new SimpleIClassTwo();
		return si.fromLong(27);
	}
}
