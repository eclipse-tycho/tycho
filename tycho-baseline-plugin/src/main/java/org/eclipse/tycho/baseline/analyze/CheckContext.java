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
package org.eclipse.tycho.baseline.analyze;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.artifacts.ArtifactVersionProvider;

/**
 * Shared context for dependency checks, holding mutable state and services used
 * across multiple {@link DependencyChecker} instances during a single check
 * run.
 */
public class CheckContext {

	private final List<DependencyVersionProblem> dependencyProblems = new ArrayList<>();
	private final Map<Path, ClassCollection> analyzeCache = new HashMap<>();
	private final DependencyAnalyzer dependencyAnalyzer;
	private final Function<String, Optional<ClassMethods>> classResolver;
	private final List<ArtifactVersionProvider> versionProviders;
	private final MavenProject project;
	private final Log log;
	private final boolean verbose;

	/**
	 * Creates a new check context.
	 *
	 * @param dependencyAnalyzer the analyzer for resolving dependency classes
	 * @param artifacts 
	 * @param classResolver      resolver for looking up class method information
	 * @param versionProviders   providers for artifact version lookups
	 * @param project            the Maven project being checked
	 * @param log                the Maven log for diagnostic output
	 * @param verbose            whether to include detailed method signatures in
	 *                           problem reports
	 * @throws MojoFailureException 
	 */
	public CheckContext(DependencyAnalyzer dependencyAnalyzer,
			DependencyArtifacts artifacts, List<ArtifactVersionProvider> versionProviders,
			MavenProject project, Log log, boolean verbose) {
		this.dependencyAnalyzer = dependencyAnalyzer;
		this.classResolver = dependencyAnalyzer
				.createDependencyClassResolver(artifacts);
		this.versionProviders = versionProviders;
		this.project = project;
		this.log = log;
		this.verbose = verbose;
	}

	/**
	 * Adds a dependency version problem to the accumulated list.
	 *
	 * @param problem the problem to record
	 */
	public void addProblem(DependencyVersionProblem problem) {
		dependencyProblems.add(problem);
	}

	/**
	 * @return the accumulated dependency version problems
	 */
	public List<DependencyVersionProblem> getProblems() {
		return dependencyProblems;
	}

	/**
	 * Returns a cached {@link ClassCollection} for the given artifact path
	 *
	 * @param artifact the artifact path to look up
	 * @return the cached collection, or {@code null}
	 */
	public ClassCollection getClassCollection(Path artifact) {
		ClassCollection classCollection = analyzeCache.get(artifact);
		if (classCollection == null) {
			File file = artifact.toFile();
			classCollection = getDependencyAnalyzer().analyzeProvides(file, classResolver);
			analyzeCache.put(file.toPath(), classCollection);
		}
		return classCollection;
	}

	/**
	 * @return the dependency analyzer
	 */
	public DependencyAnalyzer getDependencyAnalyzer() {
		return dependencyAnalyzer;
	}

	/**
	 * @return the artifact version providers
	 */
	public List<ArtifactVersionProvider> getVersionProviders() {
		return versionProviders;
	}

	/**
	 * @return the Maven project being checked
	 */
	public MavenProject getProject() {
		return project;
	}

	/**
	 * @return the Maven log
	 */
	public Log getLog() {
		return log;
	}

	/**
	 * @return whether verbose reporting is enabled
	 */
	public boolean isVerbose() {
		return verbose;
	}
}