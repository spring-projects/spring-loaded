package reflection.nonrelfields;

public class NonReloadableClassWithFields {
	
	@SuppressWarnings("unused")
	private String nrlPriv = "nrlPriv";
	String nrlPub = "nrlPub";
	static public String nrlStatic = "nrlPub";
	
	// Coverage of different types (as needed to cover all kinds of "set/get" methods.
	
	public boolean nrlBool = true;
	protected byte nrlByte = 12;
	char nrlChar = 'z';
	@SuppressWarnings("unused")
	private double nrlDouble = 12.3;
	public float nrlFloat = (float) 10.3;
	protected int nrlInt = 123;
	long nrlLong = 12345;
	public short nrlShort = 1;

	// Coverage of different primtivi type fields that are 'final' to check that all
	// generated error messages for setting those are formatted correctly
	
	final boolean fnrlBool = true;
	final protected byte fnrlByte = 12;
	final char fnrlChar = 'z';
	@SuppressWarnings("unused")
	final private double fnrlDouble = 12.3;
	final float fnrlFloat = (float) 10.3;
	final protected int fnrlInt = 123;
	final long fnrlLong = 12345;
	final short fnrlShort = 1;

	// One 'final public' of each type, to see if 'coerced' values in messages correctly formatted
	final public boolean fpnrlBool = true;
	final public byte fpnrlByte = 12;
	final public char fpnrlChar = 'z';
	final public double fpnrlDouble = 12.3;
	final public float fpnrlFloat = (float) 10.3;
	final public int fpnrlInt = 123;
	final public long fpnrlLong = 12345;
	final public short fpnrlShort = 1;
	
}
