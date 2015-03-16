
package org.springsource.loaded.test;

public class TestUtils {

	public static String getPathToClasses(String path) {
		if (Boolean.parseBoolean(System.getProperty("springloaded.tests.useGradleBuildDir", "false"))) {
			return path + "/build/classes/main";
		}
		else {
			return path + "/bin";
		}
	}
}
