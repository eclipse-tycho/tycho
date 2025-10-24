/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     Christoph Läubrich - also check for files in the classes output directory
 *******************************************************************************/

package org.eclipse.tycho.packaging;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.tycho.BuildProperties;

@Named
@Singleton
public class DefaultIncludeValidationHelper implements IncludeValidationHelper {

	@Inject
	private Logger log;

	public DefaultIncludeValidationHelper() {
	}

	public DefaultIncludeValidationHelper(Logger log) {
		this.log = log;
	}

	@Override
	public void checkBinIncludesExist(MavenProject project, BuildProperties buildProperties, boolean strict,
			String... ignoredIncludes) throws MojoExecutionException {
		checkIncludesExist("bin.includes", buildProperties.getBinIncludes(), project, strict, ignoredIncludes);
	}

	@Override
	public void checkSourceIncludesExist(MavenProject project, BuildProperties buildProperties, boolean strict)
			throws MojoExecutionException {
		checkIncludesExist("src.includes", buildProperties.getSourceIncludes(), project, strict);
	}

	private void checkIncludesExist(String buildPropertiesKey, List<String> includePatterns, MavenProject project,
			boolean strict, String... ignoredIncludes) throws MojoExecutionException {
		File baseDir = project.getBasedir();
		List<String> nonMatchingIncludes = new ArrayList<>();
		List<String> ignoreList = Arrays.asList(ignoredIncludes);
		if (includePatterns == null || includePatterns.isEmpty()) {
			String message = new File(baseDir, "build.properties").getAbsolutePath() + ": " + buildPropertiesKey
					+ " value(s) must be specified.";
			if (strict) {
				throw new MojoExecutionException(message);
			} else {
				log.warn(message);
			}
		}
		for (String includePattern : includePatterns) {
			if (ignoreList.contains(includePattern)) {
				continue;
			}
			if (checkDir(baseDir, nonMatchingIncludes, includePattern)) {
				continue;
			}
			Build build = project.getBuild();
			if (build != null) {
				String outputDirectory = build.getOutputDirectory();
				if (outputDirectory != null
						&& checkDir(new File(outputDirectory), nonMatchingIncludes, includePattern)) {
					continue;
				}
			}
			nonMatchingIncludes.add(includePattern);
		}
		if (nonMatchingIncludes.size() > 0) {
			String message = new File(baseDir, "build.properties").getAbsolutePath() + ": " + buildPropertiesKey
					+ " value(s) " + nonMatchingIncludes + " do not match any files.";
			if (strict) {
				throw new MojoExecutionException(message);
			} else {
				log.warn(message);
			}
		}
	}

	private boolean checkDir(File baseDir, List<String> nonMatchingIncludes, String includePattern) {
		if (!baseDir.isDirectory()) {
			return false;
		}
		if (new File(baseDir, includePattern).exists()) {
			return true;
		}
		// it does not exist as a file nor dir. Try if it matches any files as ant
		// pattern
		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setIncludes(new String[] { includePattern });
		scanner.setBasedir(baseDir);
		scanner.scan();
		return scanner.getIncludedFiles().length > 0;
	}

}
