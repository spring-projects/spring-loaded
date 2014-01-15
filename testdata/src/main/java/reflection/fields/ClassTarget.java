package reflection.fields;

import reflection.nonrelfields.NonReloadableClassWithFields;

@SuppressWarnings("unused")
public class ClassTarget extends NonReloadableClassWithFields implements InterfaceTarget {

	public int myField = 999;
	public static String myStaticField = "staticField";
	private boolean myPrivateField = true;

	public int myDeletedField = 100;
	private String myDeletedPrivateField = "ClassTarget.myDeletedPrivateField";
	static String myDeletedStaticField = "ClassTarget.myDeletedStaticField";

	public int myChangedField = 101;
	private int myChangedPrivateField = 102;
	static int myChangedStaticField = 103;
	private int madePublicField = 103;
	public String madeStaticField = "notStaticYet";
	
}