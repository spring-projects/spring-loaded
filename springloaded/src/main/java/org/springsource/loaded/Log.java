
package org.springsource.loaded;

/**
 * Minimal support for logging messages, avoids all dependencies it can because it will be loaded very early by the VM
 * and we don't want to introduce unnecessarily complex classloading.
 * 
 * @author Andy Clement
 * @since 1.1.5
 */
public class Log {

	public static void log(String message) {
		System.out.println("SL: " + message);
	}

}
