package annos;

@SimpleAnnotation
public class AnnotatedType2 {

	public static void printit() {
		System.out.println(">>" + AnnotatedType2.class.getAnnotation(SimpleAnnotation.class));
	}
}
