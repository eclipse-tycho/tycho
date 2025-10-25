/*******************************************************************************
 * Copyright (c) 2023, 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *    SAP SE - add option to write an XML report
 *******************************************************************************/
package org.eclipse.tycho.apitools;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
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
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.exceptions.VersionBumpRequiredException;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.project.EclipseProject;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseFramework;
import org.eclipse.tycho.osgi.framework.EclipseWorkspace;
import org.eclipse.tycho.osgi.framework.EclipseWorkspaceManager;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.w3c.dom.DOMException;

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

	@Parameter(defaultValue = "true", property = "tycho.apitools.printProblems")
	private boolean printProblems;

	@Parameter(defaultValue = "true", property = "tycho.apitools.printSummary")
	private boolean printSummary;

	@Parameter(defaultValue = "false")
	private boolean failOnResolutionError;

	@Parameter(defaultValue = "true", property = "tycho.apitools.failOnError")
	private boolean failOnError;

	@Parameter(defaultValue = "false", property = "tycho.apitools.failOnWarning")
	private boolean failOnWarning;

	@Parameter(defaultValue = "false", property = "tycho.apitools.failOnVersion")
	private boolean failOnVersion;

	@Parameter(defaultValue = "false")
	private boolean parallel;

	/**
	 * The mojo by default also creates an XML report of the API problems. The
	 * format is understood by the <a href=
	 * "https://github.com/jenkinsci/warnings-ng-plugin/blob/main/doc/Documentation.md#export-your-issues-into-a-supported-format">warnings-ng
	 * Jenkins plugin</a> using the native format.
	 * <p>
	 * If you make use of this, consider setting
	 * {@code <failOnError>false</failOnError>} in order to not fail the Maven build
	 * on the first bundle with API problems, so that all the reports of all the
	 * bundles can be analyzed subsequently by external means.
	 * <p>
	 * This may be set to an empty string to skip creation of the XML report.
	 *
	 * @since 4.0.13
	 */
	@Parameter(defaultValue = "${project.build.directory}/apianalysis/report.xml", property = "tycho.apitools.report")
	private File report;

	/**
	 * Configures if the API Analysis should run as a workspace job, this ensure
	 * that no other actions are allowed to run in parallel what sometimes can
	 * result in failures to execute the api-analysis
	 */
	@Parameter(defaultValue = "true", property = "tycho.apitools.runAsJob")
	private boolean runAsJob;

	@Inject
	private EclipseWorkspaceManager workspaceManager;

	@Inject
	private TychoProjectManager projectManager;

	@Inject
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
			Collection<Path> baselineBundles = getBaselineBundles();
			if (baselineBundles.isEmpty()) {
				log.info("Skipped because no bundles in the baseline!");
				return;
			}
			Collection<Path> dependencyBundles;
			try {
				dependencyBundles = projectManager.getProjectDependencies(project);
			} catch (Exception e) {
				throw new MojoFailureException("Can't fetch dependencies!", e);
			}
			MavenRepositoryLocation repository = getRepository();
			EclipseWorkspace<?> workspace = workspaceManager.getWorkspace(repository.getURL(), this);
			EclipseApplication apiApplication = applicationResolver.getApiApplication(repository);
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
			if (report != null && !report.getPath().isEmpty()) {
				try {
					new ApiAnalysisXmlGenerator(
							projectManager.getTychoProject(project).get()
									.getArtifactKey(DefaultReactorProject.adapt(project)).getId(),
							analysisResult, report.toPath()).writeReport();
				} catch (DOMException | IOException | ParserConfigurationException | TransformerException e) {
					throw new MojoExecutionException(e);
				}
			}
			if (printSummary) {
				log.info(errors.size() + " API ERRORS");
				log.info(warnings.size() + " API warnings");
			}
			if (errors.size() > 0 && failOnError) {
				throw getApiError(errors);
			}
			if (errors.size() > 0 && failOnVersion
					&& errors.stream().anyMatch(problem -> problem.getCategory() == IApiProblem.CATEGORY_VERSION)) {
				throw getApiError(errors);
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

	private MojoFailureException getApiError(List<IApiProblem> errors) {
		String problems = errors.stream().map(problem -> {
			if (problem.getResourcePath() == null) {
				return problem.getMessage();
			}
			return problem.getResourcePath() + ":" + problem.getLineNumber() + " " + problem.getMessage();
		}).collect(Collectors.joining(System.lineSeparator()));
		String message = "There are API errors:" + System.lineSeparator() + problems;
		for (IApiProblem problem : errors) {
			if (problem.getCategory() == IApiProblem.CATEGORY_VERSION) {
				switch (problem.getKind()) {
				case IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE:
				case IApiProblem.REEXPORTED_REMOVAL_OF_REEXPORT_MAJOR_VERSION_CHANGE:
				case IApiProblem.MAJOR_VERSION_CHANGE:
					return new VersionBumpRequiredException(message, project,
							getSuggestedVersion(current -> new Version(current.getMajor() + 1, 0, 0)));
				case IApiProblem.MINOR_VERSION_CHANGE:
				case IApiProblem.MINOR_VERSION_CHANGE_EXECUTION_ENV_CHANGED:
				case IApiProblem.REEXPORTED_MINOR_VERSION_CHANGE:
					return new VersionBumpRequiredException(message, project,
							getSuggestedVersion(current -> new Version(current.getMajor(), current.getMinor() + 1, 0)));
				default:
					break;
				}
			}
		}
		return new MojoFailureException(message);
	}

	private Version getSuggestedVersion(Function<Version, Version> update) {
		try {
		Optional<ArtifactKey> key = projectManager.getArtifactKey(project);
		if (key.isPresent()) {
			String version = key.get().getVersion();
			Version current = Version.parseVersion(version);
			return update.apply(current);
		}
		} catch(RuntimeException e) {
			getLog().warn("Can't determine suggested version!");
		}
		return null;
	}

	private ApiAnalysisResult performAnalysis(Collection<Path> baselineBundles, Collection<Path> dependencyBundles,
			EclipseFramework eclipseFramework, EclipseProject eclipseProject) throws MojoExecutionException {
		try {
			ApiAnalysis analysis = new ApiAnalysis(baselineBundles, dependencyBundles, project.getName(),
					eclipseProject.getFile(fileToPath(apiFilter)), eclipseProject.getFile(fileToPath(apiPreferences)),
					fileToPath(project.getBasedir()), debug, fileToPath(project.getArtifact().getFile()),
					stringToPath(project.getBuild().getOutputDirectory()), runAsJob);
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
		try {
			Collection<TargetEnvironment> targetEnvironments = projectManager.getBaselineEnvironments(project);
			Optional<ArtifactKey> artifactKey = projectManager.getArtifactKey(project);
			getLog().info("Resolve API baseline for " + project.getId() + " with "
					+ targetEnvironments.stream().map(String::valueOf).collect(Collectors.joining(", ")));
			Collection<Path> baselineBundles = applicationResolver.getApiBaselineBundles(
					baselines.stream().filter(repo -> repo.getUrl() != null)
							.map(repo -> new MavenRepositoryLocation(repo.getId(), URI.create(repo.getUrl()))).toList(),
					artifactKey.get(), targetEnvironments);
			getLog().debug("API baseline contains " + baselineBundles.size() + " bundles (resolve takes " + time(start)
					+ ").");
			return baselineBundles;
		} catch (IllegalArtifactReferenceException e) {
			throw new MojoFailureException("Project specify an invalid artifact key", e);
		} catch (DependencyResolutionException e) {
			if (failOnResolutionError) {
				throw new MojoFailureException("Can't resolve API baseline!", e);
			} else {
				getLog().warn(
						"Can't resolve API baseline: " + Objects.requireNonNullElse(e.getMessage(), e.toString()));
				return List.of();
			}
		}
	}

	private String time(long start) {
		long ms = System.currentTimeMillis() - start;
		if (ms < 1000) {
			return ms + " ms";
		}
		long sec = ms / 1000;
		return sec + " s";
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
