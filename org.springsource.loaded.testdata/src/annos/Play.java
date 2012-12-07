package annos;

import java.lang.annotation.Documented;
import java.lang.reflect.Method;

public class Play {

	public void run() throws Exception {
		new Play().doit();
	}

	public static void main(String[] args) throws Exception {
		new Play().doit();
	}

	public void doit() throws Exception {
		Class<?> clazz = Documented.class;
		System.out.println(clazz.getName());
		Method m = clazz.getMethod("annotationType");
		Documented d = Foo2.class.getAnnotation(Documented.class);
		System.out.println(m);
		System.out.println(d);
		System.out.println(m.getDeclaringClass().getName());
	}
}
// TODO $Proxy0 in the case I'm investigating is the impl of jlaDocumented.
