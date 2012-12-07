package invokespecial;

public class Simple extends Able {
	@SuppressWarnings("unused")
	private int iii = 2;

	public final String superCaller() {
		return super.toString();
	}

	public final String withParamSuperCaller() {
		return super.withParam(23);
	}

	public final String withDoubleSlotParamSuperCaller() {
		return super.withDoubleSlotParam(30L);
	}

	public final String withParamSuperCallerPrivateVariable() {
		return super.withDoubleSlotParamPrivateVariable();
	}
}
