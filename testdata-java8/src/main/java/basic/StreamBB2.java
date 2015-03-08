package basic;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

//interface StreamBBFoo {
//    int m(int n);
//}

public class StreamBB2 {

    public static void main(String[] args) {
        System.out.println("This is Java8");
    }

    public static int run() {
        StreamBBFoo foo = (n) -> n + 1;
        List<Integer> integers = Arrays.asList(1, 2, 3, 4);
        List<Integer> mapped = integers.stream().map(foo::m).collect(Collectors.toList());
        return mapped.size();
    }

}