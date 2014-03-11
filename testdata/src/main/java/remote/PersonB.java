package remote;

import java.io.Serializable;

public class PersonB implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String firstname;
	private String lastname;
	
	public PersonB(String f, String l) {
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
