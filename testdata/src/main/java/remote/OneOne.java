package remote;

import java.lang.reflect.Method;

public class OneOne {
public static void main(String[] args) throws Exception {
	System.out.println(hasStaticInitializer(OneOne.class));
}

  private static boolean hasStaticInitializer(Class cl) {
	  try {
		 Method[] ms = cl.getDeclaredMethods();
		 for (Method m: ms) {
			 System.out.println(m);
		 }
		  cl.getDeclaredMethod("<clinit>");
		  return true;
	  }
	  catch (Exception e) {
		  return false;
	  }
  }
  static {
	  System.out.println();
  }
}
