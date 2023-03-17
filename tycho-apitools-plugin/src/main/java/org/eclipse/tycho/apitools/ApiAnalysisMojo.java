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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
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
import org.eclipse.core.runtime.internal.adaptor.EclipseAppLauncher;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.apitools.ApiWorkspaceManager.ApiWorkspace;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.helper.PluginRealmHelper;
import org.eclipse.tycho.model.project.EclipseProject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Performs a PDE-API Tools analysis of this project.
 */
@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ApiAnalysisMojo extends AbstractMojo {

	private static final String REPO_DEFAULT = "https://download.eclipse.org/releases/2023-06/";

	@Parameter(property = "plugin.artifacts")
	protected List<Artifact> pluginArtifacts;

	@Parameter(property = "project", readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "eclipse-plugin")
	private Set<String> supportedPackagingTypes;

	@Parameter(defaultValue = "false", property = "tycho.apitools.verify.skip")
	private boolean skip;

	@Parameter(property = "baselines", name = "baselines")
	private List<Repository> baselines;

	@Parameter()
	private Repository apiToolsRepository;

	@Parameter(property = "session", readonly = true, required = true)
	private MavenSession session;

	@Parameter
	private Map<String, String> properties;

	@Component
	private ApiWorkspaceManager workspaceManager;

	@Component
	private TychoProjectManager projectManager;

	@Component
	private ApiApplicationResolver resolver;

	@Component
	private PluginRealmHelper pluginRealmHelper;

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
			long start = System.currentTimeMillis();
			Path targetFile;
			try {
				targetFile = createTargetFile();
			} catch (DependencyResolutionException e) {
				getLog().warn("Can't resolve API baseline, API baseline check is skipped!");
				return;

			}
			ApiWorkspace workspace = workspaceManager.getWorkspace(getRepository());
			Map<String, String> frameworkProperties = getFrameworkProperties(workspace.getWorkDir());
			var loader = ServiceLoader.load(ConnectFrameworkFactory.class, getClass().getClassLoader());
			ConnectFrameworkFactory factory = loader.findFirst()
					.orElseThrow(() -> new MojoExecutionException("No ConnectFrameworkFactory found"));
			Framework framework = factory.newFramework(frameworkProperties, new ApiToolsModuleConnector());
			try {
				framework.init();
			} catch (BundleException e) {
				throw new MojoExecutionException("Init framework failed!", e);
			}
			BundleContext systemBundleContext = framework.getBundleContext();
			EquinoxConfiguration configuration = setupArguments(targetFile, systemBundleContext);
			setupLogging(systemBundleContext);
			try {
				workspace.install(systemBundleContext);
			} catch (IOException e) {
				throw new MojoFailureException("Install API workspace failed!", e);
			}
			FrameworkWiring wiring = framework.adapt(FrameworkWiring.class);
			wiring.resolveBundles(Collections.emptyList());
			long startFw = System.currentTimeMillis();
			try {
				framework.start();
			} catch (BundleException e) {
				throw new MojoExecutionException("Start framework failed!", e);
			}
			getLog().debug("Framework started (took " + time(startFw) + ").");
			EclipseAppLauncher appLauncher = new EclipseAppLauncher(systemBundleContext, false, true, null,
					configuration);
			systemBundleContext.registerService(ApplicationLauncher.class, appLauncher, null);
			try {
				Object returnValue = appLauncher.start(null);
				if (returnValue instanceof Integer retCode) {
					if (retCode != 0) {
						throw new MojoFailureException("API Tools failure!");
					}
				} else {
					throw new MojoFailureException("Execute the Api application failed!");
				}
			} catch (Exception e) {
				throw new MojoFailureException("Execute the Api application failed!", e);
			}
			try {
				framework.stop();
				framework.waitForStop(0);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (BundleException e) {
				// not interesting...
			}
			getLog().info("API Analysis finished in " + time(start) + ".");
		}
	}

	private MavenRepositoryLocation getRepository() {
		if (apiToolsRepository == null) {
			return new MavenRepositoryLocation(null, URI.create(REPO_DEFAULT));
		}
		return new MavenRepositoryLocation(apiToolsRepository.getId(), URI.create(apiToolsRepository.getUrl()));
	}

	private void setupLogging(BundleContext bundleContext) {
		LogListener logListener = new LogListener() {

			@Override
			public void logged(LogEntry entry) {
				if (isOnlyDebug(entry) && !getLog().isDebugEnabled()) {
					return;
				}
				switch (entry.getLogLevel()) {
				case AUDIT:
				case ERROR:
					getLog().error(entry.getMessage(), entry.getException());
					break;
				case WARN:
					getLog().warn(entry.getMessage(), entry.getException());
					break;
				case INFO:
					getLog().info(entry.getMessage(), entry.getException());
					break;
				case TRACE:
				case DEBUG:
					getLog().debug(entry.getMessage(), entry.getException());
					break;
				}
			}

			private static boolean isOnlyDebug(LogEntry entry) {
				String message = entry.getMessage();
				if (message.contains("The workspace ") && message.contains("with unsaved changes")) {
					return true;
				}
				if (message.contains("Workspace was not properly initialized or has already shutdown")) {
					return true;
				}
				if (message.contains("Platform proxy API not available")) {
					return true;
				}
				if (message.contains("Error processing mirrors URL")) {
					return true;
				}
				if (entry.getException() instanceof BundleException) {
					return true;
				}
				return false;
			}
		};
		ServiceTracker<LogReaderService, LogReaderService> serviceTracker = new ServiceTracker<>(bundleContext,
				LogReaderService.class, new ServiceTrackerCustomizer<>() {

					@Override
					public LogReaderService addingService(ServiceReference<LogReaderService> reference) {
						LogReaderService service = bundleContext.getService(reference);
						if (service != null) {
							service.addLogListener(logListener);
						}
						return service;
					}

					@Override
					public void modifiedService(ServiceReference<LogReaderService> reference,
							LogReaderService service) {
					}

					@Override
					public void removedService(ServiceReference<LogReaderService> reference, LogReaderService service) {
						service.removeLogListener(logListener);
						bundleContext.ungetService(reference);
					}
				});
		serviceTracker.open();

	}

	private EquinoxConfiguration setupArguments(Path targetFile, BundleContext systemBundleContext)
			throws MojoFailureException {
		ServiceTracker<EnvironmentInfo, EnvironmentInfo> environmentInfoTracker = new ServiceTracker<EnvironmentInfo, EnvironmentInfo>(
				systemBundleContext, EnvironmentInfo.class, null);
		environmentInfoTracker.open();
		EquinoxConfiguration configuration = (EquinoxConfiguration) environmentInfoTracker.getService();
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
		configuration.setAppArgs(args.toArray(String[]::new));
		environmentInfoTracker.close();
		return configuration;
	}

	private Path createTargetFile() throws MojoExecutionException, MojoFailureException {
		long start = System.currentTimeMillis();
		Collection<Path> baselineBundles;
		try {
			Optional<ArtifactKey> artifactKey = projectManager.getArtifactKey(project);
			getLog().info("Resolve API baseline for " + project.getId());
			baselineBundles = resolver.getApiBaselineBundles(baselines.stream()
					.map(repo -> new MavenRepositoryLocation(repo.getId(), URI.create(repo.getUrl()))).toList(),
					artifactKey.get());
			getLog().debug(
					"API baseline contains " + baselineBundles.size() + " bundles (resolve takes " + time(start)
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

	private Map<String, String> getFrameworkProperties(Path workDir) {
		Map<String, String> map = new LinkedHashMap<>();
		if (properties != null) {
			map.putAll(properties);
		}
		map.put("osgi.configuration.area", workDir.resolve("configuration").toAbsolutePath().toString());
		map.put("osgi.instance.area", workDir.resolve("data").toAbsolutePath().toString());
		map.put("osgi.compatibility.bootdelegation", "true");
		return map;
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

}
