package reflection.fields;


@SuppressWarnings("unused")
public class SubClassTarget extends ClassTarget {

	public static String myStaticField = "mySub.staticField";	
	private String myPrivateField = "mySub.private";
	
	public String subField = "sub.staticField";	
	public static String subStaticField = "sub.staticField";	
	private String subPrivateField = "sub.private";
	
}
