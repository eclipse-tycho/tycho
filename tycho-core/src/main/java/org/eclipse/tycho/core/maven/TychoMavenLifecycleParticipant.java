/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH - Bug 457314 - handle null as tycho version
 *    Christoph Läubrich    - Bug 569829 - TychoMavenLifecycleParticipant should respect fail-at-end flag / error output is missing
 *                          - Issue 557 - BuildPropertiesParser should use reactor project instead of basedir
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.sisu.equinox.OSGiServiceFactory;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.shared.BuildFailureException;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencyClosure;
import org.eclipse.tycho.resolver.TychoResolver;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "TychoMavenLifecycleListener")
public class TychoMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    static final boolean DUMP_DATA = Boolean.getBoolean("tycho.p2.dump") || Boolean.getBoolean("tycho.p2.dump.model");

    static final boolean USE_OLD_RESOLVER = Boolean.parseBoolean(System.getProperty("tycho.resolver.classic", "true"));

    private static final String TYCHO_GROUPID = "org.eclipse.tycho";
    private static final Set<String> TYCHO_PLUGIN_IDS = new HashSet<>(
            Arrays.asList("tycho-maven-plugin", "tycho-p2-director-plugin", "tycho-p2-plugin",
                    "tycho-p2-publisher-plugin", "tycho-p2-repository-plugin", "tycho-packaging-plugin",
                    "tycho-source-plugin", "tycho-surefire-plugin", "tycho-versions-plugin", "tycho-compiler-plugin"));
    private static final String P2_USER_AGENT_KEY = "p2.userAgent";
    private static final String P2_USER_AGENT_VALUE = "tycho/";

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private TychoResolver resolver;

    @Requirement
    private PlexusContainer plexus;

    @Requirement
    private Logger log;

    @Requirement
    private BuildPropertiesParser buildPropertiesParser;

    @Requirement
    MavenProjectDependencyProcessor dependencyProcessor;

    @Requirement
    private ModelWriter modelWriter;

    public TychoMavenLifecycleParticipant() {
        // needed for plexus
    }

    // needed for unit tests
    protected TychoMavenLifecycleParticipant(Logger log) {
        this.log = log;
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        try {
            if (disableLifecycleParticipation(session)) {
                return;
            }
            List<MavenProject> projects = session.getProjects();
            validate(projects);

            // setting this system property to let EF figure out where the traffic 
            // is coming from (#467418)
            System.setProperty(P2_USER_AGENT_KEY, P2_USER_AGENT_VALUE + TychoVersion.getTychoVersion());

            configureComponents(session);

            for (MavenProject project : projects) {
                ReactorProject reactorProject = DefaultReactorProject.adapt(project);
                reactorProject.setContextValue(ReactorProject.CTX_INTERPOLATOR,
                        new TychoInterpolator(session, project));
                reactorProject.setContextValue(ReactorProject.CTX_BUILDPROPERTIESPARSER, buildPropertiesParser);
                resolver.setupProject(session, project, reactorProject);
            }
            if (USE_OLD_RESOLVER) {
                for (MavenProject project : projects) {
                    resolver.resolveMavenProject(session, project, projects);
                    if (DUMP_DATA) {
                        try {
                            modelWriter.write(new File(project.getBasedir(), "pom-model-classic.xml"), Map.of(),
                                    project.getModel());
                        } catch (IOException e) {
                        }
                    }
                }
            } else {
                try {
                    ProjectDependencyClosure closure = dependencyProcessor.computeProjectDependencyClosure(projects,
                            session);
                    for (MavenProject project : projects) {
                        Model model = project.getModel();
                        Set<String> existingDependencies = model.getDependencies().stream().map(dep -> getKey(dep))
                                .collect(Collectors.toCollection(HashSet::new));
                        for (MavenProject dependencyProject : closure.getDependencyProjects(project)) {
                            Dependency dependency = new Dependency();
                            dependency.setArtifactId(dependencyProject.getArtifactId());
                            dependency.setGroupId(dependencyProject.getGroupId());
                            dependency.setVersion(dependencyProject.getVersion());
                            String packaging = dependencyProject.getPackaging();
                            dependency.setType(packaging);
                            dependency.setScope(Artifact.SCOPE_COMPILE);
                            dependency.setOptional(false);
                            if (existingDependencies.add(getKey(dependency))) {
                                model.addDependency(dependency);
                            }
                        }
                        if (DUMP_DATA) {
                            try {
                                modelWriter.write(new File(project.getBasedir(), "pom-model.xml"), Map.of(), model);
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
    }

    private static String getKey(Dependency dependency) {

        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getType() + ":"
                + Objects.requireNonNullElse(dependency.getClassifier(), "");
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        if (plexus.hasComponent(OSGiServiceFactory.class)) {
            try {
                OSGiServiceFactory factory = plexus.lookup(OSGiServiceFactory.class);
                // do not use plexus.dispose() as this only works once and we
                // want to reuse the factory multiple times but make sure the
                // equinox framework is fully recreated
                if (factory instanceof Disposable) {
                    ((Disposable) factory).dispose();
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
            log.error("Several versions of tycho plugins are configured " + versions + ":");
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
        // command line property to disable Tycho lifecycle participant
        return "maven".equals(session.getUserProperties().get("tycho.mode"))
                || session.getUserProperties().containsKey("m2e.version")
                // disable for 'clean-only' builds. Consider that Maven can be invoked without explicit goals, if default goals are specified
                || (!session.getGoals().isEmpty() && CLEAN_PHASES.containsAll(session.getGoals()));
    }

    private void configureComponents(MavenSession session) {
        // TODO why does the bundle reader need to cache stuff in the local maven repository?
        File localRepository = new File(session.getLocalRepository().getBasedir());
        ((DefaultBundleReader) bundleReader).setLocationRepository(localRepository);
    }

}
