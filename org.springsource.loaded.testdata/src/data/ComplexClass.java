package data;

import java.util.List;

@SuppressWarnings({ "unused", "serial" })
class ComplexClass extends SimpleClass implements java.io.Serializable {
	private int privateField;
	public String publicField;
	List<String> defaultField;

	private int privateMethod() {
		return privateField;
	}

	public String publicMethod() {
		return publicField;
	}

	List<String> defaultMethod() {
		return defaultField;
	}

	void thrower() throws Exception, IllegalStateException {

	}
}