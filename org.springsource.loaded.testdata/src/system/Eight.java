package system;

/**
 * This test class represents a class in the system set for the VM. These classes cannot have their reflective calls directly
 * intercepted because we cannot introduce dependencies on types in a lower classloader, so we have to call the reflective
 * interceptor reflectively!
 */
public class Eight {

	public Eight() {
	}

	public Eight(String s) {
	}

	public String runIt() throws Exception {
		StringBuilder data = new StringBuilder();
		int mods = m(Eight.class);
		data.append("mods?" + mods + " ");
		mods = m(DefaultVis.class);
		data.append("mods?" + mods + " ");
		mods = m(Inner.class);
		data.append("mods?" + mods + " ");
		return "complete:" + data.toString().trim();
	}

	public int m(Class<?> clazz) {
		return clazz.getModifiers();
	}

	private class Inner {

	}
}

class DefaultVis {
}