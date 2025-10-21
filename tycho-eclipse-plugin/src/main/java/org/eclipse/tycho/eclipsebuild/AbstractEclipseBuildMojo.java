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
package org.eclipse.tycho.eclipsebuild;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.model.project.EclipseProject;
import org.eclipse.tycho.osgi.framework.Bundles;
import org.eclipse.tycho.osgi.framework.DefaultEclipseApplicationManager;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseApplicationFactory;
import org.eclipse.tycho.osgi.framework.EclipseApplicationManager;
import org.eclipse.tycho.osgi.framework.EclipseFramework;
import org.eclipse.tycho.osgi.framework.EclipseWorkspaceManager;
import org.eclipse.tycho.osgi.framework.Features;
import org.osgi.framework.BundleException;

/**
 * An abstract mojo baseclass that can be used to perform actions on an eclipse
 * project that requires the build infrastructure.
 * 
 * @param <Result> the rsult type
 */
public abstract class AbstractEclipseBuildMojo<Result extends EclipseBuildResult> extends AbstractMojo {

	static final String PARAMETER_LOCAL = "local";

	@Parameter()
	private Repository eclipseRepository;

	/**
	 * If configured, automatically sets a baseline for this project if api tools
	 * nature is enabled
	 */
	@Parameter(property = "baselines", name = "baselines")
	private List<Repository> baselines;

	@Parameter(defaultValue = "false", property = "tycho.eclipsebuild.skip")
	private boolean skip;

	@Parameter(defaultValue = "false", property = "tycho.eclipsebuild.debug")
	protected boolean debug;

	@Parameter(defaultValue = "false")
	private boolean failOnResolutionError;

	@Parameter(property = "tycho.eclipsebuild.application")
	private String application;
	/**
	 * Controls if the local target platform of the project should be used to
	 * resolve the eclipse application
	 */
	@Parameter(defaultValue = "false", property = "tycho.eclipsebuild.local", name = PARAMETER_LOCAL)
	private boolean local;

	@Parameter(defaultValue = "true", property = "tycho.eclipsebuild.printMarker")
	private boolean printMarker;

	@Parameter
	private List<String> bundles;

	@Parameter
	private List<String> features;

	@Parameter(property = "project", readonly = true)
	protected MavenProject project;

	@Component
	protected MavenSession mavenSession;

	@Component
	private EclipseWorkspaceManager workspaceManager;

	@Component
	private EclipseApplicationManager eclipseApplicationManager;

	@Component
	private EclipseApplicationFactory applicationFactory;

	@Component
	private TychoProjectManager projectManager;

