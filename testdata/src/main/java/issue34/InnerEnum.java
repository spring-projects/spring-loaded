package issue34;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class InnerEnum {
	
	public static void run() {
		InnerEnum.main(null);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		System.out.println("Hello World!");
		Map<String, String> map = new TreeMap<String, String>(sorters.string);
	}

	private static enum sorters implements Comparator<String> {
		string {
			private static final long serialVersionUID = 1L;

			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		}
	}
}