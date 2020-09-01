package org.springsource.loaded.agent;

import java.io.File;

public interface FileSystemWatcherInterface {
	/**
	 * Shutdown the thread.
	 */
	public void shutdown();

	/**
	 * Add a new file to the list of those being monitored. If the file is something that can be watched, then this
	 * method will cause the thread to start (if it hasn't already been started).
	 *
	 * @param fileToMonitor the file to start monitor
	 */
	public void register(File fileToMonitor);
}
