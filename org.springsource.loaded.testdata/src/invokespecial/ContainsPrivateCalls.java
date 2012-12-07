package invokespecial;

public class ContainsPrivateCalls {

	private int foo() {
		return 12;
	}

	private Long bar(long l) {
		return Long.valueOf(l);
	}

	private String goo(String s, boolean b, char ch) {
		return new StringBuilder(s).append(b).append(ch).toString();
	}

	public String callMyPrivates() {
		StringBuilder s = new StringBuilder();
		s.append(foo());
		s.append(bar(123L));
		s.append(goo("abc", true, 'z'));
		return s.toString();
	}
}
