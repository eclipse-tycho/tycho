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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
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
			if (skipIfReplaced && wasReplaced()) {
				getLog().info("Skipped because main artifact was replaced with baseline!");
				return;
			}
			long start = System.currentTimeMillis();
			Path targetFile;
			try {
				targetFile = createTargetFile();
			} catch (DependencyResolutionException e) {
				getLog().warn("Can't resolve API baseline, API baseline check is skipped!");
				return;

			}
			EclipseWorkspace<ApiAppKey> workspace = getWorkspace();
			List<String> configuration = setupArguments(targetFile);
			EclipseApplication apiApplication = applicationResolver.getApiApplication(workspace.getKey().repository);
			EclipseFramework eclipseFramework;
			try {
				eclipseFramework = apiApplication.startFramework(workspace, configuration);
			} catch (BundleException e) {
				throw new MojoFailureException("Start Framework failed!", e);
			}
			try {
				eclipseFramework.start();
			} catch (Exception e) {
				throw new MojoExecutionException("Execute ApiApplication failed", e);
			} finally {
				eclipseFramework.close();
			}
			getLog().info("API Analysis finished in " + time(start) + ".");
		}
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


	private List<String> setupArguments(Path targetFile)
			throws MojoFailureException {
		List<String> args = new ArrayList<>();
		args.add("-application");
		args.add("org.eclipse.pde.api.tools.apiAnalyzer");
		args.add("-project");
		args.add(project.getBasedir().getAbsolutePath());
		args.add("-baseline");
		args.add(targetFile.toAbsolutePath().toString());
		args.add("-dependencyList");
		try {
			args.add(writeProjectDependencies().toAbsolutePath().toString());
		} catch (Exception e) {
			throw new MojoFailureException("Can't write dependencies!", e);
		}
		args.add("-failOnError");
		return args;
	}

	private Path createTargetFile() throws MojoExecutionException, MojoFailureException {
		long start = System.currentTimeMillis();
		Collection<Path> baselineBundles;
		try {
			Optional<ArtifactKey> artifactKey = projectManager.getArtifactKey(project);
			getLog().info("Resolve API baseline for " + project.getId());
			baselineBundles = resolver.getApiBaselineBundles(baselines.stream().filter(repo -> repo.getUrl() != null)
					.map(repo -> new MavenRepositoryLocation(repo.getId(), URI.create(repo.getUrl()))).toList(),
					artifactKey.get());
			getLog().debug("API baseline contains " + baselineBundles.size() + " bundles (resolve takes " + time(start)
					+ ").");
		} catch (IllegalArtifactReferenceException e) {
			throw new MojoFailureException("Project specify an invalid artifact key", e);
		}
		String list = baselineBundles.stream().map(p -> p.toAbsolutePath().toString())
				.collect(Collectors.joining(System.lineSeparator()));
		Path targetFile = Path.of(project.getBuild().getDirectory(), project.getArtifactId() + "-apiBaseline.txt");
		try {
			Files.writeString(targetFile, list, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new MojoExecutionException("Writing target file failed!", e);
		}
		return targetFile;
	}

	private String time(long start) {
		long ms = System.currentTimeMillis() - start;
		if (ms < 1000) {
			return ms + " ms";
		}
		long sec = ms / 1000;
		return sec + " s";
	}

	private Path writeProjectDependencies() throws Exception {
		File outputFile = new File(project.getBuild().getDirectory(), "dependencies-list.txt");
		outputFile.getParentFile().mkdirs();
		Set<String> written = new HashSet<>();
		TychoProject tychoProject = projectManager.getTychoProject(project).get();
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath())) {
			List<ArtifactDescriptor> dependencies = TychoProjectUtils
					.getDependencyArtifacts(DefaultReactorProject.adapt(project)).getArtifacts();
			for (ArtifactDescriptor descriptor : dependencies) {
				File location = descriptor.fetchArtifact().get();
				if (location.equals(project.getBasedir())) {
					continue;
				}
				ReactorProject reactorProject = descriptor.getMavenProject();
				if (reactorProject == null) {
					writeLocation(writer, location, written);
				} else {
					ReactorProject otherProject = reactorProject;
					writeLocation(writer, otherProject.getArtifact(descriptor.getClassifier()), written);
				}
			}
			if (tychoProject instanceof OsgiBundleProject bundleProject) {
				pluginRealmHelper.visitPluginExtensions(project, session, ClasspathContributor.class, cpc -> {
					List<ClasspathEntry> list = cpc.getAdditionalClasspathEntries(project, Artifact.SCOPE_COMPILE);
					if (list != null && !list.isEmpty()) {
						for (ClasspathEntry entry : list) {
							for (File locations : entry.getLocations()) {
								try {
									writeLocation(writer, locations, written);
								} catch (IOException e) {
									// ignore...
								}
							}
						}
					}
				});
				// This is a hack because "org.eclipse.osgi.services" exports the annotation
				// package and might then be resolved by Tycho as a dependency, but then PDE
				// can't find the annotations here, so we always add this as a dependency
				// manually here, once "org.eclipse.osgi.services" is gone we can remove this
				// again!
				Optional<ResolvedArtifactKey> bundle = mavenBundleResolver.resolveMavenBundle(project, session,
						"org.osgi", "org.osgi.service.component.annotations", "1.3.0");
				bundle.ifPresent(key -> {
					try {
						writeLocation(writer, key.getLocation(), written);
					} catch (IOException e) {
					}
				});
			}
		}
		return outputFile.toPath();
	}

	private void writeLocation(BufferedWriter writer, File location, Set<String> written) throws IOException {
		if (location == null) {
			return;
		}
		String path = location.getAbsolutePath();
		if (written.add(path)) {
			writer.write(path);
			writer.write(System.lineSeparator());
		}
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
}
