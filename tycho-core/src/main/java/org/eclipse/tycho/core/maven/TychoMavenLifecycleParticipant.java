/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH - Bug 457314 - handle null as tycho version
 *    Christoph Läubrich    - Bug 569829 - TychoMavenLifecycleParticipant should respect fail-at-end flag / error output is missing
 *                          - Issue 557 - BuildPropertiesParser should use reactor project instead of basedir
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.BuildFailureException;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.build.BuildListeners;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencyClosure;
import org.eclipse.tycho.p2maven.transport.TransportCacheConfig;
import org.eclipse.tycho.resolver.TychoResolver;
import org.eclipse.tycho.version.TychoVersion;

@Named("TychoMavenLifecycleListener")
@Singleton
public class TychoMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    static final boolean DUMP_DATA = Boolean.getBoolean("tycho.p2.dump") || Boolean.getBoolean("tycho.p2.dump.model");

    private static final String TYCHO_GROUPID = "org.eclipse.tycho";
    private static final Set<String> TYCHO_PLUGIN_IDS = new HashSet<>(
            Arrays.asList("tycho-maven-plugin", "tycho-p2-director-plugin", "tycho-p2-plugin",
                    "tycho-p2-publisher-plugin", "tycho-p2-repository-plugin", "tycho-packaging-plugin",
                    "tycho-source-plugin", "tycho-surefire-plugin", "tycho-versions-plugin", "tycho-compiler-plugin"));
    private static final String P2_USER_AGENT_KEY = "p2.userAgent";
    private static final String P2_USER_AGENT_VALUE = "tycho/";

    @Inject
    private BundleReader bundleReader;

    @Inject
    private TychoResolver resolver;

    @Inject
    private PlexusContainer plexus;

    @Inject
    private Logger log;

    @Inject
    MavenProjectDependencyProcessor dependencyProcessor;

    @Inject
    private ModelWriter modelWriter;

    @Inject
    BuildListeners buildListeners;

    @Inject
    TychoProjectManager projectManager;

    @Inject
    TransportCacheConfig transportCacheConfig;

    public TychoMavenLifecycleParticipant() {
        // needed for plexus
    }

    // needed for unit tests
    protected TychoMavenLifecycleParticipant(Logger log) {
        this.log = log;
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        log.info("Tycho Version:  " + TychoVersion.getTychoVersion() + " (" + TychoVersion.getSCMInfo() + ")");
        log.info("Tycho Mode:     "
                + session.getUserProperties().getProperty(TychoConstants.SESSION_PROPERTY_TYCHO_MODE, "project"));
        log.info("Tycho Builder:  "
                + session.getUserProperties().getProperty(TychoConstants.SESSION_PROPERTY_TYCHO_BUILDER, "maven"));
        log.info("Build Threads:  " + session.getRequest().getDegreeOfConcurrency());
        if (disableLifecycleParticipation(session)) {
            buildListeners.notifyBuildStart(session);
            return;
        }
        List<MavenProject> projects = session.getProjects();
        try {
            validate(projects);

            // setting this system property to let EF figure out where the traffic
            // is coming from (#467418)
            System.setProperty(P2_USER_AGENT_KEY, P2_USER_AGENT_VALUE + TychoVersion.getTychoVersion());

            configureComponents(session);

            for (MavenProject project : projects) {
                resolver.setupProject(session, project);
            }
            Map<Boolean, List<MavenProject>> partition = projects.stream().collect(Collectors.partitioningBy(
                    project -> projectManager.getTargetPlatformConfiguration(project).isRequireEagerResolve()));
            List<MavenProject> eagerProjects = partition.get(true);
            List<MavenProject> lazyProjects = partition.get(false);

            if (eagerProjects.size() > 0) {
                resolveProjects(session, eagerProjects);
            }
            if (lazyProjects.size() > 0) {
                try {
                    ProjectDependencyClosure closure = dependencyProcessor.computeProjectDependencyClosure(projects,
                            session);
                    for (MavenProject project : lazyProjects) {
                        if (projectManager.getTychoProject(project).isEmpty()) {
                            //do not inject additional dependencies for non Tycho managed projects!
                            continue;
                        }
                        Collection<MavenProject> dependencyProjects = closure.getDependencyProjects(project,
                                projectManager.getContextIUs(project));
                        MavenDependencyInjector.injectMavenProjectDependencies(project, dependencyProjects);
                        if (DUMP_DATA) {
                            try {
                                Set<MavenProject> visited = new HashSet<>();
                                modelWriter.write(new File(project.getBasedir(), "pom-model.xml"), Map.of(),
                                        project.getModel());
                                try (BufferedWriter writer = Files.newBufferedWriter(
                                        new File(project.getBasedir(), "requirements.txt").toPath())) {
                                    writer.write(project.getId() + ":\r\n");
                                    dumpProjectRequirements(project, writer, closure, dependencyProjects, "\t",
                                            visited);
                                }
                            } catch (IOException e) {
                            }
                        }
                    }
                } catch (CoreException e) {
                    throw new MavenExecutionException(e.getMessage(), e);
                }
            }
        } catch (BuildFailureException e) {
            // build failure is not an internal (unexpected) error, so avoid printing a stack
            // trace by wrapping it in MavenExecutionException
            throw new MavenExecutionException(e.getMessage(), e);
        }
        buildListeners.notifyBuildStart(session);
    }

    private void dumpProjectRequirements(MavenProject project, BufferedWriter writer, ProjectDependencyClosure closure,
            Collection<MavenProject> dependencyProjects, String indent, Set<MavenProject> visited) throws IOException {
        if (visited.add(project)) {
            List<IRequirement> projectRequirements = closure.getProjectUnits(project).stream()
                    .flatMap(iu -> iu.getRequirements().stream()).toList();
            String indent2 = indent + "\t";
            for (MavenProject dependency : dependencyProjects) {
                writer.write(indent + " depends on " + dependency.getId() + ":\r\n");
                for (IRequirement requirement : projectRequirements) {
                    List<IInstallableUnit> satisfies = closure.getProjectUnits(dependency).stream()
                            .filter(iu -> iu.satisfies(requirement)).toList();
                    for (IInstallableUnit satIU : satisfies) {
                        writer.write(indent2 + "provides " + satIU + " that satisfies " + requirement + "\r\n");
                    }
                }
                dumpProjectRequirements(dependency, writer, closure,
                        closure.getDependencyProjects(dependency, projectManager.getContextIUs(project)), indent2,
                        visited);
            }
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        buildListeners.notifyBuildEnd(session);
        if (plexus.hasComponent(EquinoxServiceFactory.class)) {
            try {
                EquinoxServiceFactory factory = plexus.lookup(EquinoxServiceFactory.class);
                // do not use plexus.dispose() as this only works once and we
                // want to reuse the factory multiple times but make sure the
                // equinox framework is fully recreated
                if (factory instanceof org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable disposable) {
                    disposable.dispose();
                }
            } catch (ComponentLookupException e) {
                throw new MavenExecutionException(e.getMessage(), e);
            }
        }
    }

    private void validate(List<MavenProject> projects) throws MavenExecutionException {
        validateConsistentTychoVersion(projects);
        validateUniqueBaseDirs(projects);
    }

    private void resolveProjects(MavenSession session, List<MavenProject> projects) {

        MavenExecutionRequest request = session.getRequest();
        boolean failFast = MavenExecutionRequest.REACTOR_FAIL_FAST.equals(request.getReactorFailureBehavior());
        Map<MavenProject, BuildFailureException> resolutionErrors = new ConcurrentHashMap<>();
        Consumer<MavenProject> resolveProject = project -> {
            if (failFast && !resolutionErrors.isEmpty()) {
                //short circuit
                return;
            }
            try {
                MavenSession clone = session.clone();
                clone.setCurrentProject(project);
                resolver.resolveProject(clone, project);
                if (DUMP_DATA) {
                    try {
                        modelWriter.write(new File(project.getBasedir(), "pom-model-classic.xml"), Map.of(),
                                project.getModel());
                    } catch (IOException e) {
                    }
                }
            } catch (BuildFailureException e) {
                resolutionErrors.put(project, e);
                if (failFast) {
                    throw e;
                }
            }
        };

        int degreeOfConcurrency = request.getDegreeOfConcurrency();
        Predicate<MavenProject> takeWhile = Predicate.not(p -> failFast && !resolutionErrors.isEmpty());
        if (degreeOfConcurrency > 1) {
            ForkJoinPool executor = new ForkJoinPool(degreeOfConcurrency);
            ForkJoinTask<?> future = executor
                    .submit(() -> projects.parallelStream().takeWhile(takeWhile).forEach(resolveProject));
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                throw cause instanceof RuntimeException ex //
                        ? ex
                        : new RuntimeException("resolve dependencies failed", cause);
            } finally {
                executor.shutdown();
            }

        } else {
            projects.stream().takeWhile(takeWhile).forEach(resolveProject);
        }

        reportResolutionErrors(resolutionErrors, projects, failFast);
    }

    private void reportResolutionErrors(Map<MavenProject, BuildFailureException> resolutionErrors,
            List<MavenProject> projects, boolean failFast) {
        if (resolutionErrors.isEmpty()) {
            return;
        }

        if (resolutionErrors.size() == 1 || failFast) {
            //The idea is if user want to fail-fast he would expect to get exactly one error (the first one),
            //while if parallel execution is enabled it might report an (incomplete) list of other failures that happened due to the parallel processing.
            throw resolutionErrors.values().iterator().next();
        }

        DependencyResolutionException exception = new DependencyResolutionException(
                String.format("Cannot resolve dependencies of %d/%d projects, see log for details",
                        resolutionErrors.size(), projects.size()));
        resolutionErrors.values().forEach(exception::addSuppressed);
        resolutionErrors.forEach((project, error) -> log.error(project.getName() + ": " + error.getMessage()));

        throw exception;
    }

    protected void validateConsistentTychoVersion(List<MavenProject> projects) throws MavenExecutionException {
        Map<String, Set<MavenProject>> versionToProjectsMap = new HashMap<>();
        for (MavenProject project : projects) {
            for (Plugin plugin : project.getBuild().getPlugins()) {
                if (TYCHO_GROUPID.equals(plugin.getGroupId()) && TYCHO_PLUGIN_IDS.contains(plugin.getArtifactId())) {
                    String version = plugin.getVersion();
                    // Skip checking plug ins that do not have a version
                    if (version == null) {
                        continue;
                    }
                    log.debug(
                            TYCHO_GROUPID + ":" + plugin.getArtifactId() + ":" + version + " configured in " + project);
                    Set<MavenProject> projectSet = versionToProjectsMap.get(version);
                    if (projectSet == null) {
                        projectSet = new LinkedHashSet<>();
                        versionToProjectsMap.put(version, projectSet);
                    }
                    projectSet.add(project);
                }
            }
        }
        if (versionToProjectsMap.size() > 1) {
            List<String> versions = new ArrayList<>(versionToProjectsMap.keySet());
            Collections.sort(versions);
            log.error("Several versions of Tycho plugins are configured " + versions + ":");
            for (String version : versions) {
                log.error(version + ":");
                for (MavenProject project : versionToProjectsMap.get(version)) {
                    log.error("\t" + project.toString());
                }
            }
            throw new MavenExecutionException("All tycho plugins configured in one reactor must use the same version",
                    projects.get(0).getFile());
        }
    }

    private void validateUniqueBaseDirs(List<MavenProject> projects) throws MavenExecutionException {
        // we store intermediate build results in the target/ folder and use the baseDir as unique key
        // so multiple modules in the same baseDir would lead to irreproducible/unexpected results
        // e.g. with mvn clean. This should really not be supported by maven core
        Set<File> baseDirs = new HashSet<>();
        for (MavenProject project : projects) {
            File basedir = project.getBasedir();
            if (baseDirs.contains(basedir)) {
                throw new MavenExecutionException(
                        "Multiple modules within the same basedir are not supported: " + basedir, project.getFile());
            } else {
                baseDirs.add(basedir);
            }
        }
    }

    private static final Set<String> CLEAN_PHASES = Set.of("pre-clean", "clean", "post-clean");

    private boolean disableLifecycleParticipation(MavenSession session) {
        return isM2E(session) || isCleanOnly(session);
    }

    private boolean isCleanOnly(MavenSession session) {
        // disable for 'clean-only' builds. Consider that Maven can be invoked without explicit goals, if default goals are specified
        return !session.getGoals().isEmpty() && CLEAN_PHASES.containsAll(session.getGoals());
    }

    private boolean isM2E(MavenSession session) {
        return session.getUserProperties().containsKey("m2e.version");
    }

    private void configureComponents(MavenSession session) {
        // TODO why does the bundle reader need to cache stuff in the local maven repository?
        ((DefaultBundleReader) bundleReader).setCacheLocation(transportCacheConfig.getCacheLocation());
    }

}
