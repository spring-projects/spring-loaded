package executor;

import common.Marker;

public interface I {
	@Marker
	public void m();

	//@common.Marker
	//@common.Anno(id = "abc")
	public void m2();
}
