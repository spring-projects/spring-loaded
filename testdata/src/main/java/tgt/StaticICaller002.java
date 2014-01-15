package tgt;

public class StaticICaller002 {
	public String run() {
		SimpleI002 si = (SimpleI002) new SimpleIClass();
		return si.fromInt();
	}
}
