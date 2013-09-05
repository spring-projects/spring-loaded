package executor;

@SuppressWarnings("unused")
public interface I2 {

	// annotation removed
	public void m();

	// two annotations added
	@common.Marker
	@common.Anno(id = "abc")
	public void m2();
}
