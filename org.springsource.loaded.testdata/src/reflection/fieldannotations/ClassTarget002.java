package reflection.fieldannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;
import reflection.CTAnnoT;

public class ClassTarget002 {

	public String fWithNo;
	
	@AnnoT @AnnoT3("boing") 
	public String fWithSame;
	
	@AnnoT @AnnoT2 @AnnoT3("added") 
	public String fWithAdded;
	
	@AnnoT2 
	public String fWithRemoved;
	
	@AnnoT3("end")
	public String fWithChanged;
	
	@AnnoT2 @AnnoT3("doinf") @CTAnnoT
	public String fWithMixed;

	// Newly added fields below (so must have a version 003 to see if can make changes to them)
	
	public String newWithNo;
	
	@AnnoT @AnnoT3("boing") 
	public String newWithSame;
	
	public @AnnoT String newWithAdded;
	
	@AnnoT3("del") @AnnoT2 
	public String newWithRemoved;
	
	@AnnoT3("begin")
	public String newWithChanged;
	
	@AnnoT @AnnoT3("begin") @CTAnnoT
	public String newWithMixed;
	
}
