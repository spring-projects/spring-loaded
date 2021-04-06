/*
 * Copyright 2010-2012 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springsource.loaded.agent;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.springsource.loaded.FileChangeListener;
import org.springsource.loaded.GlobalConfiguration;
import org.springsource.loaded.ReloadableType;
import org.springsource.loaded.TypeRegistry;


/**
 *
 * @author Andy Clement
 * @since 0.5.0
 */
public class ReloadableFileChangeListener implements FileChangeListener {

	private static Logger log = Logger.getLogger(ReloadableFileChangeListener.class.getName());

	private TypeRegistry typeRegistry;

	private Map<File, ReloadableType> correspondingReloadableTypes = new HashMap<File, ReloadableType>();

	Map<File, Set<JarEntry>> watchedJarContents = new HashMap<File, Set<JarEntry>>();

	static class JarEntry {

		final ReloadableType rtype;

		final String slashname;

		long lmt;

		public JarEntry(ReloadableType rtype, String slashname, long lmt) {
			this.rtype = rtype;
			this.slashname = slashname;
			this.lmt = lmt;
		}
	}

	public ReloadableFileChangeListener(TypeRegistry typeRegistry) {
		this.typeRegistry = typeRegistry;
	}

	public void fileChanged(File file) {
		if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
			log.info(" processing change for " + file);
		}
		ReloadableType rtype = correspondingReloadableTypes.get(file);
		if (file.getName().endsWith(".jar")) {
			if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
				log.info(" processing change for JAR " + file);
			}
			try {
				ZipFile zf = new ZipFile(file);
				Set<JarEntry> entriesBeingWatched = watchedJarContents.get(file);
				for (JarEntry entryBeingWatched : entriesBeingWatched) {
					ZipEntry ze = zf.getEntry(entryBeingWatched.slashname);
					long lmt = ze.getTime();//getLastModifiedTime().toMillis();
					if (lmt > entryBeingWatched.lmt) {
						// entry in jar has been updated
						if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
							log.info(" detected update to jar entry. jar=" + file.getName() + " class="
									+ entryBeingWatched.slashname + "  OLD LMT=" + new Date(entryBeingWatched.lmt)
									+ " NEW LMT=" + new Date(lmt));
						}
						typeRegistry.loadNewVersion(entryBeingWatched.rtype, lmt, zf.getInputStream(ze));
						entryBeingWatched.lmt = lmt;
					}
				}
				zf.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			typeRegistry.loadNewVersion(rtype, file);
		}
	}

	public void register(ReloadableType rtype, File file) {
		if (file.getName().endsWith(".jar")) {
			// Compute the last mod time of the entry in the jar
			String slashname = "";
			try {
				ZipFile zf = new ZipFile(file);
				slashname = rtype.getSlashedName() + ".class";
				ZipEntry ze = zf.getEntry(slashname);
				long lmt = ze.getTime();//LastModifiedTime().toMillis();
				JarEntry je = new JarEntry(rtype, slashname, lmt);
				zf.close();
				Set<JarEntry> jarEntries = watchedJarContents.get(file);
				if (jarEntries == null) {
					jarEntries = new HashSet<JarEntry>();
					watchedJarContents.put(file, jarEntries);
				}
				jarEntries.add(je);
				if (GlobalConfiguration.isRuntimeLogging && log.isLoggable(Level.INFO)) {
					log.info(" watching jar file entry. Jar=" + file + "  file=" + rtype.getSlashedName() + " lmt="
							+ lmt);
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}catch (NullPointerException ex)
			{
				log.warning("class : " +slashname + "not exist in Jar , register  watch failed  " );
				ex.printStackTrace();
			}
		}
		else {
			correspondingReloadableTypes.put(file, rtype);
		}
	}
}
