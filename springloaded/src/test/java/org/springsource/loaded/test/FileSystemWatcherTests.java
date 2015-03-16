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

package org.springsource.loaded.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springsource.loaded.FileChangeListener;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.agent.FileSystemWatcher;


public class FileSystemWatcherTests {

	/**
	 * Create a folder, watch it then put a couple of files in and check they are detected
	 */
	@Ignore
	@Test
	public void dirs() throws IOException {
		TestFileChangeListener listener = new TestFileChangeListener();
		File dir = getTempDir();
		FileSystemWatcher watcher = new FileSystemWatcher(listener, -1, "test");
		watcher.register(dir);
		pause(1000);
		create(dir, "abc.txt");
		pause(1100);
		create(dir, "abcd.txt");
		pause(1100);
		watcher.shutdown();
		Assert.assertTrue(listener.changesDetected.contains("abc.txt"));
		Assert.assertTrue(listener.changesDetected.contains("abcd.txt"));
	}

	@Test
	public void files() throws IOException {
		TestFileChangeListener listener = new TestFileChangeListener();
		File dir = getTempDir();
		FileSystemWatcher watcher = new FileSystemWatcher(listener, -1, "test");
		pause(1000);
		File f1 = create(dir, "abc.txt");
		watcher.register(f1);
		pause(1100);
		File f2 = create(dir, "abcd.txt");
		watcher.register(f2);
		pause(1100);
		watcher.setPaused(true);
		// Whilst paused, touch both files
		touch(f2);
		touch(f1);
		watcher.setPaused(false);
		pause(3000);
		watcher.shutdown();
		System.out.println(listener.changesDetected);
		assertEquals("abc.txt", listener.changesDetected.get(0));
		assertEquals("abcd.txt", listener.changesDetected.get(1));
	}

	@Ignore
	@Test
	public void innersFirst() throws IOException {
		System.out.println("innersFirst");
		TestFileChangeListener listener = new TestFileChangeListener();
		File dir = getTempDir();
		FileSystemWatcher watcher = new FileSystemWatcher(listener, -1, "test");
		pause(1000);
		File f1 = create(dir, "Book$1.class");
		watcher.register(f1);
		pause(1100);
		File f2 = create(dir, "Book.class");
		watcher.register(f2);
		pause(1100);
		File f3 = create(dir, "Book$_2.class");
		watcher.register(f3);
		pause(1100);
		watcher.setPaused(true);
		// Whilst paused, touch both files
		touch(f3);
		touch(f2);
		touch(f1);
		watcher.setPaused(false);
		pause(3000);
		System.out.println(listener.changesDetected);
		watcher.shutdown();
		// Check that inners reported first
		assertEquals("Book$1.class", listener.changesDetected.get(0));
		assertEquals("Book$_2.class", listener.changesDetected.get(1));
		assertEquals("Book.class", listener.changesDetected.get(2));
	}

	@Ignore
	@Test
	public void innerInnersFirst() throws IOException {
		TestFileChangeListener listener = new TestFileChangeListener();
		File dir = getTempDir();
		FileSystemWatcher watcher = new FileSystemWatcher(listener, -1, "test");
		pause(1000);
		File f1 = create(dir, "Book$1.class");
		watcher.register(f1);
		pause(1100);
		File f2 = create(dir, "Book.class");
		watcher.register(f2);
		pause(1100);
		File f3 = create(dir, "Book$_2.class");
		watcher.register(f3);
		pause(1100);
		File f4 = create(dir, "Book$Foo.class");
		watcher.register(f4);
		pause(1100);
		File f5 = create(dir, "Book$Foo$1.class");
		watcher.register(f5);
		pause(1100);
		watcher.setPaused(true);
		// Whilst paused, touch both files
		touch(f5);
		touch(f4);
		touch(f3);
		touch(f2);
		touch(f1);
		watcher.setPaused(false);
		pause(1100);
		watcher.shutdown();
		// Check that inners reported first
		System.out.println(listener.changesDetected);
		assertEquals("Book$1.class", listener.changesDetected.get(0));
		assertEquals("Book$Foo$1.class", listener.changesDetected.get(1));
		assertEquals("Book$Foo.class", listener.changesDetected.get(2));
		assertEquals("Book$_2.class", listener.changesDetected.get(3));
		assertEquals("Book.class", listener.changesDetected.get(4));
	}

	private void touch(File f) {
		try {
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(3);
			fos.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private File create(File dir, String filename) throws IOException {
		File f = new File(dir, filename);
		boolean b = f.createNewFile();
		Assert.assertTrue(b);
		return f;
	}

	private void pause(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (Exception e) {
		}
	}

	private File getTempDir() {
		try {
			File tempFile;
			tempFile = File.createTempFile("eternal", "");
			// File base =
			tempFile.getParentFile();
			// String name =
			// tempFile.getName();
			tempFile.delete();
			boolean b = tempFile.mkdir();
			if (!b) {
				throw new RuntimeException("Failed to create folder " + tempFile);
			}
			return tempFile;
		}
		catch (IOException e) {
			return null;
		}
	}

	static class TestFileChangeListener implements FileChangeListener {

		List<String> changesDetected = new ArrayList<String>();

		public void fileChanged(File file) {
			System.out.println("File change detected " + file);
			changesDetected.add(file.getName());
		}

		public void register(ReloadableType rtype, File file) {
		}
	}
}
