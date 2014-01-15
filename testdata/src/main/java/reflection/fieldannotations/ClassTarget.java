package reflection.fieldannotations;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;
import reflection.CTAnnoT;

public class ClassTarget {

	public String fWithNo;
	
	@AnnoT @AnnoT3("boing") 
	public String fWithSame;
	
	public @AnnoT String fWithAdded;
	
	@AnnoT3("del") @AnnoT2 
	public String fWithRemoved;
	
	@AnnoT3("begin")
	public String fWithChanged;
	
	@AnnoT @AnnoT3("begin") @CTAnnoT
	public String fWithMixed;
	
}
