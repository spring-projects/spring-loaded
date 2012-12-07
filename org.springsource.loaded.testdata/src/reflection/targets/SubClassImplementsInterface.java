package reflection.targets;

import reflection.NonReloadableSuperClass;

public class SubClassImplementsInterface extends NonReloadableSuperClass implements InterfaceTarget {

	@Override
	public int deletedInterfaceMethod() {
		return 0;
	}

}
