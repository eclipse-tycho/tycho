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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link CleanupPreferencesUpdater}
 */
public class CleanupPreferencesUpdaterTest {

	@TempDir
	Path tempDir;

	@Test
	public void testHasCleanupProfile_whenProfileExists() throws IOException {
		// Create a preferences file with a cleanup profile
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> lines = List.of(
				"eclipse.preferences.version=1",
				"cleanup_profile=_Custom Profile",
				"cleanup.always_use_this_for_formatting=true");
		Files.write(prefsFile, lines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of("cleanup.always_use_this_for_formatting", "false");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			assertTrue(updater.hasCleanupProfile());
		}
	}

	@Test
	public void testHasCleanupProfile_whenProfileDoesNotExist() throws IOException {
		// Create a preferences file without a cleanup profile
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> lines = List.of(
				"eclipse.preferences.version=1",
				"some.other.setting=value");
		Files.write(prefsFile, lines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of("cleanup.always_use_this_for_formatting", "false");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			assertFalse(updater.hasCleanupProfile());
		}
	}

	@Test
	public void testHasSaveActions_whenEnabled() throws IOException {
		// Create a preferences file with save actions enabled
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> lines = List.of(
				"eclipse.preferences.version=1",
				"sp_cleanup.on_save_use_additional_actions=true");
		Files.write(prefsFile, lines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of("cleanup.always_use_this_for_formatting", "false");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			assertTrue(updater.hasSaveActions());
		}
	}

	@Test
	public void testHasSaveActions_whenDisabled() throws IOException {
		// Create a preferences file with save actions disabled
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> lines = List.of(
				"eclipse.preferences.version=1",
				"sp_cleanup.on_save_use_additional_actions=false");
		Files.write(prefsFile, lines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of("cleanup.always_use_this_for_formatting", "false");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			assertFalse(updater.hasSaveActions());
		}
	}

	@Test
	public void testHasSaveActions_whenNotPresent() throws IOException {
		// Create a preferences file without save actions setting
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> lines = List.of(
				"eclipse.preferences.version=1",
				"some.other.setting=value");
		Files.write(prefsFile, lines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of("cleanup.always_use_this_for_formatting", "false");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			assertFalse(updater.hasSaveActions());
		}
	}

	@Test
	public void testUpdateProjectCleanupProfile_updatesExistingValues() throws IOException {
		// Create a preferences file with existing cleanup settings
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> lines = List.of(
				"eclipse.preferences.version=1",
				"cleanup_profile=_Custom Profile",
				"cleanup.always_use_this_for_formatting=true",
				"cleanup.add_missing_annotations=false");
		Files.write(prefsFile, lines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of(
				"cleanup.always_use_this_for_formatting", "false",
				"cleanup.add_missing_annotations", "true");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			updater.updateProjectCleanupProfile();
		}

		List<String> result = Files.readAllLines(prefsFile, StandardCharsets.UTF_8);
		assertTrue(result.contains("cleanup.always_use_this_for_formatting=false"));
		assertTrue(result.contains("cleanup.add_missing_annotations=true"));
		assertTrue(result.contains("eclipse.preferences.version=1"));
		assertTrue(result.contains("cleanup_profile=_Custom Profile"));
	}

	@Test
	public void testUpdateProjectCleanupProfile_addsNewKeys() throws IOException {
		// Create a preferences file with minimal cleanup settings
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> lines = List.of(
				"eclipse.preferences.version=1",
				"cleanup_profile=_Custom Profile");
		Files.write(prefsFile, lines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of(
				"cleanup.always_use_this_for_formatting", "false",
				"cleanup.add_missing_annotations", "true");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			updater.updateProjectCleanupProfile();
		}

		List<String> result = Files.readAllLines(prefsFile, StandardCharsets.UTF_8);
		assertTrue(result.contains("cleanup.always_use_this_for_formatting=false"));
		assertTrue(result.contains("cleanup.add_missing_annotations=true"));
		assertTrue(result.contains("eclipse.preferences.version=1"));
		assertTrue(result.contains("cleanup_profile=_Custom Profile"));
	}

	@Test
	public void testUpdateProjectCleanupProfile_preservesNonCleanupSettings() throws IOException {
		// Create a preferences file with mixed settings
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> lines = List.of(
				"eclipse.preferences.version=1",
				"cleanup_profile=_Custom Profile",
				"cleanup.always_use_this_for_formatting=true",
				"formatter_profile=_Custom Formatter",
				"other.setting=value");
		Files.write(prefsFile, lines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of(
				"cleanup.always_use_this_for_formatting", "false");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			updater.updateProjectCleanupProfile();
		}

		List<String> result = Files.readAllLines(prefsFile, StandardCharsets.UTF_8);
		assertTrue(result.contains("cleanup.always_use_this_for_formatting=false"));
		assertTrue(result.contains("formatter_profile=_Custom Formatter"));
		assertTrue(result.contains("other.setting=value"));
		assertTrue(result.contains("eclipse.preferences.version=1"));
	}

