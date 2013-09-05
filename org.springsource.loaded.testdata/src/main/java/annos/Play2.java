package annos;

import java.lang.annotation.Documented;

// TODO [important] need to promote these to public so that the executor can see them!
@Documented
@interface Foo {
}

public class Play2 {

	public void run() {
		new Play2().doit();
	}

	public static void main(String[] args) {
		new Play2().doit();
	}

	public void doit() {
		Documented d = Foo.class.getAnnotation(Documented.class);
		System.out.println(d.annotationType());
	}
}
