package data;

public class ScenarioA004 {

	String name;

	public String foo() {
		return "from " + getValue();
	}

	public String getValue() {
		if (name == null) {
			name = "004";
		}
		return "ScenarioA " + name;
	}

	public String getName() {
		return name;
	}
}
