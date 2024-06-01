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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.eclipsebuild;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.osgi.framework.Bundles;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseApplicationManager;
import org.eclipse.tycho.osgi.framework.EclipseFramework;
import org.eclipse.tycho.osgi.framework.EclipseWorkspaceManager;
import org.eclipse.tycho.osgi.framework.Features;
import org.osgi.framework.BundleException;

/**
 * This mojo allows to perform an eclipse-build on a project like it would be performed inside the
 * IDE, this can be useful in cases where there are very special builders that are not part of
 * Tycho.
 */
@Mojo(name = "eclipse-build", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class EclipseBuildMojo extends AbstractMojo {

	static final String PARAMETER_LOCAL = "local";

	private static final String NAME = "Eclipse Build Project";

    @Parameter()
    private Repository eclipseRepository;

    @Parameter(defaultValue = "false", property = "tycho.eclipsebuild.skip")
    private boolean skip;

    @Parameter(defaultValue = "false", property = "tycho.eclipsebuild.debug")
    private boolean debug;

    /**
     * Controls if the local target platform of the project should be used to resolve the eclipse
     * application
     */
	@Parameter(defaultValue = "false", property = "tycho.eclipsebuild.local", name = PARAMETER_LOCAL)
    private boolean local;

    @Parameter(defaultValue = "true", property = "tycho.eclipsebuild.failOnError")
    private boolean failOnError;

	@Parameter(defaultValue = "true", property = "tycho.eclipsebuild.printMarker")
	private boolean printMarker;

	@Parameter
	private List<String> bundles;

	@Parameter
	private List<String> features;

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Component
    private EclipseWorkspaceManager workspaceManager;

    @Component
    private EclipseApplicationManager eclipseApplicationManager;

    @Component
    private TychoProjectManager projectManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Collection<Path> projectDependencies;
        try {
            projectDependencies = projectManager.getProjectDependencies(project);
        } catch (Exception e) {
            throw new MojoFailureException("Can't resolve project dependencies", e);
        }
        EclipseApplication application;
        //TODO configureable by parameters!
        Bundles bundles = new Bundles(getBundles());
		Features features = new Features(getFeatures());
        if (local) {
            TargetPlatform targetPlatform = projectManager.getTargetPlatform(project).orElseThrow(
                    () -> new MojoFailureException("Can't get target platform for project " + project.getId()));
            application = eclipseApplicationManager.getApplication(targetPlatform, bundles, features, NAME);
        } else {
            application = eclipseApplicationManager.getApplication(eclipseRepository, bundles, features, NAME);
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
            EclipseBuildResult result = framework
					.execute(new EclipseBuild(project.getBasedir().toPath(), debug));
			List<IMarker> errors = result.markers()
					.filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR).toList();
			if (printMarker) {
				Log log = getLog();
				result.markers().filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_INFO)
						.forEach(info -> printMarker(info, result, log::info));
				result.markers().filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_WARNING)
						.forEach(warn -> printMarker(warn, result, log::warn));
				errors.forEach(error -> printMarker(error, result, log::error));
			}
            if (failOnError && errors.size() > 0) {
                String msg = errors.stream().map(problem -> asString(problem, result))
                        .collect(Collectors.joining(System.lineSeparator()));
                throw new MojoFailureException("There are Build errors:" + System.lineSeparator() + msg);
            }
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

	private Set<String> getFeatures() {
		Set<String> set = new HashSet<>();
		if (features != null) {
			set.addAll(features);
		}
		return set;
	}

	private Set<String> getBundles() {
		Set<String> set = new HashSet<>();
		set.add("org.eclipse.core.resources");
		set.add("org.eclipse.core.runtime");
		set.add("org.eclipse.core.jobs");
		if (bundles != null) {
			set.addAll(bundles);
		}
		return set;
	}

    private void printMarker(IMarker marker, EclipseBuildResult result, Consumer<CharSequence> consumer) {
        StringBuilder sb = asString(marker, result);
        consumer.accept(sb.toString().trim());
    }

    private StringBuilder asString(IMarker marker, EclipseBuildResult result) {
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

}
