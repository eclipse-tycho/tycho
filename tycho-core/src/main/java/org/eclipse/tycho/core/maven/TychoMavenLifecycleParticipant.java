/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH - Bug 457314 - handle null as tycho version
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.shared.BuildFailureException;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.resolver.TychoResolver;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "TychoMavenLifecycleListener")
public class TychoMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final String TYCHO_GROUPID = "org.eclipse.tycho";
    private static final Set<String> TYCHO_PLUGIN_IDS = new HashSet<>(Arrays.asList("tycho-maven-plugin",
            "tycho-p2-director-plugin", "tycho-p2-plugin", "tycho-p2-publisher-plugin", "tycho-p2-repository-plugin",
            "tycho-packaging-plugin", "tycho-pomgenerator-plugin", "tycho-source-plugin", "tycho-surefire-plugin",
            "tycho-versions-plugin", "tycho-compiler-plugin"));
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
                resolver.setupProject(session, project, DefaultReactorProject.adapt(project));
            }

            resolveProjects(session, projects);
        } catch (BuildFailureException e) {
            // build failure is not an internal (unexpected) error, so avoid printing a stack
            // trace by wrapping it in MavenExecutionException   
            throw new MavenExecutionException(e.getMessage(), e);
        }
    }

    private void validate(List<MavenProject> projects) throws MavenExecutionException {
        validateConsistentTychoVersion(projects);
        validateUniqueBaseDirs(projects);
    }

    private void resolveProjects(MavenSession session, List<MavenProject> projects) {
        List<ReactorProject> reactorProjects = DefaultReactorProject.adapt(session);
        ForkJoinPool executor = new ForkJoinPool(session.getRequest().getDegreeOfConcurrency());

        ForkJoinTask<?> future = executor.submit(() -> {
            projects.parallelStream().forEach(project -> resolver.resolveProject(session, project, reactorProjects));
        });

        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof BuildFailureException) {
                throw (BuildFailureException) e.getCause();
            }
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
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

    private boolean disableLifecycleParticipation(MavenSession session) {
        // command line property to disable Tycho lifecycle participant
        if ("maven".equals(session.getUserProperties().get("tycho.mode"))) {
            return true;
        }
        if (session.getUserProperties().containsKey("m2e.version")) {
            return true;
        }
        return false;
    }

    private void configureComponents(MavenSession session) {
        // TODO why does the bundle reader need to cache stuff in the local maven repository?
        File localRepository = new File(session.getLocalRepository().getBasedir());
        ((DefaultBundleReader) bundleReader).setLocationRepository(localRepository);
    }

}