	@Test
	public void testUpdateSaveActions_updatesExistingValues() throws IOException {
		// Create a preferences file with existing save actions
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> lines = List.of(
				"eclipse.preferences.version=1",
				"sp_cleanup.on_save_use_additional_actions=true",
				"sp_cleanup.always_use_this_for_formatting=true",
				"sp_cleanup.add_missing_annotations=false");
		Files.write(prefsFile, lines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of(
				"cleanup.always_use_this_for_formatting", "false",
				"cleanup.add_missing_annotations", "true");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			updater.updateSaveActions();
		}

		List<String> result = Files.readAllLines(prefsFile, StandardCharsets.UTF_8);
		assertTrue(result.contains("sp_cleanup.always_use_this_for_formatting=false"));
		assertTrue(result.contains("sp_cleanup.add_missing_annotations=true"));
		assertTrue(result.contains("sp_cleanup.on_save_use_additional_actions=true"));
	}

	@Test
	public void testUpdateSaveActions_addsNewKeys() throws IOException {
		// Create a preferences file with minimal save actions
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> lines = List.of(
				"eclipse.preferences.version=1",
				"sp_cleanup.on_save_use_additional_actions=true");
		Files.write(prefsFile, lines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of(
				"cleanup.always_use_this_for_formatting", "false",
				"cleanup.add_missing_annotations", "true");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			updater.updateSaveActions();
		}

		List<String> result = Files.readAllLines(prefsFile, StandardCharsets.UTF_8);
		assertTrue(result.contains("sp_cleanup.always_use_this_for_formatting=false"));
		assertTrue(result.contains("sp_cleanup.add_missing_annotations=true"));
		assertTrue(result.contains("sp_cleanup.on_save_use_additional_actions=true"));
	}

	@Test
	public void testClose_doesNotWriteIfNotUpdated() throws IOException, InterruptedException {
		// Create a preferences file
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> originalLines = List.of(
				"eclipse.preferences.version=1",
				"cleanup_profile=_Custom Profile");
		Files.write(prefsFile, originalLines, StandardCharsets.UTF_8);
		long originalModifiedTime = Files.getLastModifiedTime(prefsFile).toMillis();

		Map<String, String> cleanUpProfile = Map.of("cleanup.new_setting", "value");

		// Wait a bit to ensure modification time would differ
		Thread.sleep(10);

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			// Don't call any update methods
		}

		long afterModifiedTime = Files.getLastModifiedTime(prefsFile).toMillis();
		assertEquals(originalModifiedTime, afterModifiedTime, "File should not be modified if no updates were made");
	}

	@Test
	public void testClose_writesIfUpdated() throws IOException {
		// Create a preferences file
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> originalLines = List.of(
				"eclipse.preferences.version=1",
				"cleanup_profile=_Custom Profile",
				"cleanup.always_use_this_for_formatting=true");
		Files.write(prefsFile, originalLines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of("cleanup.always_use_this_for_formatting", "false");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			updater.updateProjectCleanupProfile();
		}

		List<String> result = Files.readAllLines(prefsFile, StandardCharsets.UTF_8);
		assertTrue(result.contains("cleanup.always_use_this_for_formatting=false"));
	}

	@Test
	public void testMultipleUpdates() throws IOException {
		// Create a preferences file with both cleanup profile and save actions
		Path prefsFile = tempDir.resolve("org.eclipse.jdt.ui.prefs");
		List<String> lines = List.of(
				"eclipse.preferences.version=1",
				"cleanup_profile=_Custom Profile",
				"sp_cleanup.on_save_use_additional_actions=true",
				"cleanup.always_use_this_for_formatting=true",
				"sp_cleanup.add_missing_annotations=false");
		Files.write(prefsFile, lines, StandardCharsets.UTF_8);

		Map<String, String> cleanUpProfile = Map.of(
				"cleanup.always_use_this_for_formatting", "false",
				"cleanup.add_missing_annotations", "true");

		try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
			updater.updateProjectCleanupProfile();
			updater.updateSaveActions();
		}

		List<String> result = Files.readAllLines(prefsFile, StandardCharsets.UTF_8);
		assertTrue(result.contains("cleanup.always_use_this_for_formatting=false"));
		assertTrue(result.contains("cleanup.add_missing_annotations=true"));
		assertTrue(result.contains("sp_cleanup.always_use_this_for_formatting=false"));
		assertTrue(result.contains("sp_cleanup.add_missing_annotations=true"));
	}
}
