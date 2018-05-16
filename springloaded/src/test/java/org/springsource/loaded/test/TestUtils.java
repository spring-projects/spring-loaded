
package org.springsource.loaded.test;

public class TestUtils {

	public static String getPathToClasses(String path) {
		return getPathToClasses(path, false);
	}

	public static String getPathToClasses(String path, Boolean testFolder) {
		if (Boolean.parseBoolean(System.getProperty("springloaded.tests.useGradleBuildDir", "false"))) {
			return path + "/build/classes/" + (testFolder ? "test" : "main");
		}
		else {
			return path + "/bin";
		}
	}
}
