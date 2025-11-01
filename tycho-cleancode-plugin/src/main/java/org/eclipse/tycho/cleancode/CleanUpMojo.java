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
import java.util.HashMap;
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
	@Parameter(property = "updateProjectSettings")
	private boolean updateProjectSettings;

	/**
	 * If set to <code>true</code>, the settings file
	 * ${project.basedir}/.settings/org.eclipse.jdt.ui.prefs will be read and
	 * cleanup settings will be extracted. These settings will override the
	 * mojo configured cleanUpProfile. If the file is not present, the build
	 * will not fail.
	 */
	@Parameter(defaultValue = "true")
	private boolean useProjectSettings;

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
		Map<String, String> effectiveProfile = getEffectiveCleanUpProfile();
		return new CleanUp(project.getBasedir().toPath(), debug, effectiveProfile, applyCleanupsIndividually,
				getIgnores());
	}

	/**
	 * Gets the effective cleanup profile by merging project settings with mojo configuration.
	 * If useProjectSettings is true, project settings override mojo configuration.
	 * 
	 * @return the effective cleanup profile map, or null if no settings are configured
	 */
	private Map<String, String> getEffectiveCleanUpProfile() {
		Map<String, String> effectiveProfile = new HashMap<>();
		
		// Start with mojo configured profile if present
		if (cleanUpProfile != null && !cleanUpProfile.isEmpty()) {
			effectiveProfile.putAll(cleanUpProfile);
		}
		
		// Override with project settings if enabled
		if (useProjectSettings) {
			Path prefsFile = project.getBasedir().toPath().resolve(".settings").resolve("org.eclipse.jdt.ui.prefs");
			
			if (Files.isRegularFile(prefsFile)) {
				try {
					Map<String, String> projectSettings = loadCleanupSettingsFromPrefsFile(prefsFile);
					effectiveProfile.putAll(projectSettings);
					getLog().debug("Loaded " + projectSettings.size() + " cleanup settings from " + prefsFile);
				} catch (IOException e) {
					getLog().debug("Parameter 'useProjectSettings' is set to true, but could not read preferences file '" 
							+ prefsFile + "': " + e.getMessage());
				}
			} else {
				getLog().debug("Parameter 'useProjectSettings' is set to true, but preferences file '" + prefsFile
						+ "' could not be found");
			}
		}
		
		return effectiveProfile.isEmpty() ? null : effectiveProfile;
	}

	/**
	 * Loads cleanup settings from the Eclipse preferences file.
	 * Extracts all keys starting with "cleanup." or "sp_cleanup."
	 * 
	 * @param prefsFile the preferences file path
	 * @return map of cleanup settings
	 * @throws IOException if the file cannot be read
	 */
	private Map<String, String> loadCleanupSettingsFromPrefsFile(Path prefsFile) throws IOException {
		Map<String, String> cleanupSettings = new HashMap<>();
		List<String> lines = Files.readAllLines(prefsFile, StandardCharsets.UTF_8);
		
		for (String line : lines) {
			line = line.trim();
			// Skip empty lines and comments
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			
			// Check if line starts with cleanup-related prefixes
			if (line.startsWith("cleanup.") || line.startsWith("sp_cleanup.")) {
				String[] kv = line.split("=", 2);
				if (kv.length == 2) {
					String key = kv[0].trim();
					String value = kv[1].trim();
					cleanupSettings.put(key, value);
				}
			}
		}
		
		return cleanupSettings;
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
		if (updateProjectSettings) {
			try {
				updateProjectSettingsFile();
			} catch (IOException e) {
				getLog().warn("Can't update project settings", e);
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
	 * @throws IOException
	 */
	private void updateProjectSettingsFile() throws IOException {

		Path settingsDir = project.getBasedir().toPath().resolve(".settings");
		Path prefsFile = settingsDir.resolve("org.eclipse.jdt.ui.prefs");

		if (!Files.isRegularFile(prefsFile)) {
			return;
		}

		// Read all lines from the file with explicit charset
		List<String> lines = Files.readAllLines(prefsFile, StandardCharsets.UTF_8);
		List<String> updatedLines = new ArrayList<>();
		Set<String> missingKeys = new HashSet<>(cleanUpProfile.keySet());

		// Process existing lines
		for (String line : lines) {
			boolean updated = false;
			// Skip comments and empty lines - keep them as-is
			if (!line.startsWith("cleanup.")) {
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
				updatedLines.add(key + "=" + cleanUpProfile.get(key));
			} else {
				updatedLines.add(line);
			}
		}
		// Add any keys from the profile that weren't found in the file
		for (String key : missingKeys) {
			updatedLines.add(key + "=" + cleanUpProfile.get(key));
		}

		// Write the updated content back to the file with explicit charset
		Files.write(prefsFile, updatedLines, StandardCharsets.UTF_8);
	}

}
