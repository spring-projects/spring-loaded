package reflection.fieldannotations;

import reflection.AnnoT;
import reflection.AnnoT3;

public class ClassTarget003 {

	// Newly added fields below (so must have a version 003 to see if can make changes to them)
	
	public String newWithNo;
	
	@AnnoT @AnnoT3("boing") 
	public String newWithSame;
	
	@AnnoT @AnnoT3("added to new")
	public String newWithAdded;
	
	public String newWithRemoved;
	
	@AnnoT3("newly ended")
	public String newWithChanged;
	
	@AnnoT3("banana") @AnnoT
	public String newWithMixed;
	
}
