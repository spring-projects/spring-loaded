package simple

import java.lang.reflect.Field;

class SelfReflector {

	public static void main(String[] args) {
		println run();
	}
	public int i
	
	public static String run() {
		StringBuilder s = new StringBuilder()
		Field[] fs = getFields()
		List<String> names = new ArrayList<String>()
		for (Field f: fs) {
			names.add(f.getName());
		}
		Collections.sort(names);
		s.append(names.size()).append(" ");
		for (String n: names) {
			s.append(n).append(" ");
		}
		return s.toString().trim();
	}
	
	public static Field[] getFields() {
		return SelfReflector.class.getDeclaredFields();
	}
}
