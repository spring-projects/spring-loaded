package basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LambdaO {

	public static void main(String[] args) {
		run();
	}

	public static int run() {
		List<Integer> integers = Arrays.asList(1, 2, 3);
		List<Integer> collected = integers.stream().collect(Collectors.toCollection(ArrayList::new));
		return collected.size();
	}

}