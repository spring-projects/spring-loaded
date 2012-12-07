package reflection.fieldannotations;

import reflection.AnnoT;
import reflection.AnnoT3;

public interface InterfaceTarget003 {

	// Newly added fields below (so must have a version 003 to see if can make changes to them)

	String newWithNo = "nno";
	
	@AnnoT @AnnoT3("boing") 
	String newWithSame = "nws";
	
	@AnnoT @AnnoT3("added to new")
	String newWithAdded = "bingo";
	
	String newWithRemoved = "something";
	
	@AnnoT3("newly ended")
	String newWithChanged = "ahah";
	
	@AnnoT3("banana") @AnnoT
	String newWithMixed = "blender";
		
}
