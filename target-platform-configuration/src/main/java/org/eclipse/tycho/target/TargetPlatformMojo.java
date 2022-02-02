/*******************************************************************************
 * Copyright (c) 2013, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Issue #462 - Delay Pom considered items to the final Target Platform calculation 
 *******************************************************************************/
package org.eclipse.tycho.target;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.resolver.DefaultDependencyResolverFactory;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManagerFacade;

@Mojo(name = "target-platform", threadSafe = true)
public class TargetPlatformMojo extends AbstractMojo {
    private static final Object LOCK = new Object();

    // TODO site doc (including steps & parameters handled in afterProjectsRead?)
    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Component
    private EquinoxServiceFactory osgiServices;

    @Component
    private Logger logger;

    @Component
    private LegacySupport legacySupport;

    @Component
    private DefaultDependencyResolverFactory dependencyResolverLocator;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            ReactorRepositoryManagerFacade repositoryManager = osgiServices
                    .getService(ReactorRepositoryManagerFacade.class);
            List<ReactorProjectIdentities> upstreamProjects = getReferencedTychoProjects();
            ReactorProject reactorProject = DefaultReactorProject.adapt(project);
            MavenSession session = legacySupport.getSession();
            DependencyResolver dependencyResolver = dependencyResolverLocator.lookupDependencyResolver(project);
            PomDependencyCollector pomDependenciesCollector = dependencyResolver.resolvePomDependencies(session,
                    project);
            repositoryManager.computeFinalTargetPlatform(reactorProject, upstreamProjects, pomDependenciesCollector);
        }
    }

    private List<ReactorProjectIdentities> getReferencedTychoProjects() throws MojoExecutionException {
        List<ReactorProjectIdentities> result = new ArrayList<>();
        HashSet<GAV> considered = new HashSet<>();

        getTransitivelyReferencedTychoProjects(project.getProjectReferences().values(), considered, result);

        return result;
    }

    private void getTransitivelyReferencedTychoProjects(Collection<MavenProject> candidateProjects,
            HashSet<GAV> consideredProjects, List<ReactorProjectIdentities> result) throws MojoExecutionException {

        for (MavenProject reactorProject : candidateProjects) {
            if (!enterProject(reactorProject, consideredProjects)) {
                continue;
            }

            // check for target platform relevant build results (registered by either p2-metadata-default or attach-artifacts)
            File metadataXml = getAttachedArtifact(reactorProject, TychoConstants.CLASSIFIER_P2_METADATA);
            if (metadataXml == null) {
                continue;
            }
            File artifactXml = getAttachedArtifact(reactorProject, TychoConstants.CLASSIFIER_P2_ARTIFACTS);

            // found a Tycho project -> include in target platform
            logger.debug("Adding reactor project: " + reactorProject.toString());
            ReactorProject tychoReactorProject = DefaultReactorProject.adapt(reactorProject);
            verifyIndexFileLocations(tychoReactorProject, metadataXml, artifactXml);
            result.add(tychoReactorProject.getIdentities());

            getTransitivelyReferencedTychoProjects(reactorProject.getProjectReferences().values(), consideredProjects,
                    result);
        }
    }

    private boolean enterProject(MavenProject project, HashSet<GAV> consideredProjects) {
        GAV projectGav = new GAV(project.getGroupId(), project.getArtifactId(), project.getVersion());

        if (consideredProjects.contains(projectGav)) {
            return false;
        } else {
            consideredProjects.add(projectGav);
            return true;
        }
    }

    private static File getAttachedArtifact(MavenProject project, String classifier) {
        for (Artifact artifact : project.getAttachedArtifacts()) {
            if (classifier.equals(artifact.getClassifier())) {
                return artifact.getFile();
            }
        }
        return null;
    }

    private static void verifyIndexFileLocations(ReactorProject project, File metadataXml, File artifactXml)
            throws MojoExecutionException {
        verifyArtifactLocationInTargetFolder(project, TychoConstants.CLASSIFIER_P2_METADATA,
                TychoConstants.FILE_NAME_P2_METADATA, metadataXml);
        verifyArtifactLocationInTargetFolder(project, TychoConstants.CLASSIFIER_P2_ARTIFACTS,
                TychoConstants.FILE_NAME_P2_ARTIFACTS, artifactXml);
        verifyFilePresenceInTargetFolder(project, TychoConstants.FILE_NAME_LOCAL_ARTIFACTS);
    }

    private static void verifyArtifactLocationInTargetFolder(ReactorProject project, String artifactClassifier,
            String expectedPathInTarget, File actualLocation) throws MojoExecutionException {
        File expectedLocation = project.getBuildDirectory().getChild(expectedPathInTarget);
        if (actualLocation == null) {
            throw new MojoExecutionException("Unexpected build result of " + project + ": Artifact with classifier '"
                    + artifactClassifier + "' expected at location \"" + expectedLocation + "\", but is missing");
        } else if (!(expectedLocation.equals(actualLocation.getAbsoluteFile()))) {
            throw new MojoExecutionException("Unexpected build result of " + project + ": Artifact with classifier '"
                    + artifactClassifier + "' expected at location \"" + expectedLocation + "\", but is at \""
                    + actualLocation.getAbsolutePath() + "\"");
        }
    }

    private static void verifyFilePresenceInTargetFolder(ReactorProject project, String expectedPathInTarget)
            throws MojoExecutionException {
        File expectedLocation = project.getBuildDirectory().getChild(expectedPathInTarget);
        if (!expectedLocation.isFile()) {
            throw new MojoExecutionException(
                    "Unexpected build result of " + project + ": File \"" + expectedLocation + "\" is missing");
        }
    }

}
