package remote;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Person2 implements Serializable {

	private String firstname;
	private String lastname;
	
	public Person2(String f, String l) {
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
	
//	private String getInitials() {
//		return firstname.charAt(0)+""+lastname.charAt(0);
//	}
}
