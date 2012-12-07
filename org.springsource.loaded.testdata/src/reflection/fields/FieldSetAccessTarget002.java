package reflection.fields;

import java.lang.reflect.Field;

/**
 * A class with some fields in it.
 * 
 * @author kdvolder
 */
@SuppressWarnings("unused")
public class FieldSetAccessTarget002 {

	// Fields that are still in the reloaded version with their value changed
	private String privateField = "new privateField value";
	protected String protectedField = "new protectedField value";
	String defaultField = "new defaultField value";
	public String publicField = "new publicField value";
	public final String finalPublicField = "new finalPublicField value";
	private final String finalPrivateField = "new finalPrivateField value";

	// Same as above, but also some primitive types (different code paths)
	private int privatePrimField = 21;
	protected int protectedPrimField = 22;
	int defaultPrimField = 23;
	public int publicPrimField = 24;
	public int deletedPrimField = 25;
	public final int finalPrimField = 26;
	private final int finalPrivatePrimField = 27;
	
	// For access checking when calls originate in "privileged" context (i.e. the class itself)
	/**
	 * Gets a field in class, can override access constraints if requested to do so.
	 */
	public String getFieldWithAccess(Class<?> targetClass, String whichField, boolean setAccess) throws Exception {
		Object targetInstance = targetClass.newInstance();
		Field field = targetClass.getDeclaredField(whichField);
		if (setAccess) {
			field.setAccessible(true);
		}
		return (String) field.get(targetInstance);
	}

	/**
	 * Sets a field in class, can override access constraints if requested to do so.
	 */
	public void setFieldWithAccess(Class<?> targetClass, String whichField, boolean setAccess) throws Exception {
		Object targetInstance = targetClass.newInstance();
		Field field = targetClass.getDeclaredField(whichField);
		if (setAccess) {
			field.setAccessible(true);
		}
		// Not checking for type errors in this test, make sure we set correct type of value
		if (field.getType().equals(int.class)) {
			field.set(targetInstance, 888);
		} else {
			field.set(targetInstance, "<BANG>");
		}
	}
	
}
