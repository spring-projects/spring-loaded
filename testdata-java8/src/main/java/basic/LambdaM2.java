package basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LambdaM2 {
	
	public static void main(String[] args) {
		run();
	}
	
	public static String run() {
		return new LambdaM2().run2();
	}
	
	public String run2() {
		List<String> list = new ArrayList<>();
		list.add("test3");
		Map m = list.stream().collect(Collectors.toMap(this::foo, Function.identity()));
		return m.toString();
	}
	
	public int foo(String input) {
		return input.length()*2;
	}
	
}
