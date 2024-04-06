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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.apitools;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.EcJLogFileEnhancer;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.EcJLogFileEnhancer.Source;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.project.EclipseProject;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseFramework;
import org.eclipse.tycho.osgi.framework.EclipseWorkspace;
import org.eclipse.tycho.osgi.framework.EclipseWorkspaceManager;
import org.osgi.framework.BundleException;

/**
 * Performs a PDE-API Tools analysis of this project.
 */
@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ApiAnalysisMojo extends AbstractMojo {

	static final String BUNDLE_APP = "org.eclipse.equinox.app";

	static final String BUNDLE_SCR = "org.apache.felix.scr";

	static final String BUNDLE_CORE = "org.eclipse.core.runtime";

	@Parameter(property = "plugin.artifacts")
	protected List<Artifact> pluginArtifacts;

	@Parameter(property = "project", readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "eclipse-plugin")
	private Set<String> supportedPackagingTypes;

	@Parameter(defaultValue = "false", property = "tycho.apitools.verify.skip")
	private boolean skip;

	@Parameter(defaultValue = "false", property = "tycho.apitools.debug")
	private boolean debug;

	@Parameter(defaultValue = "true", property = "tycho.apitools.verify.skipIfReplaced")
	private boolean skipIfReplaced;

	@Parameter(property = "baselines", name = "baselines")
	private List<Repository> baselines;

	@Parameter()
	private Repository apiToolsRepository;

	@Parameter(property = "session", readonly = true, required = true)
	private MavenSession session;

	@Parameter
	private Map<String, String> properties;

	@Parameter(defaultValue = "${project.basedir}/.settings/" + IApiCoreConstants.API_FILTERS_XML_NAME)
	private File apiFilter;

	@Parameter(defaultValue = "${project.basedir}/.settings/org.eclipse.pde.api.tools.prefs")
	private File apiPreferences;

	/**
	 * If given a folder, enhances the ECJ compiler logs with API errors so it can
	 * be analyzed by tools understanding that format
	 */
	@Parameter(defaultValue = "${project.build.directory}/compile-logs")
	private File logDirectory;

	@Parameter(defaultValue = "true")
	private boolean printProblems;

	@Parameter(defaultValue = "true")
	private boolean printSummary;

	@Parameter(defaultValue = "false")
	private boolean failOnResolutionError;

	@Parameter(defaultValue = "true")
	private boolean failOnError;

	@Parameter(defaultValue = "false")
	private boolean failOnWarning;

	@Parameter(defaultValue = "false")
	private boolean parallel;

	@Parameter(defaultValue = "false")
	private boolean enhanceLogs;

	@Component
	private EclipseWorkspaceManager workspaceManager;

	@Component
	private TychoProjectManager projectManager;

	@Component
	private ApiApplicationResolver applicationResolver;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return;
		}
		Optional<EclipseProject> eclipseProjectValue = projectManager.getEclipseProject(project);
		if (eclipseProjectValue.isEmpty() || !eclipseProjectValue.get().hasNature(ApiPlugin.NATURE_ID)) {
			return;
		}
		EclipseProject eclipseProject = eclipseProjectValue.get();
		if (supportedPackagingTypes.contains(project.getPackaging())) {
			Log log = getLog();
			if (skipIfReplaced && wasReplaced()) {
				log.info("Skipped because main artifact was replaced with baseline!");
				return;
			}
			long start = System.currentTimeMillis();
			Collection<Path> baselineBundles;
			try {
				baselineBundles = getBaselineBundles();
			} catch (DependencyResolutionException e) {
				if (failOnResolutionError) {
					throw new MojoFailureException("Can't resolve API baseline!", e);
				} else {
					log.warn("Can't resolve API baseline, API baseline check is skipped!");
					return;
				}
			}
			Collection<Path> dependencyBundles;
			try {
				dependencyBundles = projectManager.getProjectDependencies(project);
			} catch (Exception e) {
				throw new MojoFailureException("Can't fetch dependencies!", e);
			}
			EclipseWorkspace<ApiAppKey> workspace = getWorkspace();
			EclipseApplication apiApplication = applicationResolver.getApiApplication(workspace.getKey().repository);
			EclipseFramework eclipseFramework;
			try {
				eclipseFramework = apiApplication.startFramework(workspace, List.of());
			} catch (BundleException e) {
				throw new MojoFailureException("Start Framework failed!", e);
			}
			ApiAnalysisResult analysisResult;
			if (parallel) {
				analysisResult = performAnalysis(baselineBundles, dependencyBundles, eclipseFramework, eclipseProject);
			} else {
				synchronized (ApiAnalysisMojo.class) {
					// due to
					// https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues/3885#note_1266412 we
					// can not execute more than one analysis without excessive memory consumption
					// unless this is fixed it is safer to only run one analysis at a time
					analysisResult = performAnalysis(baselineBundles, dependencyBundles, eclipseFramework,
							eclipseProject);
				}
			}
			log.info("API Analysis finished in " + time(start) + ".");
			analysisResult.resolveErrors()
					.forEach(resolveError -> log.warn(resolveError + " analysis might be inaccurate!"));
			Map<Integer, List<IApiProblem>> problems = analysisResult.problems()
					.collect(Collectors.groupingBy(IApiProblem::getSeverity));
			List<IApiProblem> errors = problems.getOrDefault(ApiPlugin.SEVERITY_ERROR, List.of());
			List<IApiProblem> warnings = problems.getOrDefault(ApiPlugin.SEVERITY_WARNING, List.of());
			if (printProblems) {
				for (IApiProblem problem : errors) {
					printProblem(problem, "API ERROR", log::error);
				}
				for (IApiProblem problem : warnings) {
					printProblem(problem, "API WARNING", log::warn);
				}
			}
			if (enhanceLogs && logDirectory != null && logDirectory.isDirectory()) {
				try {
					Map<String, List<IApiProblem>> problemsMap = analysisResult.problems().collect(
							Collectors.groupingBy(problem -> Objects.requireNonNullElse(problem.getResourcePath(),
									"no-path-" + System.identityHashCode(problem))));
					if (!problemsMap.isEmpty()) {
						try (EcJLogFileEnhancer enhancer = EcJLogFileEnhancer.create(logDirectory)) {
							enhanceLog(enhancer, problemsMap);
						}
						return;
					}
				} catch (IOException e) {
					log.warn("Can't enhance logs in directory " + logDirectory);
				}
			}
			if (printSummary) {
				log.info(errors.size() + " API ERRORS");
				log.info(warnings.size() + " API warnings");
			}
			if (errors.size() > 0 && failOnError) {
				String msg = errors.stream().map(problem -> {
					if (problem.getResourcePath() == null) {
						return problem.getMessage();
					}
					return problem.getResourcePath() + ":" + problem.getLineNumber() + " " + problem.getMessage();
				}).collect(Collectors.joining(System.lineSeparator()));
				throw new MojoFailureException("There are API errors:" + System.lineSeparator() + msg);
			}
			if (warnings.size() > 0 && failOnWarning) {
				String msg = warnings.stream().map(problem -> {
					if (problem.getResourcePath() == null) {
						return problem.getMessage();
					}
					return problem.getResourcePath() + ":" + problem.getLineNumber() + " " + problem.getMessage();
				}).collect(Collectors.joining(System.lineSeparator()));
				throw new MojoFailureException("There are API warnings:" + System.lineSeparator() + msg);
			}
		}
	}

	private void enhanceLog(EcJLogFileEnhancer enhancer, Map<String, List<IApiProblem>> problemsMap) {
		Log log = getLog();
		int notMapped = 0;
		for (Entry<String, List<IApiProblem>> problemEntry : problemsMap.entrySet()) {
			String path = problemEntry.getKey();
			List<IApiProblem> problemsList = problemEntry.getValue();
			List<Source> list = enhancer.sources().filter(source -> {
				String pathAttribute = source.getPath();
				return pathAttribute != null && !pathAttribute.isEmpty() && pathAttribute.endsWith(path);
			}).toList();
			if (list.isEmpty()) {
				for (IApiProblem notfound : problemsList) {
					notMapped++;
					if (printProblems) {
						// it was already printed before...
						continue;
					}
					if (ApiPlugin.SEVERITY_ERROR == notfound.getSeverity()) {
						printProblem(notfound, "API ERROR", log::error);
					} else if (ApiPlugin.SEVERITY_WARNING == notfound.getSeverity()) {
						printProblem(notfound, "API WARNING", log::warn);
					}
				}
			} else {
				Map<Integer, List<IApiProblem>> problemsBySeverity = problemsList.stream()
						.collect(Collectors.groupingBy(IApiProblem::getSeverity));
				List<IApiProblem> errors = problemsBySeverity.getOrDefault(ApiPlugin.SEVERITY_ERROR, List.of());
				List<IApiProblem> warnings = problemsBySeverity.getOrDefault(ApiPlugin.SEVERITY_WARNING, List.of());
				for (Source sourceEntry : list) {
					for (IApiProblem problem : warnings) {
						sourceEntry.addProblem(EcJLogFileEnhancer.SEVERITY_WARNING, problem.getLineNumber(),
								problem.getCharStart(), problem.getCharEnd(), problem.getCategory(), problem.getId(),
								problem.getMessage());
					}
					for (IApiProblem problem : errors) {
						sourceEntry.addProblem(EcJLogFileEnhancer.SEVERITY_ERROR, problem.getLineNumber(),
								problem.getCharStart(), problem.getCharEnd(), problem.getCategory(), problem.getId(),
								problem.getMessage());
					}
				}

			}
		}
		if (notMapped > 0) {
			log.warn(notMapped + " API problems can't be mapped to the compiler log!");
		}

	}

	private ApiAnalysisResult performAnalysis(Collection<Path> baselineBundles, Collection<Path> dependencyBundles,
			EclipseFramework eclipseFramework, EclipseProject eclipseProject) throws MojoExecutionException {
		try {
			ApiAnalysis analysis = new ApiAnalysis(baselineBundles, dependencyBundles, project.getName(),
					eclipseProject.getFile(fileToPath(apiFilter)), eclipseProject.getFile(fileToPath(apiPreferences)),
					fileToPath(project.getBasedir()), debug, fileToPath(project.getArtifact().getFile()),
					stringToPath(project.getBuild().getOutputDirectory()));
			return eclipseFramework.execute(analysis);
		} catch (Exception e) {
			throw new MojoExecutionException("Execute ApiApplication failed", e);
		} finally {
			eclipseFramework.close();
		}
	}

	private void printProblem(IApiProblem problem, String type, Consumer<CharSequence> consumer) {
		Path path = getFullPath(problem);
		String file = path.getFileName().toString();
		int lineNumber = problem.getLineNumber();
		String message = problem.getMessage().trim();
		consumer.accept(
				String.format("[%s] File %s at line %d: %s (location: %s)", type, file, lineNumber, message, path));

	}

	private Path getFullPath(IApiProblem problem) {
		String path = problem.getResourcePath();
		if (path == null) {
			return Path.of("unknown");
		}
		return project.getBasedir().toPath().resolve(path);
	}

	private EclipseWorkspace<ApiAppKey> getWorkspace() {
		return workspaceManager.getWorkspace(new ApiAppKey(getRepository()));
	}

	private boolean wasReplaced() {
		if (DefaultReactorProject.adapt(project)
				.getContextValue(TychoConstants.KEY_BASELINE_REPLACE_ARTIFACT_MAIN) instanceof Boolean replaced) {
			return replaced;
		}
		return false;
	}

	private MavenRepositoryLocation getRepository() {
		if (apiToolsRepository == null || apiToolsRepository.getUrl() == null) {
			return new MavenRepositoryLocation(null, URI.create(TychoConstants.ECLIPSE_LATEST));
		}
		return new MavenRepositoryLocation(apiToolsRepository.getId(), URI.create(apiToolsRepository.getUrl()));
	}

	private Collection<Path> getBaselineBundles() throws MojoFailureException {
		long start = System.currentTimeMillis();
		Collection<Path> baselineBundles;
		try {
			Collection<TargetEnvironment> targetEnvironments = getBaselineEnvironments();
			Optional<ArtifactKey> artifactKey = projectManager.getArtifactKey(project);
			getLog().info("Resolve API baseline for " + project.getId() + " with "
					+ targetEnvironments.stream().map(String::valueOf).collect(Collectors.joining(", ")));
			baselineBundles = applicationResolver.getApiBaselineBundles(
					baselines.stream().filter(repo -> repo.getUrl() != null)
							.map(repo -> new MavenRepositoryLocation(repo.getId(), URI.create(repo.getUrl()))).toList(),
					artifactKey.get(), targetEnvironments);
			getLog().debug("API baseline contains " + baselineBundles.size() + " bundles (resolve takes " + time(start)
					+ ").");
		} catch (IllegalArtifactReferenceException e) {
			throw new MojoFailureException("Project specify an invalid artifact key", e);
		}
		return baselineBundles;
	}

	/**
	 * This method selected the a target environment best suited for the current
	 * baseline, if it is a valid choice the running target is used (e.g. linux on
	 * linux host, windows on windows hosts and so on), if such environment is not
	 * available it is using the configured ones form the project as is.
	 * 
	 * @return the chosen {@link TargetEnvironment}s
	 */
	private Collection<TargetEnvironment> getBaselineEnvironments() {
		Collection<TargetEnvironment> targetEnvironments = projectManager.getTargetEnvironments(project);
		TargetEnvironment runningEnvironment = TargetEnvironment.getRunningEnvironment();
		for (TargetEnvironment targetEnvironment : targetEnvironments) {
			if (targetEnvironment.equals(runningEnvironment)) {
				return List.of(targetEnvironment);
			}
		}
		return targetEnvironments;
	}

	private String time(long start) {
		long ms = System.currentTimeMillis() - start;
		if (ms < 1000) {
			return ms + " ms";
		}
		long sec = ms / 1000;
		return sec + " s";
	}

	private static final class ApiAppKey {

		private URI key;
		private MavenRepositoryLocation repository;

		public ApiAppKey(MavenRepositoryLocation repository) {
			this.repository = repository;
			key = Objects.requireNonNull(repository.getURL()).normalize();
		}

		@Override
		public int hashCode() {
			return Objects.hash(key);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ApiAppKey other = (ApiAppKey) obj;
			return Objects.equals(key, other.key);
		}

	}

	private static Path stringToPath(String file) {
		if (file == null) {
			return null;
		}
		return Path.of(file);
	}

	private static Path fileToPath(File file) {
		if (file != null) {
			return file.toPath();
		}
		return null;
	}
}
