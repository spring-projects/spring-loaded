package catchers;

public class B extends A {

	public Object callProtectedMethod() {
		return protectedMethod();
	}
}
