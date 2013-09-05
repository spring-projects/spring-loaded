package iri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FormattingHelper {

	public String format(Annotation[] as) {
		List<String> names = new ArrayList<String>();
		for (Annotation f : as) {
			names.add(f.toString());
		}
		return sortAndPrintNames(names);
	}

	public String format(Field[] fs) {
		List<String> names = new ArrayList<String>();
		for (Field f : fs) {
			names.add(f.getName());
		}
		return sortAndPrintNames(names);
	}

	public String format(Field f) {
		return f.getName();
	}

	public String format(Method[] ms) {
		List<String> list = new ArrayList<String>();
		for (Method m : ms) {
			list.add(format(m));
		}
		return sortAndPrintNames(list);
	}

	public String format(Method m) {
		StringBuilder s = new StringBuilder();
		s.append(m.getName());
		Class<?>[] ps = m.getParameterTypes();
		if (ps == null) {
			s.append("()");
		} else {
			s.append("(");
			for (int i = 0; i < ps.length; i++) {
				if (i > 0) {
					s.append(",");
				}
				s.append(ps[i].getSimpleName());
			}
			s.append(")");
		}
		return s.toString().trim();
	}

	public String format(Constructor<?>[] ms) {
		List<String> list = new ArrayList<String>();
		for (Constructor<?> m : ms) {
			list.add(format(m));
		}
		return sortAndPrintNames(list);
	}

	public String format(Constructor<?> m) {
		StringBuilder s = new StringBuilder();
		s.append(m.getName());
		Class<?>[] ps = m.getParameterTypes();
		if (ps == null) {
			s.append("()");
		} else {
			s.append("(");
			for (int i = 0; i < ps.length; i++) {
				if (i > 0) {
					s.append(",");
				}
				s.append(ps[i].getSimpleName());
			}
			s.append(")");
		}
		return s.toString().trim();
	}

	public String sortAndPrintNames(List<String> names) {
		StringBuilder s = new StringBuilder();
		Collections.sort(names);
		s.append(names.size()).append(":");
		for (String n : names) {
			s.append(n).append(" ");
		}
		return s.toString().trim();
	}
}
