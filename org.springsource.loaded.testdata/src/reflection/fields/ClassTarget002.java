package reflection.fields;

import reflection.nonrelfields.NonReloadableClassWithFields;

@SuppressWarnings("unused")
public class ClassTarget002 extends NonReloadableClassWithFields implements InterfaceTarget {

	public int myField = 666;
	public static String myStaticField = "staticField";
	private boolean myPrivateField = false;

	public String myChangedField = "201";
	private String myChangedPrivateField= "202"; // in ClassTarget was: private int myChangedPrivateField = 102;
	static String myChangedStaticField;// = "203"; static int myChangedStaticField = 103;

	public String myNewField = "201";
	private String myNewPrivateField = "202";
	static String myNewStaticField;// = "203";
	public int madePublicField = 9103;
	public static String madeStaticField;// = "nowStatic";

}
