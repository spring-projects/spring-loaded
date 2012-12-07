package catchers;

class Super {
	public final int hashCode() {
		return 12;
	}
}

public class Finality extends Super {
	// no catcher, inherited method is final
}
