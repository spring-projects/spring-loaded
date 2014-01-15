package reflection.fieldannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;
import reflection.CTAnnoT;

public interface InterfaceTarget002 {

	String fWithNo = "no";
	
	@AnnoT @AnnoT3("boing") 
	String fWithSame = "sam";
	
	@AnnoT @AnnoT2 @AnnoT3("added") 
	String fWithAdded = "add";
	
	@AnnoT2 
	String fWithRemoved = "rem";
	
	@AnnoT3("end")
	String fWithChanged = "cha";
	
	@AnnoT2 @AnnoT3("doinf") @CTAnnoT
	String fWithMixed = "mix";

	// Newly added fields below (so must have a version 003 to see if can make changes to them)
	
	String newWithNo = "nno";
	
	@AnnoT @AnnoT3("boing") 
	String newWithSame = "nws";
	
	@AnnoT String newWithAdded= "nwa";
	
	@AnnoT3("del") @AnnoT2 
	String newWithRemoved = "nwr";
	
	@AnnoT3("begin")
	String newWithChanged = "nwc";
	
	@AnnoT @AnnoT3("begin") @CTAnnoT
	String newWithMixed = "nwm";
	
}
