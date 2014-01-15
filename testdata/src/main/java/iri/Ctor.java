package iri;

import java.lang.reflect.Constructor;

public class Ctor {

	public Ctor(String s, int i) {

	}

	public static String run() throws Exception {
		Constructor<Ctor> c = Ctor.class.getDeclaredConstructor(String.class, Integer.TYPE);
		Ctor instance = (Ctor) c.newInstance("abc", 3);
		return instance.toString();
	}

	public String toString() {
		return "instance";
	}
}
