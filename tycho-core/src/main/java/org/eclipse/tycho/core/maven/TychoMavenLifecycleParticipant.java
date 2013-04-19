/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
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

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.resolver.TychoDependencyResolver;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "TychoMavenLifecycleListener")
public class TychoMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final String TYCHO_GROUPID = "org.eclipse.tycho";
    private static final Set<String> TYCHO_PLUGIN_IDS = new HashSet<String>(Arrays.asList("tycho-maven-plugin",
            "tycho-p2-director-plugin", "tycho-p2-plugin", "tycho-p2-publisher-plugin", "tycho-p2-repository-plugin",
            "tycho-packaging-plugin", "tycho-pomgenerator-plugin", "tycho-source-plugin", "tycho-surefire-plugin",
            "tycho-versions-plugin"));

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private TychoDependencyResolver resolver;

    @Requirement
    private PlexusContainer plexus;

    @Requirement
    private Logger log;
    // TODO update once maven 3.1 is released
    private static final ComparableVersion MIN_MAVEN_VERSION = new ComparableVersion("3.1.0-alpha-1");

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        if (disableLifecycleParticipation(session)) {
            return;
        }
        List<MavenProject> projects = session.getProjects();
        validate(session);
        registerExecutionListener(session);
        configureComponents(session);

        for (MavenProject project : projects) {
            resolver.setupProject(session, project, DefaultReactorProject.adapt(project));
        }

        List<ReactorProject> reactorProjects = DefaultReactorProject.adapt(session);
        for (MavenProject project : projects) {
            resolver.resolveProject(session, project, reactorProjects);
        }
    }

    private void validate(MavenSession session) throws MavenExecutionException {
        validateMinMavenVersion(session);
        validateConsistentTychoVersion(session.getProjects());
        validateUniqueBaseDirs(session.getProjects());
    }

    private void validateMinMavenVersion(MavenSession session) throws MavenExecutionException {
        // ${maven.version} available since 3.0.4, see http://maven.apache.org/ref/3.0.4/maven-model-builder/
        String versionString = session.getSystemProperties().getProperty("maven.version");
        if (versionString == null) {
            throw new MavenExecutionException("Minimum required maven version is " + MIN_MAVEN_VERSION
                    + ". Detected version is < 3.0.4", session.getTopLevelProject().getFile());
        }
        ComparableVersion version = new ComparableVersion(versionString);
        if (version.compareTo(MIN_MAVEN_VERSION) < 0) {
            throw new MavenExecutionException("Minimum required maven version is " + MIN_MAVEN_VERSION
                    + ". Detected version: " + version, session.getTopLevelProject().getFile());
        }
    }

    private void validateConsistentTychoVersion(List<MavenProject> projects) throws MavenExecutionException {
        Map<String, Set<MavenProject>> versionToProjectsMap = new HashMap<String, Set<MavenProject>>();
        for (MavenProject project : projects) {
            for (Plugin plugin : project.getBuild().getPlugins()) {
                if (TYCHO_GROUPID.equals(plugin.getGroupId()) && TYCHO_PLUGIN_IDS.contains(plugin.getArtifactId())) {
                    String version = plugin.getVersion();
                    log.debug(TYCHO_GROUPID + ":" + plugin.getArtifactId() + ":" + version + " configured in "
                            + project);
                    Set<MavenProject> projectSet = versionToProjectsMap.get(version);
                    if (projectSet == null) {
                        projectSet = new LinkedHashSet<MavenProject>();
                        versionToProjectsMap.put(version, projectSet);
                    }
                    projectSet.add(project);
                }
            }
        }
        if (versionToProjectsMap.size() > 1) {
            List<String> versions = new ArrayList<String>(versionToProjectsMap.keySet());
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
        Set<File> baseDirs = new HashSet<File>();
        for (MavenProject project : projects) {
            File basedir = project.getBasedir();
            if (baseDirs.contains(basedir)) {
                throw new MavenExecutionException("Multiple modules within the same basedir are not supported: "
                        + basedir, project.getFile());
            } else {
                baseDirs.add(basedir);
            }
        }
    }

    // workaround for http://jira.codehaus.org/browse/MNG-5206
    // TODO remove method when fix for MNG-5206 is released (maven 3.0.5) 
    private void registerExecutionListener(MavenSession session) {
        MavenExecutionRequest request = session.getRequest();
        ChainedExecutionListener listener = new ChainedExecutionListener(request.getExecutionListener());
        listener.addListener(new AbstractExecutionListener() {

            @Override
            public void sessionEnded(ExecutionEvent event) {
                try {
                    EquinoxServiceFactory equinoxServiceFactory = plexus.lookup(EquinoxServiceFactory.class);
                    if (equinoxServiceFactory != null) {
                        plexus.release(equinoxServiceFactory);
                    }
                } catch (ComponentLifecycleException e) {
                    // we tried
                } catch (ComponentLookupException e) {
                    // we tried
                }
            }

        });
        request.setExecutionListener(listener);
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
