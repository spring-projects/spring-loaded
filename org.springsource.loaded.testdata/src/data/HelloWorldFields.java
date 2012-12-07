package data;

public class HelloWorldFields {

	String theMessage = "Hello Andy";

	public void greet() {
		System.out.println(theMessage);
	}

	public void setMessage(String newValue) {
		theMessage = newValue;
	}

}