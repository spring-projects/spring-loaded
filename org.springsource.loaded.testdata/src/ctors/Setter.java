package ctors;

public class Setter {

	public int integer;
	public String string;
	public Setter setter;

	Setter() {
		integer = 1;
		string = "one";
	}

	public String toString() {
		return "instance of Setter";
	}

	public int getInteger() {
		return integer;
	}

	public String getString() {
		return string;
	}

}
