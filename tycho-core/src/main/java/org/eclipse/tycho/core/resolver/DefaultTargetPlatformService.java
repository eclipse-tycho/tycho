/*******************************************************************************
 * Copyright (c) 2013, 2022 SAP AG and others.
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
package org.eclipse.tycho.core.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;

@Component(role = TargetPlatformService.class)
public class DefaultTargetPlatformService implements TargetPlatformService {

    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private DefaultDependencyResolverFactory dependencyResolverLocator;

    @Requirement
    ReactorRepositoryManager repositoryManager;

    @Override
    public Optional<TargetPlatform> getTargetPlatform(ReactorProject project) throws DependencyResolutionException {
        synchronized (project) {
            Object contextValue = project.getContextValue(TargetPlatform.FINAL_TARGET_PLATFORM_KEY);
            if (contextValue instanceof TargetPlatform) {
                return Optional.of((TargetPlatform) contextValue);
            }
            MavenSession session = legacySupport.getSession();
            if (repositoryManager == null) {
                return Optional.empty();
            }
            List<ReactorProjectIdentities> upstreamProjects = getReferencedTychoProjects(project);
            DependencyResolver dependencyResolver = dependencyResolverLocator.lookupDependencyResolver(project);
            PomDependencyCollector pomDependenciesCollector = dependencyResolver.resolvePomDependencies(session,
                    project.adapt(MavenProject.class));
            TargetPlatform finalTargetPlatform = repositoryManager.computeFinalTargetPlatform(project, upstreamProjects,
                    pomDependenciesCollector);
            return Optional.ofNullable(finalTargetPlatform);
        }
    }

    private List<ReactorProjectIdentities> getReferencedTychoProjects(ReactorProject reactorProject)
            throws DependencyResolutionException {
        List<ReactorProjectIdentities> result = new ArrayList<>();

        MavenProject mavenProject = reactorProject.adapt(MavenProject.class);

        if (mavenProject != null) {
            HashSet<GAV> considered = new HashSet<>();
            Collection<MavenProject> values = mavenProject.getProjectReferences().values();
            getTransitivelyReferencedTychoProjects(values, considered, result);
        }
        return result;
    }

    private void getTransitivelyReferencedTychoProjects(Collection<MavenProject> candidateProjects,
            HashSet<GAV> consideredProjects, List<ReactorProjectIdentities> result)
            throws DependencyResolutionException {

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

            Collection<MavenProject> values = reactorProject.getProjectReferences().values();
            getTransitivelyReferencedTychoProjects(values, consideredProjects, result);
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
            throws DependencyResolutionException {
        verifyArtifactLocationInTargetFolder(project, TychoConstants.CLASSIFIER_P2_METADATA,
                TychoConstants.FILE_NAME_P2_METADATA, metadataXml);
        verifyArtifactLocationInTargetFolder(project, TychoConstants.CLASSIFIER_P2_ARTIFACTS,
                TychoConstants.FILE_NAME_P2_ARTIFACTS, artifactXml);
        verifyFilePresenceInTargetFolder(project, TychoConstants.FILE_NAME_LOCAL_ARTIFACTS);
    }

    private static void verifyArtifactLocationInTargetFolder(ReactorProject project, String artifactClassifier,
            String expectedPathInTarget, File actualLocation) throws DependencyResolutionException {
        File expectedLocation = project.getBuildDirectory().getChild(expectedPathInTarget);
        if (actualLocation == null) {
            throw new DependencyResolutionException(
                    "Unexpected build result of " + project + ": Artifact with classifier '" + artifactClassifier
                            + "' expected at location \"" + expectedLocation + "\", but is missing");
        } else if (!(expectedLocation.equals(actualLocation.getAbsoluteFile()))) {
            throw new DependencyResolutionException("Unexpected build result of " + project
                    + ": Artifact with classifier '" + artifactClassifier + "' expected at location \""
                    + expectedLocation + "\", but is at \"" + actualLocation.getAbsolutePath() + "\"");
        }
    }

    private static void verifyFilePresenceInTargetFolder(ReactorProject project, String expectedPathInTarget)
            throws DependencyResolutionException {
        File expectedLocation = project.getBuildDirectory().getChild(expectedPathInTarget);
        if (!expectedLocation.isFile()) {
            throw new DependencyResolutionException(
                    "Unexpected build result of " + project + ": File \"" + expectedLocation + "\" is missing");
        }
    }

    @Override
    public void clearTargetPlatform(ReactorProject project) {
        synchronized (project) {
            project.setContextValue(TargetPlatform.FINAL_TARGET_PLATFORM_KEY, null);
        }

    }
}
