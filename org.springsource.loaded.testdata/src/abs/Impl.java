package abs;

public class Impl extends AbsImpl {
  public int method() {
    return 1;
  }

  public static void run() {
    System.out.println(new Impl().method());
  }
}
