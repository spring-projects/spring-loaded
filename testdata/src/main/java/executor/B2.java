package executor;



public class B2 {

	// annotation removed
	public void m() {
	}

	// two annotations added
	@common.Marker
	@common.Anno(id = "abc")
	public void m2() {
	}
}
