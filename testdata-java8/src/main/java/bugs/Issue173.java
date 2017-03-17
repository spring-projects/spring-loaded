package bugs;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Issue173 {
    public static void main(String[] args) {
        System.out.println(run());
    }

    public static String run() {
        return url("path").toString();
    }

    public static URI url(Object... args) {
        try {
            /* flattening the paths */
            final List<String> paths = Arrays
                    .stream(args)
                    .flatMap(arg -> {
                        if (arg instanceof Collection) {
                            return ((Collection<Object>) arg).stream();
                        } else if (arg instanceof Object[]) {
                            return Arrays.stream((Object[]) arg);
                        }

                        return Arrays.stream(new Object[]{
                                arg
                        });
                    })
                    .map(Object::toString)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            Path path = Paths.get("/", paths.toArray(new String[0]));
            URI uri = new URI("https", "www.redacted.com", path.toString(), null);

            return uri;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
