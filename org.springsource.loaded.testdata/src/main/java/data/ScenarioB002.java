package data;

import java.lang.annotation.Annotation;

public class ScenarioB002 {

	public static String methodAccessor() {
		try {
			ScenarioB002.class.getDeclaredMethod("foo");
			return "method found";
		} catch (Exception e) {
			return "method not found";
		}
	}

	public static String annoAccessor() {
		try {
			Annotation[] annos = ScenarioB002.class.getDeclaredMethod("foo").getDeclaredAnnotations();
			if (annos == null || annos.length == 0) {
				return "no annotations";
			} else {
				return "found " + (annos[0]);
			}
		} catch (Exception e) {
			return "no annotations";
		}
	}

	public void foo() {

	}
}
