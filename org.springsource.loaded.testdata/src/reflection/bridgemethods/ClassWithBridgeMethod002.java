package reflection.bridgemethods;

public class ClassWithBridgeMethod002 implements Cloneable {
	
	@Override
	protected ClassWithBridgeMethod002 clone() throws CloneNotSupportedException {
		return this;
	}

}
