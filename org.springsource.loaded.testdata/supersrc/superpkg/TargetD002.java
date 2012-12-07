package superpkg;

import java.io.Serializable;

public class TargetD002 {

	Serializable sub;

	public java.io.Serializable getOne() {
		Class<?> clazz;
		try {
			clazz = Class.forName("subpkg.Subby", false, Thread.currentThread().getContextClassLoader());
			sub = (Serializable) clazz.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sub;
	}

}
