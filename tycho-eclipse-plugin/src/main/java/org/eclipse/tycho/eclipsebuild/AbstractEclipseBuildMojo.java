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
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.model.project.EclipseProject;
import org.eclipse.tycho.osgi.framework.Bundles;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
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

	@Parameter(defaultValue = "false", property = "tycho.eclipsebuild.skip")
	private boolean skip;

	@Parameter(defaultValue = "false", property = "tycho.eclipsebuild.debug")
	protected boolean debug;

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
	private EclipseWorkspaceManager workspaceManager;

	@Component
	private EclipseApplicationManager eclipseApplicationManager;

	@Component
	private TychoProjectManager projectManager;

	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		Optional<EclipseProject> eclipseProjectValue = projectManager.getEclipseProject(project);
		if (eclipseProjectValue.isEmpty()) {
			return;
		}
		EclipseProject eclipseProject = eclipseProjectValue.get();
		if (!isValid(eclipseProject)) {
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
		try (EclipseFramework framework = application.startFramework(workspaceManager
				.getWorkspace(EclipseApplicationManager.getRepository(eclipseRepository).getURL(), this), List.of())) {
			if (debug) {
				framework.printState();
			}
			if (framework.hasBundle(Bundles.BUNDLE_PDE_CORE)) {
				framework.execute(new SetTargetPlatform(projectDependencies, debug));
			} else {
				getLog().info("Skip set Target Platform because " + Bundles.BUNDLE_PDE_CORE
						+ " is not part of the framework...");
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
		if (eclipseProject.hasNature("org.eclipse.pde.PluginNature")) {
			set.add(Bundles.BUNDLE_PDE_CORE);
		}
		if (eclipseProject.hasNature("org.eclipse.jdt.core.javanature")) {
			set.add(Bundles.BUNDLE_JDT_CORE);
		}
		// TODO if project has org.eclipse.pde.api.tools.apiAnalysisNature and we have a
		// baseline parameter then
		// set.add(Bundles.BUNDLE_API_TOOLS);
		return set;
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
