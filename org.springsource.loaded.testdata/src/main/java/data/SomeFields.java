package data;

import java.util.List;
import java.util.Map;

@SuppressWarnings({ "unused", "serial" })
class SomeFields extends SimpleClass implements java.io.Serializable {
	private int privateField;
	public String publicField;
	List<String> defaultField;
	protected Map<String, List<Integer>> protectedField;
}