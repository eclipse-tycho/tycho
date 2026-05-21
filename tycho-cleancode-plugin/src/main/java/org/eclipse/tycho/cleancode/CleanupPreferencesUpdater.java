/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.cleancode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Updates Eclipse JDT cleanup preferences in the org.eclipse.jdt.ui.prefs file.
 * <p>
 * This class manages updates to cleanup profile settings and save actions in Eclipse project
 * preferences. It supports updating both the main cleanup profile settings and the save action
 * settings (prefixed with "sp_"). Changes are written back to the file only when {@link #close()}
 * is called and updates have been made.
 * </p>
 */
public class CleanupPreferencesUpdater implements AutoCloseable {

	private Path prefsFile;
	private boolean updated = false;
	private List<String> lines;
	private Map<String, String> cleanUpProfile;

	/**
	 * Creates a new updater for the specified preferences file.
	 * 
	 * @param prefsFile
	 *            the path to the org.eclipse.jdt.ui.prefs file
	 * @param cleanUpProfile
	 *            the cleanup profile settings to apply (key-value pairs)
	 * @throws IOException
	 *             if the file cannot be read
	 */
	public CleanupPreferencesUpdater(Path prefsFile, Map<String, String> cleanUpProfile) throws IOException {
		this.prefsFile = prefsFile;
		this.cleanUpProfile = cleanUpProfile;
		this.lines = Files.readAllLines(prefsFile, StandardCharsets.UTF_8);
	}

	/**
	 * Updates the project cleanup profile settings in the preferences file.
	 * <p>
	 * This method updates existing cleanup.* keys and adds any new keys from the cleanup profile.
	 * Changes are marked for writing when {@link #close()} is called.
	 * </p>
	 */
	public synchronized void updateProjectCleanupProfile() {
		List<String> newLines = updateProjectSettingsFile(null, lines);
		if (!newLines.equals(lines)) {
			updated = true;
			lines = newLines;
		}
	}

	/**
	 * Updates the save action settings in the preferences file.
	 * <p>
	 * This method updates existing sp_cleanup.* keys and adds any new keys from the cleanup
	 * profile (prefixed with "sp_"). Changes are marked for writing when {@link #close()} is
	 * called.
	 * </p>
	 */
	public synchronized void updateSaveActions() {
		List<String> newLines = updateProjectSettingsFile("sp_", lines);
		if (!newLines.equals(lines)) {
			updated = true;
			lines = newLines;
		}
	}

	/**
	 * Checks if the preferences file contains a cleanup profile definition.
	 * 
	 * @return true if a cleanup_profile key is present, false otherwise
	 */
	public synchronized boolean hasCleanupProfile() {
		for (String line : lines) {
			KV kv = parseLine(line);
			if (kv.key().equals("cleanup_profile")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the preferences file has save actions enabled.
	 * 
	 * @return true if sp_cleanup.on_save_use_additional_actions is set to true, false otherwise
	 */
	public synchronized boolean hasSaveActions() {
		for (String line : lines) {
			KV kv = parseLine(line);
			if (kv.key().equals("sp_cleanup.on_save_use_additional_actions")) {
				return Boolean.parseBoolean(kv.value());
			}
		}
		return false;
	}

	private List<String> updateProjectSettingsFile(String prefix, List<String> lines) {
		List<String> updatedLines = new ArrayList<>();
		Set<String> missingKeys = new HashSet<>(cleanUpProfile.keySet());
		for (String line : lines) {
			KV kv = parseLine(line);
			if (!kv.matches(prefix)) {
				updatedLines.add(line);
				continue;
			}
			// Check if this line matches any key in the cleanup profile
			String key = kv.key(prefix);
			if (missingKeys.remove(key)) {
				updatedLines.add(kv.key() + "=" + cleanUpProfile.get(key));
			} else {
				updatedLines.add(line);
			}
		}
		// Add any keys from the profile that weren't found in the file
		for (String key : missingKeys) {
			String prefixed = prefix == null ? key : prefix + key;
			updatedLines.add(prefixed + "=" + cleanUpProfile.get(key));
		}
		return updatedLines;
	}

	/**
	 * Writes any updates back to the preferences file if changes were made.
	 * <p>
	 * This method is called automatically when using try-with-resources. Only writes to the file
	 * if {@link #updateProjectCleanupProfile()} or {@link #updateSaveActions()} were called and
	 * resulted in changes.
	 * </p>
	 * 
	 * @throws IOException
	 *             if the file cannot be written
	 */
	@Override
	public synchronized void close() throws IOException {
		if (updated) {
			// Write the updated content back to the file with explicit charset
			Files.write(prefsFile, lines, StandardCharsets.UTF_8);
		}
	}

	private static KV parseLine(String line) {
		String[] kv = line.split("=", 2);
		if (kv.length != 2) {
			return new KV(line, null);
		}
		return new KV(kv[0], kv[1]);
	}

	private static final record KV(String key, String value) {

		public boolean matches(String prefix) {
			if (value == null) {
				return false;
			}
			// For prefixed updates, only match keys that actually have the prefix
			if (prefix != null && !key.startsWith(prefix)) {
				return false;
			}
			return key(prefix).startsWith("cleanup.");
		}

		public String key(String prefix) {
			if (prefix == null || !key.startsWith(prefix)) {
				return key;
			}
			return key.substring(prefix.length());
		}

	}

}
