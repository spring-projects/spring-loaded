package reflection.methodannotations;

import reflection.AnnoT;
import reflection.AnnoT3;

public interface InterfaceTarget002 {

	@AnnoT @AnnoT3("Boo")
	static final String myConstant = "Boohoo";
	
	@AnnoT @AnnoT3("snazzy")
	void pubMethod();
	
	@AnnoT @AnnoT3("snazzy")
	void dingdong();
	
	@AnnoT3(value = "Bar")
	void privMethod();

	@Deprecated @AnnoT
	boolean defaultMethod(String a, int b);

}
