package accessors;

public class DefaultFields {

	int defaultField = 1;

	public int a() {
		return defaultField;
	}

	public static void main(String[] args) {
		DefaultFields top = new DefaultFields();
		DefaultFieldsSub bot = new DefaultFieldsSub();
		System.out.println(top.a());
		System.out.println(bot.a());
		//		System.out.println(top.b());
		System.out.println(bot.b());
	}
}
