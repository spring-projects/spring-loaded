package interfaces;

public class Runner {

	public static TheInterface x = new TheImplementation();

	public int runGetValue() {
		return x.getValue();
	}

	public String runToString() {
		return x.toString();
	}

	public String doit() {
		return x.doit();
	}

}
