package differs;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@interface Annot2 {
  String id() default "abc";
  long value() default 34;
}
