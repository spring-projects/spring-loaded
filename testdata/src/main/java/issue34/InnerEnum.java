package issue34;

//import java.util.Comparator;
//import java.util.Map;
//import java.util.TreeMap;

public class InnerEnum {
	
	public static void run() {
		InnerEnum.main(null);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		System.out.println("Hello World!");
		Object o = sorters.string;
//		Map<String, String> map = new TreeMap<String, String>(sorters.string);
	}
	
	// May be able to switch back to using Comparator (and the TreeMap line above) once AspectJ 1.8.0 is out
	interface MyComparator<T> {
		int compare(T a,T b);
		boolean equals(Object o);
	}

	private static enum sorters implements MyComparator<String> {
		string {
			private static final long serialVersionUID = 1L;

			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
			
		}
	}
}