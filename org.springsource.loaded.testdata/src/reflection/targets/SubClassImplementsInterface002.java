package reflection.targets;

import reflection.NonReloadableSuperClass;

public class SubClassImplementsInterface002 extends NonReloadableSuperClass implements InterfaceTarget {

	@Override
	public int deletedInterfaceMethod() {
		return 0;
	}
	
	public void extraMethod() {
	}

}
