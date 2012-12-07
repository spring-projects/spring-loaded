package annos;

@SimpleAnnotation
public class AnnotatedType {

	public static void printit() {
		System.out.println(AnnotatedType.class.getAnnotation(SimpleAnnotation.class));
	}
}
