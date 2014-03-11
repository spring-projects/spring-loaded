package remote;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Person implements Serializable {

	private String firstname;
	private String lastname;
	
	public Person(String f, String l) {
		this.firstname = f;
		this.lastname = l;
	}
	
	public String getFirstname() {
		return firstname;
	}
	
	public String getLastname() {
		return lastname; 
	}
	
	public String toString() {
		return firstname+" "+lastname;
	}
	
}
