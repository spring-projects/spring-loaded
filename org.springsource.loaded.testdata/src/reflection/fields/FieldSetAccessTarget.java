package reflection.fields;

import java.lang.reflect.Field;

/**
 * A class with some fields in it.
 * 
 * @author kdvolder
 */
@SuppressWarnings("unused")
public class FieldSetAccessTarget {

	// Fields that are still in the reloaded version with their value changed
	private String privateField = "privateField value";
	protected String protectedField = "protectedField value";
	String defaultField = "defaultField value";
	public String publicField = "publicField value";
	public String deletedPublicField = "deletedPublicField value";
	public final String finalPublicField = "finalPublicField value";
	private final String finalPrivateField = "finalPrivateField value";
	
	// Same as above, but also some primitive types (different code paths)
	private int privatePrimField = 11;
	protected int protectedPrimField = 12;
	int defaultPrimField = 13;
	public int publicPrimField = 14;
	public int deletedPrimField = 15;
	public final int finalPrimField = 16;
	private final int finalPrivatePrimField = 17;
	
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
