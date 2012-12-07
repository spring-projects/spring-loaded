package data;

public class Banana {

	Pear p = new Pear();

	public int[] getIntArrayField() {
		return p.intArray;
	}

	public void setIntArrayField(int[] is) {
		p.intArray = is;
	}

	public static int[] getStaticIntArrayField() {
		return Pear.staticIntArray;
	}

	public static void setStaticIntArrayField(int[] is) {
		Pear.staticIntArray = is;
	}

}
