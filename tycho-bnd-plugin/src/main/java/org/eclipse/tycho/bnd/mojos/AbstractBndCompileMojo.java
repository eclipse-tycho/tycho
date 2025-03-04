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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.bnd.mojos;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.bnd.CopyMapping;

import aQute.bnd.build.Project;

public abstract class AbstractBndCompileMojo extends AbstractBndProjectMojo {

	private static final Set<String> MATCH_ALL = Collections.singleton("**/*");

	/**
	 * Whether all resources in the source folders should be copied to
	 * ${project.build.outputDirectory}.
	 * 
	 * <code>true</code> (default) means that all resources are copied from the
	 * source folders to <code>${project.build.outputDirectory}</code>.
	 * 
	 * <code>false</code> means that no resources are copied from the source folders
	 * to <code>${project.build.outputDirectory}</code>.
	 * 
	 * Set this to <code>false</code> in case you want to keep resources separate
	 * from java files in <code>src/main/resources</code> and handle them using
	 * <a href="https://maven.apache.org/plugins/maven-resources-plugin/">
	 * maven-resources-plugin</a> (e.g. for <a href=
	 * "https://maven.apache.org/plugins/maven-resources-plugin/examples/filter.html">resource
	 * filtering<a/>.
	 * 
	 */
	@Parameter(defaultValue = "true")
	private boolean copyResources;

	/**
	 * A list of exclusion filters for non-java resource files which should not be
	 * copied to the output directory.
	 */
	@Parameter
	private Set<String> excludeResources = new HashSet<>();

	/*
	 * mimics the behavior of the compile task which by default copies all
	 * (non-java) resource files in source directories into the target folder
	 */
	protected void doCopyResources(Project project) throws Exception {
		if (!copyResources) {
			return;
		}
		Collection<File> compileSourceRoots = getSourcePath(project);
		for (File sourceRootFile : compileSourceRoots) {
			if (!sourceRootFile.isDirectory()) {
				getLog().warn("Source directory " + sourceRootFile + " does not exist");
				continue;
			}

			Set<String> excludes = new HashSet<>();
			excludes.addAll(excludeResources);
			excludes.add("**/*.java");
			// keep ignoring the following files after
			// https://github.com/codehaus-plexus/plexus-utils/pull/174
			excludes.add("**/.gitignore");
			excludes.add("**/.gitattributes");
			StaleSourceScanner scanner = new StaleSourceScanner(0L, MATCH_ALL, excludes);
			CopyMapping copyMapping = new CopyMapping();
			scanner.addSourceMapping(copyMapping);
			try {
				scanner.getIncludedSources(sourceRootFile, getOutput(project));
				for (CopyMapping.SourceTargetPair sourceTargetPair : copyMapping.getSourceTargetPairs()) {
					FileUtils.copyFile(new File(sourceRootFile, sourceTargetPair.source), sourceTargetPair.target);
				}
			} catch (InclusionScanException e) {
				throw new MojoExecutionException("Exception while scanning for resource files in " + sourceRootFile, e);
			} catch (IOException e) {
				throw new MojoExecutionException(
						"Exception copying resource files from " + sourceRootFile + " to " + getOutput(project), e);
			}
		}
	}

	protected abstract Collection<File> getSourcePath(Project project) throws Exception;

	protected abstract File getOutput(Project project) throws Exception;
}
