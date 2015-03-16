/*
 * Copyright 2010-2012 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springsource.loaded.agent;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springsource.loaded.FileChangeListener;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.TypeRegistry;

/**
 * A simple watcher for the file system. Uses a thread to keep an eye on a number of files and calls back registered
 * interested parties when a change is observed. The thread only starts when there is something to watch. The thread is
 * given a name indicating the classloader for which it is watching files. Once it starts to watch files the name will
 * be enhanced to indicate how many.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class FileSystemWatcher {

	// the thread being managed
	private Thread thread;

	// whether the thread is running
	private boolean threadRunning = false;

	// The Watcher running inside the thread
	private Watcher watchThread;

	public FileSystemWatcher(FileChangeListener listener, int typeRegistryId, String classloadername) {
		watchThread = new Watcher(listener, typeRegistryId, classloadername);
	}

	/**
	 * Start the thread if it isn't already started.
	 */
	private void ensureWatchThreadRunning() {
		if (!threadRunning) {
			thread = new Thread(watchThread);
			thread.setDaemon(true);
			thread.start();
			watchThread.setThread(thread);
			watchThread.updateName();
			threadRunning = true;
		}
	}

	/**
	 * Shutdown the thread.
	 */
	public void shutdown() {
		if (threadRunning) {
			watchThread.timeToStop();
		}
	}

	/**
	 * Add a new file to the list of those being monitored. If the file is something that can be watched, then this
	 * method will cause the thread to start (if it hasn't already been started).
	 * 
	 * @param fileToMonitor the file to start monitor
	 */
	public void register(File fileToMonitor) {
		if (watchThread.addFile(fileToMonitor)) {
			ensureWatchThreadRunning();
			watchThread.updateName();
		}
	}

	/**
	 * Enables the filesystem watching to be paused/unpaused.
	 * 
	 * @param shouldBePaused watching should be paused?
	 */
	public void setPaused(boolean shouldBePaused) {
		watchThread.paused = shouldBePaused;
	}
}


class Watcher implements Runnable {

	private static Logger log = Logger.getLogger(Watcher.class.getName());

	long lastScanTime;

	// TODO configurable scan interval?
	private static long interval = 1100;// ms

	List<File> watchListFiles = new ArrayList<File>();

	List<Long> watchListLMTs = new ArrayList<Long>();

	FileChangeListener listener;

	private boolean timeToStop = false;

	public boolean paused = false;

	private Thread thread = null;

	private int typeRegistryId;

	private String classloadername;

	private int registryLivenessCount = 0;

	private static int registryLivenessCountInterval = 300;

	public Watcher(FileChangeListener listener, int typeRegistryId, String classloadername) {
		this.listener = listener;
		this.typeRegistryId = typeRegistryId;
		this.classloadername = classloadername;
	}

	public void setThread(Thread thread) {
		this.thread = thread;
	}

	/**
	 * Add a new File that the thread should start watching. If the file does not exist nothing happens (this may be
	 * because a class has been generated on the fly and really there is nothing to watch on disk).
	 * 
	 * @param fileToWatch the new file to watch
	 * @return true if the file is now being watched, false otherwise
	 */
	public boolean addFile(File fileToWatch) {
		if (!fileToWatch.exists()) {
			return false;
		}
		synchronized (this) {
			if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
				log.info("Now watching " + fileToWatch);
			}
			int insertionPos = findPosition(fileToWatch);
			if (insertionPos == -1) {
				watchListFiles.add(fileToWatch);
				watchListLMTs.add(fileToWatch.lastModified());
			}
			else {
				watchListFiles.add(insertionPos, fileToWatch);
				watchListLMTs.add(insertionPos, fileToWatch.lastModified());
			}
			return true;
		}
	}

	public void updateName() {
		if (thread != null) {
			thread.setName("FileSystemWatcher: files=#" + watchListFiles.size() + " cl=" + classloadername);
		}
	}

	private int findPosition(File file) {
		String filename = file.getName();
		int len = watchListFiles.size();
		if (len == 0) {
			return 0;
		}
		for (int f = 0; f < len; f++) {
			File file2 = watchListFiles.get(f);
			int cmp = file2.getName().compareTo(filename);
			// as we are using 'names' we are only considering the last part, so foo/bar/Goo.class and foo/Goo.class look the same
			// and will return cmp==0.  Not really sure it matters about using fq names
			if (cmp > 0) {
				return f;
			}
			else if (GlobalConfiguration.assertsMode && cmp == 0) {
				// Are we watching the same file twice, that is bad!
				if (file2.getAbsoluteFile().toString().equals(file.getAbsoluteFile().toString())) {
					log.severe("Watching the same file twice: " + file.getAbsoluteFile().toString());
				}
			}
		}
		return -1;
	}

	public void run() {
		while (!timeToStop) {
			registryLivenessCount++;
			if ((registryLivenessCount % registryLivenessCountInterval) == 0) {
				// Time to check if the registry is still alive!
				if (!TypeRegistry.typeRegistryExistsForId(typeRegistryId)) {
					if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
						log.info("TypeRegistry " + typeRegistryId + " gone, no point in thread continuing!");
					}
					return;
				}
				registryLivenessCount = 0;
			}
			try {
				Thread.sleep(interval);
			}
			catch (Exception e) {
			}
			if (!paused) {
				List<File> changedFiles = new ArrayList<File>();
				synchronized (this) {
					int len = watchListFiles.size();
					for (int f = 0; f < len; f++) {
						File file = watchListFiles.get(f);
						long lastModTime = file.lastModified();
						if (lastModTime > watchListLMTs.get(f)) {
							if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
								log.info("Observed last modification time change for " + file + " (lastScanTime="
										+ lastScanTime + ")");
							}
							watchListLMTs.set(f, lastModTime);
							changedFiles.add(file);
						}
					}
					lastScanTime = System.currentTimeMillis();
				}
				for (File changedFile : changedFiles) {
					determineChangesSince(changedFile, lastScanTime);
				}
			}
		}
	}

	/*
	 * problem is that we check some file X, it hasn't changed - we then take longer than interval to check all the
	 * other files we are watching.
	 */

	private void determineChangesSince(File file, long lastScanTime) {
		try {
			if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
				log.info("Firing file changed event " + file);
			}
			listener.fileChanged(file);
			if (file.isDirectory()) {
				File[] filesOfInterest = file.listFiles(new RecentChangeFilter(lastScanTime));
				for (File f : filesOfInterest) {
					if (f.isDirectory()) {
						determineChangesSince(f, lastScanTime);
					}
					else {
						if (GlobalConfiguration.verboseMode && log.isLoggable(Level.INFO)) {
							log.info("Observed last modification time change for " + f + "  (lastScanTime="
									+ lastScanTime + ")");
							log.info("Firing file changed event " + file);
						}
						listener.fileChanged(f);
					}
				}
			}
		}
		catch (Throwable t) {
			if (log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, "FileWatcher caught serious error, see cause", t);
			}
		}
	}

	static class RecentChangeFilter implements FileFilter {

		private long lastScanTime;

		public RecentChangeFilter(long lastScanTime) {
			this.lastScanTime = lastScanTime;
		}

		public boolean accept(File pathname) {
			return (pathname.lastModified() > lastScanTime);
		}

	}

	public void timeToStop() {
		timeToStop = true;
	}

}
