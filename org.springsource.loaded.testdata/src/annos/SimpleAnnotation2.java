package annos;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SimpleAnnotation2 {
	String value() default "hello";

	int number() default 42;
}
