package reflection.fields;

import reflection.SubTestVal;
import reflection.TestVal;

@SuppressWarnings("unused")
public class SubClassTarget002 extends ClassTarget {

	public static String myStaticField = "mySub.staticField";
	private String myPrivateField = "mySub.private";

	public String subField = "sub.staticField";
	public static String subStaticField = "sub.staticField";
	private String subPrivateField = "sub.private";

	public String myDeletedField = "movedToSubclass";
	private String myDeletedPrivateField = "movedToSubclassPrivate";
	static String myDeletedStaticField;// = "movedToSubclassStatic";

	// Ensure coverage of all primitive types.
	byte byteField = 123;
	long longField = 123123;
	short shortField = 5;
	boolean boolField = true;
	char charField = 'A';
//	int intField; //no need plenty of fields with ints elsewhere already
	float floatField = (float)3.14;
	double doubleField = 6.28;
	
	// Ensure coverage of boxed types
	Byte boxByteField = 123;
	Long boxLongField = (long)123123;
	Short boxShortField = 5;
	Boolean boxBoolField = true;
	Character boxCharField = 'A';
	Integer intField = 10;
	Float boxFloatField = (float)3.14;
	Double boxDoubleField = 6.28;
	
	// Ensure coverage of object types other than string, and having subtype relations
	SubTestVal subSubTypeField = SubTestVal.it;
	TestVal superSubTypeField = SubTestVal.it;
	TestVal superSuperTypeField = TestVal.it;

}
