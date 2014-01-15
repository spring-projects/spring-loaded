package reflection.bridgemethods;

public class ClassWithBridgeMethod implements Cloneable {
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return this;
	}

}
