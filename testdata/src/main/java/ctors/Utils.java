package ctors;

public class Utils {

	public static String stack(int n) {
		StringBuilder s = new StringBuilder();
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		// Skip the currentThread() entry and this stack() entry
		if (ste[1].toString().indexOf("stack(") == -1) {
			throw new IllegalStateException("Assumed stack was entry 2");
		}
		s.append(ste[2]).append("\n");
		return s.toString();
	}

}
