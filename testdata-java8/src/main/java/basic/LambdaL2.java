package basic;

public class LambdaL2 {

  public interface Foo { String m(String s, String t); }

  public String getFoo(String s, String t)  {
    System.out.println("in second foo");
    return "foo"+s+t;
  }

  public static void main(String[] args) {
    run();
  }

  public static String run() {
   String res= new LambdaL2().run2();
    System.out.println(res);
    return res;
  }

  public String run2() {
    Foo f = this::getFoo;
    return f.m("a","b");
  }

}