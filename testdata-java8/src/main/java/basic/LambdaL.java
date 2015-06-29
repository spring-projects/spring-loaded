package basic;

public class LambdaL {
  static {
    System.out.println("original static initializer");
  }

  public LambdaL() {
    System.out.println("original instance");
  }

    public interface Foo { String m(String s); }

    public String getFoo(String s)  {
        System.out.println("in first foo");
        return "foo"+s;
    }

    public static void main(String[] args) {
        run();
    }

    public static String run() {
        String res= new LambdaL().run2();
        System.out.println(res);
        return res;
    }

    public String run2() {
        Foo f = this::getFoo;
        return f.m("a");
    }

} 
