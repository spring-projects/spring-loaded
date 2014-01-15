package common;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Anno {
	String id();

	int someValue() default 37;

	long longValue() default 2L;
}
