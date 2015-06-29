package basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LambdaN {
	
	public static void main(String[] args) {
		run();
	}
	
	public static String run() {
		return new LambdaN().run2();
	}
	
	public String run2() {
		List<String> list = new ArrayList<>();
		HelperN h = new HelperN();
		list.add("test3");
		Map m = list.stream().collect(Collectors.toMap(h::foo, Function.identity()));
		return m.toString();
	}
	
	
}

class HelperN {
	public int foo(String input) {
		return input.length()*3;
	}
	
}