package reflection.fieldannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;
import reflection.CTAnnoT;

public interface InterfaceTarget {

	String fWithNo = "fWithNo";
	
	@AnnoT @AnnoT3("boing") 
	String fWithSame = "same";
	
	@AnnoT String fWithAdded = "added";
	
	@AnnoT3("del") @AnnoT2 
	String fWithRemoved = "removed";
	
	@AnnoT3("begin")
	String fWithChanged = "changed";
	
	@AnnoT @AnnoT3("begin") @CTAnnoT
	String fWithMixed = "mixed";
	
}
