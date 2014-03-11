package remote;

import java.io.Serializable;

public class PersonB2 implements Serializable {

	private static final long serialVersionUID = 1L;

	private String firstname;
	private String lastname;
	

	public PersonB2(String f, String l) {
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
	
	public String getInitials() {
		return firstname.charAt(0)+""+lastname.charAt(0);
	}
}
