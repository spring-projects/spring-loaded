package clinitg;

public class Four {

	static { 
		System.out.println("original clinit");
		bar()
	}

	public static String run() {
		bar()
		boo()
		baz()
	}
	
	public static void bar() {
		print '1'
	}
	
	public static void boo() {
		print '2'
	}
	
	public static void baz() {
		print '3'
	}
}
