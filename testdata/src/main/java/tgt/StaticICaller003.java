package tgt;

public class StaticICaller003 {
	public String run() {
		SimpleI003 si = new SimpleIClass003();
		return si.stringify(2.0d, 12, 32768L, false);
	}
}
