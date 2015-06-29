package basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LambdaM {
	
//	public interface Foo { String m(String s); }

//	public String getFoo(String s)  {
//		return "foo"+s;
//	}
	
	public static void main(String[] args) {
		run();
	}
	
	public static String run() {
		return new LambdaM().run2();
	}
	
	public String run2() {
		List<String> list = new ArrayList<>();
		list.add("test3");
		Map m = list.stream().collect(Collectors.toMap(String::length, Function.identity()));
		return m.toString();
	}
	
}
