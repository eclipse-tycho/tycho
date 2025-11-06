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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
 * Applies Eclipse JDT code cleanup actions to Java source files in the project.
 * <p>
 * This mojo performs automated code cleanup operations on Java files using Eclipse's cleanup
 * engine. It can apply a custom cleanup profile or use the project's existing cleanup settings.
 * The cleanup operations may include formatting, organizing imports, removing unused code,
 * adding missing annotations, and other code quality improvements.
 * </p>
 * <p>
 * The mojo can optionally update the project's cleanup profile and save action settings in the
 * {@code .settings/org.eclipse.jdt.ui.prefs} file after cleanup is performed.
 * </p>
 * 
 * @see CleanupPreferencesUpdater
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
			getLog().info("Applied cleanup: " + cleanup);
		});
		builder.newLine();
		builder.newLine();
		builder.write();
		if (updateProjectCleanupProfile || updateProjectSaveActions) {
			Path settingsDir = project.getBasedir().toPath().resolve(".settings");
			Path prefsFile = settingsDir.resolve("org.eclipse.jdt.ui.prefs");
			try {
				if (Files.isRegularFile(prefsFile)) {
					try (CleanupPreferencesUpdater updater = new CleanupPreferencesUpdater(prefsFile, cleanUpProfile)) {
						if (updateProjectCleanupProfile) {
							if (updater.hasCleanupProfile()) {
								updater.updateProjectCleanupProfile();
								getLog().info("Updated cleanup profile settings in project preferences");
							} else {
								getLog().info(
										"Project preferences do not specify a cleanup profile, skipping profile update");
							}
						}
						if (updateProjectSaveActions) {
							if (updater.hasSaveActions()) {
								updater.updateSaveActions();
								getLog().info("Updated save action settings in project preferences");
							} else {
								getLog().info("Project has disabled additional save actions, skipping save action update");
							}
						}
					}
				} else {
					getLog().info("Project preferences file not found, skipping settings update");
				}
			} catch (IOException e) {
				getLog().warn("Failed to update project preferences", e);
			}
		}
	}

	@Override
	protected boolean isValid(EclipseProject eclipseProject) {
		// Cleanups can only be applied to java projects
		return eclipseProject.hasNature("org.eclipse.jdt.core.javanature");
	}

}
