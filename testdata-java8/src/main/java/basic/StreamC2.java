package basic;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StreamC2 {

    public static void main(String[] args) {
        System.out.println("This is Java8");
    }

    public static int run() {
        List<Integer> integers = Arrays.asList(1, 2, 3, 4);
        List<Integer> mapped = integers.stream().map(n -> n).collect(Collectors.toList());
        return mapped.size();
    }

}