/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.core.MarkdownBuilder;
import org.eclipse.tycho.eclipsebuild.AbstractEclipseBuildMojo;
import org.eclipse.tycho.model.project.EclipseProject;

/**
 * This mojo allows to perform eclipse cleanup action
 */
@Mojo(name = "cleanup", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CleanUpMojo extends AbstractEclipseBuildMojo<CleanupResult> {

	@Parameter(defaultValue = "${project.build.directory}/cleanups.md", property = "tycho.cleanup.report")
	private File reportFileName;

	/**
	 * Defines key value pairs of a cleanup profile, if not defined will use the
	 * project defaults
	 */
	@Parameter
	private Map<String, String> cleanUpProfile;

	@Parameter
	private boolean applyCleanupsIndividually;

	/**
	 * Specifies patterns of files that should be excluded
	 */
	@Parameter
	private List<String> ignores;

	/**
	 * If enabled, the cleanup profile settings will be written to the project's
	 * org.eclipse.jdt.ui.prefs file after cleanup
	 */
	@Parameter(property = "updateProjectCleanupProfile")
	private boolean updateProjectCleanupProfile;

	/**
	 * If enabled, the save action cleanup settings will be written to the project's
	 * org.eclipse.jdt.ui.prefs file after cleanup. Only updates if
	 * sp_cleanup.on_save_use_additional_actions=true is set in the file.
	 */
	@Parameter(property = "updateProjectSaveActions")
	private boolean updateProjectSaveActions;

	@Override
	protected String[] getRequireBundles() {
		return new String[] { "org.eclipse.jdt.ui" };
	}

	@Override
	protected String getName() {
		return "Perform Cleanup";
	}

	@Override
	protected CleanUp createExecutable() {
		return new CleanUp(project.getBasedir().toPath(), debug, cleanUpProfile, applyCleanupsIndividually,
				getIgnores());
	}

	private List<Pattern> getIgnores() {
		if (ignores == null || ignores.isEmpty()) {
			return null;
		}
		return ignores.stream().map(Pattern::compile).toList();
	}

	@Override
	protected void handleResult(CleanupResult result) throws MojoFailureException {
		if (result.isEmpty()) {
			return;
		}
		MarkdownBuilder builder = new MarkdownBuilder(reportFileName);
		builder.h3("The following cleanups were applied:");
		result.cleanups().forEach(cleanup -> {
			builder.addListItem(cleanup);
			getLog().info("CleanUp: " + cleanup);
		});
		builder.newLine();
		builder.newLine();
		builder.write();
		if (updateProjectCleanupProfile) {
			try {
				updateProjectSettingsFile("cleanup.");
			} catch (IOException e) {
				getLog().warn("Can't update project cleanup profile settings", e);
			}
		}
		if (updateProjectSaveActions) {
			try {
				updateProjectSettingsFile("sp_cleanup.");
			} catch (IOException e) {
				getLog().warn("Can't update project save action settings", e);
			}
		}
	}

	@Override
	protected boolean isValid(EclipseProject eclipseProject) {
		// Cleanups can only be applied to java projects
		return eclipseProject.hasNature("org.eclipse.jdt.core.javanature");
	}

	/**
	 * Updates the org.eclipse.jdt.ui.prefs file with the cleanup profile settings
	 * 
	 * @param prefix the prefix to use for filtering keys (e.g., "cleanup." or "sp_cleanup.")
	 * @throws IOException
	 */
	private void updateProjectSettingsFile(String prefix) throws IOException {

		Path settingsDir = project.getBasedir().toPath().resolve(".settings");
		Path prefsFile = settingsDir.resolve("org.eclipse.jdt.ui.prefs");

		if (!Files.isRegularFile(prefsFile)) {
			return;
		}

		// Read all lines from the file with explicit charset
		List<String> lines = Files.readAllLines(prefsFile, StandardCharsets.UTF_8);
		
		// For cleanup profile (cleanup.), check if cleanup_profile is defined
		if ("cleanup.".equals(prefix)) {
			boolean cleanupProfileDefined = false;
			for (String line : lines) {
				if (line.startsWith("cleanup_profile=")) {
					cleanupProfileDefined = true;
					break;
				}
			}
			if (!cleanupProfileDefined) {
				getLog().info("Skipping cleanup profile settings update: cleanup_profile is not defined");
				return;
			}
		}
		
		// For save actions (sp_cleanup.), check if on_save_use_additional_actions is true
		if ("sp_cleanup.".equals(prefix)) {
			boolean saveActionsEnabled = false;
			for (String line : lines) {
				if (line.startsWith("sp_cleanup.on_save_use_additional_actions=")) {
					String[] kv = line.split("=", 2);
					if (kv.length == 2 && "true".equals(kv[1].trim())) {
						saveActionsEnabled = true;
						break;
					}
				}
			}
			if (!saveActionsEnabled) {
				getLog().info("Skipping save action settings update: sp_cleanup.on_save_use_additional_actions is not true");
				return;
			}
		}
		
		List<String> updatedLines = new ArrayList<>();
		Set<String> missingKeys = new HashSet<>();
		
		// Filter cleanUpProfile keys based on prefix and prepare missing keys set
		// For save actions, also accept keys without "sp_" prefix and convert them
		for (String key : cleanUpProfile.keySet()) {
			if (key.startsWith(prefix)) {
				missingKeys.add(key);
			} else if ("sp_cleanup.".equals(prefix) && key.startsWith("cleanup.") && !key.startsWith("sp_cleanup.")) {
				// For save actions, convert cleanup.* to sp_cleanup.*
				String spKey = "sp_" + key;
				missingKeys.add(spKey);
			}
		}

		// Process existing lines
		for (String line : lines) {
			// Skip comments and empty lines - keep them as-is
			if (!line.startsWith(prefix)) {
				updatedLines.add(line);
				continue;
			}
			String[] kv = line.split("=", 2);
			if (kv.length != 2) {
				updatedLines.add(line);
				continue;
			}
			String key = kv[0].trim();
			// Check if this line matches any key in the cleanup profile
			if (missingKeys.remove(key)) {
				// For save actions, if the original key was cleanup.*, use it, otherwise use the key as-is
				String originalKey = key;
				if ("sp_cleanup.".equals(prefix) && key.startsWith("sp_") && cleanUpProfile.containsKey(key.substring(3))) {
					// Original key was cleanup.* without sp_ prefix
					originalKey = key.substring(3); // Remove "sp_"
				}
				String value = cleanUpProfile.containsKey(key) ? cleanUpProfile.get(key) : cleanUpProfile.get(originalKey);
				updatedLines.add(key + "=" + value);
			} else {
				updatedLines.add(line);
			}
		}
		// Add any keys from the profile that weren't found in the file
		for (String key : missingKeys) {
			// For save actions, if the key already has sp_ prefix, use cleanUpProfile.get(key), 
			// otherwise it's a converted key so get value from original cleanup.* key
			String value;
			if ("sp_cleanup.".equals(prefix) && key.startsWith("sp_") && !cleanUpProfile.containsKey(key)) {
				// This was converted from cleanup.* to sp_cleanup.*, get value from original
				String originalKey = key.substring(3); // Remove "sp_"
				value = cleanUpProfile.get(originalKey);
			} else {
				value = cleanUpProfile.get(key);
			}
			updatedLines.add(key + "=" + value);
		}

		// Write the updated content back to the file with explicit charset
		Files.write(prefsFile, updatedLines, StandardCharsets.UTF_8);
	}

}
