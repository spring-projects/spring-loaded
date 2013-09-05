package data;

import java.lang.annotation.Annotation;

public class ScenarioB003 {

	public static String methodAccessor() {
		try {
			ScenarioB003.class.getDeclaredMethod("foo");
			return "method found";
		} catch (Exception e) {
			return "method not found";
		}
	}

	public static String annoAccessor() {
		try {
			Annotation[] annos = ScenarioB003.class.getDeclaredMethod("foo").getDeclaredAnnotations();
			if (annos == null || annos.length == 0) {
				return "no annotations";
			} else {
				return "found " + (annos[0]);
			}
		} catch (Exception e) {
			return "no annotations";
		}
	}

	@Wiggle
	public void foo() {

	}
}
