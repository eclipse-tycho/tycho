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
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ResolvedArtifactKey;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.MavenBundleResolver;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.helper.PluginRealmHelper;
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

	@Component
	private EclipseWorkspaceManager workspaceManager;

	@Component
	private TychoProjectManager projectManager;

	@Component
	private ApiApplicationResolver resolver;

	@Component
	private PluginRealmHelper pluginRealmHelper;

	@Component
	protected MavenBundleResolver mavenBundleResolver;

	@Component
	private ApiApplicationResolver applicationResolver;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return;
		}
		Optional<EclipseProject> eclipseProject = projectManager.getEclipseProject(project);
		if (eclipseProject.isEmpty()
				|| !eclipseProject.get().hasNature("org.eclipse.pde.api.tools.apiAnalysisNature")) {
			return;
		}

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
				log.warn("Can't resolve API baseline, API baseline check is skipped!");
				return;
			}
			Collection<Path> dependencyBundles;
			try {
				dependencyBundles = getProjectDependencies();
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
			try {
				analysisResult = eclipseFramework.execute(new ApiAnalysis(baselineBundles, dependencyBundles,
						project.getName(), fileToPath(apiFilter), fileToPath(apiPreferences),
						fileToPath(project.getBasedir()), debug, fileToPath(project.getArtifact().getFile())));
			} catch (Exception e) {
				throw new MojoExecutionException("Execute ApiApplication failed", e);
			} finally {
				eclipseFramework.close();
			}
			log.info("API Analysis finished in " + time(start) + ".");
			analysisResult.resolveErrors()
					.forEach(resolveError -> log.warn(resolveError + " analysis might be inaccurate!"));
			Map<Integer, List<IApiProblem>> problems = analysisResult.problems()
					.collect(Collectors.groupingBy(IApiProblem::getSeverity));
			List<IApiProblem> errors = problems.getOrDefault(ApiPlugin.SEVERITY_ERROR, List.of());
			List<IApiProblem> warnings = problems.getOrDefault(ApiPlugin.SEVERITY_WARNING, List.of());
			log.info(errors.size() + " API ERRORS");
			log.info(warnings.size() + " API warnings");
			for (IApiProblem problem : errors) {
				printProblem(problem, "API ERROR", log::error);
			}
			for (IApiProblem problem : warnings) {
				printProblem(problem, "API WARNING", log::warn);
			}
			if (errors.size() > 0) {
				String msg = errors.stream().map(problem -> {
					if (problem.getResourcePath() == null) {
						return problem.getMessage();
					}
					return problem.getResourcePath() + ":" + problem.getLineNumber() + " " + problem.getMessage();
				}).collect(Collectors.joining(System.lineSeparator()));
				throw new MojoFailureException("There are API errors:" + System.lineSeparator() + msg);
			}
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
			return Path.of("unkown");
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
			Optional<ArtifactKey> artifactKey = projectManager.getArtifactKey(project);
			getLog().info("Resolve API baseline for " + project.getId());
			baselineBundles = resolver.getApiBaselineBundles(
					baselines.stream().filter(repo -> repo.getUrl() != null)
							.map(repo -> new MavenRepositoryLocation(repo.getId(), URI.create(repo.getUrl()))).toList(),
					artifactKey.get());
			getLog().debug("API baseline contains " + baselineBundles.size() + " bundles (resolve takes " + time(start)
					+ ").");
		} catch (IllegalArtifactReferenceException e) {
			throw new MojoFailureException("Project specify an invalid artifact key", e);
		}
		return baselineBundles;
	}

	private String time(long start) {
		long ms = System.currentTimeMillis() - start;
		if (ms < 1000) {
			return ms + " ms";
		}
		long sec = ms / 1000;
		return sec + " s";
	}

	private Collection<Path> getProjectDependencies() throws Exception {
		Set<Path> dependencySet = new HashSet<>();
		TychoProject tychoProject = projectManager.getTychoProject(project).get();
		List<ArtifactDescriptor> dependencies = TychoProjectUtils
				.getDependencyArtifacts(DefaultReactorProject.adapt(project)).getArtifacts();
		for (ArtifactDescriptor descriptor : dependencies) {
			File location = descriptor.fetchArtifact().get();
			if (location.equals(project.getBasedir())) {
				continue;
			}
			ReactorProject reactorProject = descriptor.getMavenProject();
			if (reactorProject == null) {
				writeLocation(location, dependencySet);
			} else {
				ReactorProject otherProject = reactorProject;
				writeLocation(otherProject.getArtifact(descriptor.getClassifier()), dependencySet);
			}
		}
		if (tychoProject instanceof OsgiBundleProject bundleProject) {
			pluginRealmHelper.visitPluginExtensions(project, session, ClasspathContributor.class, cpc -> {
				List<ClasspathEntry> list = cpc.getAdditionalClasspathEntries(project, Artifact.SCOPE_COMPILE);
				if (list != null && !list.isEmpty()) {
					for (ClasspathEntry entry : list) {
						for (File locations : entry.getLocations()) {
							writeLocation(locations, dependencySet);
						}
					}
				}
			});
			// This is a hack because "org.eclipse.osgi.services" exports the annotation
			// package and might then be resolved by Tycho as a dependency, but then PDE
			// can't find the annotations here, so we always add this as a dependency
			// manually here, once "org.eclipse.osgi.services" is gone we can remove this
			// again!
			Optional<ResolvedArtifactKey> bundle = mavenBundleResolver.resolveMavenBundle(project, session, "org.osgi",
					"org.osgi.service.component.annotations", "1.3.0");
			bundle.ifPresent(key -> {
				writeLocation(key.getLocation(), dependencySet);
			});
		}
		return dependencySet;
	}

	private void writeLocation(File location, Collection<Path> consumer) {
		if (location == null) {
			return;
		}
		consumer.add(location.getAbsoluteFile().toPath());
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

	private static Path fileToPath(File file) {
		if (file != null) {
			return file.toPath();
		}
		return null;
	}
}
