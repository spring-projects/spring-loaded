package prot;

public class SubOne extends One {

	public int getPublicField() {
		return publicField;
	}

	public void setPublicField(int publicField) {
		this.publicField = publicField;
	}

	public int getProtectedField() {
		return protectedField;
	}

	public void setProtectedField(int protectedField) {
		this.protectedField = protectedField;
	}

	public static int getPublicStaticField() {
		return publicStaticField;
	}

	public static void setPublicStaticField(int publicStaticField) {
		One.publicStaticField = publicStaticField;
	}

	public static int getProtectedStaticField() {
		return protectedStaticField;
	}

	public static void setProtectedStaticField(int protectedStaticField) {
		One.protectedStaticField = protectedStaticField;
	}

	public int[] getProtectedArrayOfInts() {
		return protectedArrayOfInts;
	}

	public void setProtectedArrayOfInts(int[] protectedArrayOfInts) {
		this.protectedArrayOfInts = protectedArrayOfInts;
	}

	public String[] getProtectedArrayOfStrings() {
		return protectedArrayOfStrings;
	}

	public void setProtectedArrayOfStrings(String[] protectedArrayOfStrings) {
		this.protectedArrayOfStrings = protectedArrayOfStrings;
	}

	public long[][] getProtectedArrayOfArrayOfLongs() {
		return protectedArrayOfArrayOfLongs;
	}

	public void setProtectedArrayOfArrayOfLongs(long[][] protectedArrayOfArrayOfLongs) {
		this.protectedArrayOfArrayOfLongs = protectedArrayOfArrayOfLongs;
	}

	public short getProtectedShortField() {
		return protectedShortField;
	}

	public void setProtectedShortField(short protectedShortField) {
		this.protectedShortField = protectedShortField;
	}

	public long getProtectedLongField() {
		return protectedLongField;
	}

	public void setProtectedLongField(long protectedLongField) {
		this.protectedLongField = protectedLongField;
	}

	public boolean isProtectedBooleanField() {
		return protectedBooleanField;
	}

	public void setProtectedBooleanField(boolean protectedBooleanField) {
		this.protectedBooleanField = protectedBooleanField;
	}

	public byte getProtectedByteField() {
		return protectedByteField;
	}

	public void setProtectedByteField(byte protectedByteField) {
		this.protectedByteField = protectedByteField;
	}

	public char getProtectedCharField() {
		return protectedCharField;
	}

	public void setProtectedCharField(char protectedCharField) {
		this.protectedCharField = protectedCharField;
	}

	public double getProtectedDoubleField() {
		return protectedDoubleField;
	}

	public void setProtectedDoubleField(double protectedDoubleField) {
		this.protectedDoubleField = protectedDoubleField;
	}

	public float getProtectedFloatField() {
		return protectedFloatField;
	}

	public void setProtectedFloatField(float protectedFloatField) {
		this.protectedFloatField = protectedFloatField;
	}
}
