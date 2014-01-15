package interfacerewriting;

public class TheRunner002 {

	TheInterface002 ti = new TheImpl002();

	public String run() {
		return ti.foobar();
	}
}
