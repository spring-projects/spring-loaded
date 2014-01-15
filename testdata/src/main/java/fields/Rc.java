package fields;

public class Rc extends Qc {
	public int getNumber() {
		return number;
	}
  public static void main(String []argv) {
    System.out.println(new Rc().getNumber());
  }
}