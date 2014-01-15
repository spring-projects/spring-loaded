package reflection.targets;

@SuppressWarnings("unused")
public class SubClassTarget extends ClassTarget {

	public int mySubField = 999;
	public static String myStaticSubField = "staticField";
	private boolean myPrivateField = true;
	private boolean myPrivateSubField = false;

	public String subMethod() {
		return "SubClassTarget.subMethod";
	}

	@Override
	public String overrideMethod() {
		return "SubClassTarget.overrideMethod";
	}

	public String overrideMethodDeleted() {
		return "SubClassTarget.overrideMethodDeleted";
	}

	public static int staticMethodAdded() {
		return 1999;
	}
}
