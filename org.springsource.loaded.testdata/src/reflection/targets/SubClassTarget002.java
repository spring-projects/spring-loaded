package reflection.targets;

@SuppressWarnings("unused")
public class SubClassTarget002 extends ClassTarget {

	public int mySubField = 999;
	public static String myStaticSubField = "staticField";
	private boolean myPrivateField = true;
	private boolean myPrivateSubField = false;

	@Override
	protected String protectedMethod() {
		return "SubClassTarget002.protectedMethod";
	}

	@Override
	public String overrideMethod() {
		return "SubClassTarget002.overrideMethod";
	}

}