	@Component
	ToolchainManager toolchainManager;



	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		if (projectManager.getTychoProject(project).isEmpty()) {
			getLog().info("Skipping, not a Tycho project!");
			return;
		}
		Optional<EclipseProject> eclipseProjectValue = projectManager.getEclipseProject(project);
		if (eclipseProjectValue.isEmpty()) {
			getLog().info("Skipping, not an Eclipse project!");
			return;
		}
		EclipseProject eclipseProject = eclipseProjectValue.get();
		if (!isValid(eclipseProject)) {
			getLog().info("Skipping, not a valid project type for this mojo!");
			return;
		}
		Collection<Path> projectDependencies;
		try {
			projectDependencies = projectManager.getProjectDependencies(project);
		} catch (Exception e) {
			throw new MojoFailureException("Can't resolve project dependencies", e);
		}
		EclipseApplication application;
		Bundles bundles = new Bundles(getBundles(eclipseProject));
		Features features = new Features(getFeatures());
		if (local) {
			TargetPlatform targetPlatform = projectManager.getTargetPlatform(project).orElseThrow(
					() -> new MojoFailureException("Can't get target platform for project " + project.getId()));
			application = eclipseApplicationManager.getApplication(targetPlatform, bundles, features, getName());
		} else {
			application = eclipseApplicationManager.getApplication(eclipseRepository, bundles, features, getName());
		}
		List<String> arguments;
		String applicationName = this.application;
		boolean useApplication = applicationName != null && !applicationName.isBlank();
		if (useApplication) {
			arguments = List.of(EclipseApplication.ARG_APPLICATION, applicationName);
		} else {
			arguments = List.of();
		}
		try (EclipseFramework framework = application.startFramework(workspaceManager
				.getWorkspace(DefaultEclipseApplicationManager.getRepository(eclipseRepository).getURL(), this), arguments)) {
			if (debug) {
				framework.printState();
			}
			if (useApplication) {
				Thread thread = new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							framework.start();
						} catch (Exception e) {
							getLog().error("Running application " + applicationName + " failed", e);
						}
					}
				});
				thread.setName(getName() + " Application Thread");
				thread.start();
				framework.waitForApplicationStart(TimeUnit.SECONDS.toMillis(30));
			}
			if (hasJDTNature(eclipseProject)) {
				if (framework.hasBundle(Bundles.BUNDLE_JDT_LAUNCHING)) {
					List<Path> jvms = new ArrayList<>();
					for (Toolchain toolchain : toolchainManager.getToolchains(mavenSession, "jdk", Map.of())) {
						String tool = toolchain.findTool("java");
						if (tool != null) {
							jvms.add(Path.of(tool).getParent().getParent());
						}
					}
					framework.execute(new SetJVMs(jvms, debug));
				} else {
					getLog().info(
							"Skip set JVMs because " + Bundles.BUNDLE_JDT_LAUNCHING
							+ " is not part of the framework...");
				}
			}

			if (hasPDENature(eclipseProject)) {
				if (framework.hasBundle(Bundles.BUNDLE_PDE_CORE)) {
					Collection<TargetEnvironment> targetEnvironments = projectManager.getBaselineEnvironments(project);
					Map<String, String> targetEnvironment = targetEnvironments.stream().findFirst()
							.map(te -> te.toFilterProperties()).orElse(Map.of());
					framework.execute(new SetTargetPlatform(projectDependencies, targetEnvironment, debug));
				} else {
					getLog().info("Skip set Target Platform because " + Bundles.BUNDLE_PDE_CORE
							+ " is not part of the framework...");
				}
			}
			if (hasAPIToolsNature(eclipseProject)) {
				if (framework.hasBundle(Bundles.BUNDLE_API_TOOLS)) {
					if (hasBaselinesSet()) {
						framework.execute(new SetApiBaseline(project.getId(), getBaselineBundles(), debug));
					} else {
						getLog().info("Skip set ApiBaseline because no baselines set...");
					}
				} else {
					getLog().info("Skip set ApiBasline because " + Bundles.BUNDLE_API_TOOLS
							+ " is not part of the framework...");
				}
			}
			Result result = framework.execute(createExecutable(), getRequireBundles());
			if (printMarker) {
				Log log = getLog();
				result.markers().filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_INFO)
						.forEach(info -> printMarker(info, result, log::info));
				result.markers().filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_WARNING)
						.forEach(warn -> printMarker(warn, result, log::warn));
				result.markers().filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
						.forEach(error -> printMarker(error, result, log::error));
			}
			handleResult(result);
		} catch (BundleException e) {
			throw new MojoFailureException("Can't start framework!", e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause.getClass().getName().equals(CoreException.class.getName())) {
				throw new MojoFailureException(cause.getMessage(), cause);
			}
			throw new MojoExecutionException(cause);
		}
	}

	protected boolean isValid(EclipseProject eclipseProject) {
		return true;
	}

	protected abstract void handleResult(Result result) throws MojoFailureException;

	protected abstract AbstractEclipseBuild<Result> createExecutable();

	protected String[] getRequireBundles() {
		return new String[0];
	}

	protected Set<String> getFeatures() {
		Set<String> set = new HashSet<>();
		if (features != null) {
			set.addAll(features);
		}
		return set;
	}

	protected Set<String> getBundles(EclipseProject eclipseProject) {
		Set<String> set = new HashSet<>();
		set.add("org.eclipse.core.resources");
		set.add("org.eclipse.core.runtime");
		set.add("org.eclipse.core.jobs");
		if (bundles != null) {
			set.addAll(bundles);
		}
		for (String requiredBundle : getRequireBundles()) {
			set.add(requiredBundle);
		}
		if (hasPDENature(eclipseProject)) {
			set.add(Bundles.BUNDLE_PDE_CORE);
		}
		if (hasJDTNature(eclipseProject)) {
			set.add(Bundles.BUNDLE_JDT_CORE);
		}
		if (hasAPIToolsNature(eclipseProject)) {
			set.add(Bundles.BUNDLE_API_TOOLS);
		}
		return set;
	}

	private Collection<Path> getBaselineBundles() throws MojoFailureException {
		try {
			Collection<TargetEnvironment> targetEnvironments = projectManager.getBaselineEnvironments(project);
			Optional<ArtifactKey> artifactKey = projectManager.getArtifactKey(project);
			getLog().info("Resolve API baseline for " + project.getId() + " with "
					+ targetEnvironments.stream().map(String::valueOf).collect(Collectors.joining(", ")));
			return applicationFactory.getApiBaselineBundles(
					baselines.stream().filter(repo -> repo.getUrl() != null)
							.map(repo -> new MavenRepositoryLocation(repo.getId(), URI.create(repo.getUrl()))).toList(),
					artifactKey.get(), targetEnvironments);
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

	private boolean hasJDTNature(EclipseProject eclipseProject) {
		return eclipseProject.hasNature("org.eclipse.jdt.core.javanature");
	}

	private boolean hasAPIToolsNature(EclipseProject eclipseProject) {
		return eclipseProject.hasNature("org.eclipse.pde.api.tools.apiAnalysisNature");
	}

	private boolean hasPDENature(EclipseProject eclipseProject) {
		return eclipseProject.hasNature("org.eclipse.pde.PluginNature");
	}

	private boolean hasBaselinesSet() {
		return baselines != null && baselines.size() > 0;
	}

	private static void printMarker(IMarker marker, EclipseBuildResult result, Consumer<CharSequence> consumer) {
		consumer.accept(asString(marker, result).toString().trim());
	}

	protected static StringBuilder asString(IMarker marker, EclipseBuildResult result) {
		StringBuilder sb = new StringBuilder();
		String path = result.getMarkerPath(marker);
		if (path != null) {
			sb.append(path);
			int line = marker.getAttribute("lineNumber", -1);
			if (line > -1) {
				sb.append(":");
				sb.append(line);
			}
			sb.append(" ");
		}
		String message = marker.getAttribute("message", "");
		if (!message.isBlank()) {
			sb.append(message);
			sb.append(" ");
		}
		String sourceId = marker.getAttribute("sourceId", "");
		if (!sourceId.isBlank()) {
			sb.append(sourceId);
			sb.append(" ");
		}
		return sb;
	}

	protected abstract String getName();

}
