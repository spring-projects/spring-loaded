package data;

public class Reflector {

	public void runOne() {
		AnnotatedClazz.class.getAnnotation(Anno.class);
	}

}
