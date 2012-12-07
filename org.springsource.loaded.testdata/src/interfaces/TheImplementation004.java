package interfaces;

public class TheImplementation004 implements TheInterface004 {

	public int getValue() {
		return 23;
	}

	public String toString() {
		return "i am version 3";
	}

	public String newmethod() {
		return "oranges";
	}

	public String doit() {
		TheInterface004 ti = new TheImplementation004();
		return ti.newmethod();
	}
}
