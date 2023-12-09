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
import org.eclipse.tycho.p2.repository.PublishingRepository;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.p2resolver.PreliminaryTargetPlatformImpl;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;

@Component(role = TargetPlatformService.class)
public class DefaultTargetPlatformService implements TargetPlatformService {

    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement(hint = "p2")
    private DependencyResolver dependencyResolver;

    @Requirement
    private ReactorRepositoryManager repositoryManager;

    @Requirement
    private P2ResolverFactory p2ResolverFactory;

    @Requirement
    private TargetPlatformFactory tpFactory;

    @Override
    public Optional<TargetPlatform> getTargetPlatform() throws DependencyResolutionException {
        MavenSession session = legacySupport.getSession();
        if (session == null) {
            return Optional.empty();
        }
        MavenProject mavenProject = session.getCurrentProject();
        if (mavenProject == null) {
            return Optional.empty();
        }
        return getTargetPlatform(DefaultReactorProject.adapt(mavenProject));
    }

    @Override
    public Optional<TargetPlatform> getTargetPlatform(ReactorProject project) throws DependencyResolutionException {
        return project.computeContextValue(TargetPlatform.FINAL_TARGET_PLATFORM_KEY, () -> {
            MavenSession session = legacySupport.getSession();
            if (repositoryManager == null || session == null) {
                return Optional.empty();
            }
            List<ReactorProjectIdentities> upstreamProjects = getReferencedTychoProjects(project);
            PomDependencyCollector pomDependenciesCollector = dependencyResolver.resolvePomDependencies(session,
                    project.adapt(MavenProject.class));
            TargetPlatform finalTargetPlatform = computeFinalTargetPlatform(project, upstreamProjects,
                    pomDependenciesCollector);
            return Optional.ofNullable(finalTargetPlatform);
        });
    }

    /**
     * Computes the (immutable) target platform with final p2 metadata and attaches it to the given
     * project.
     * 
     * @param project
     *            the reactor project to compute the target platform for.
     * @param upstreamProjects
     *            Other projects in the reactor which have already been built and may be referenced
     *            by the given project.
     */
    private TargetPlatform computeFinalTargetPlatform(ReactorProject project,
            List<? extends ReactorProjectIdentities> upstreamProjects, PomDependencyCollector pomDependencyCollector) {
        MavenSession session = project.adapt(MavenSession.class);
        if (session == null) {
            session = legacySupport.getSession();
        }
        PreliminaryTargetPlatformImpl preliminaryTargetPlatform = (PreliminaryTargetPlatformImpl) dependencyResolver
                .computePreliminaryTargetPlatform(project, DefaultReactorProject.adapt(session));
        List<PublishingRepository> upstreamProjectResults = getBuildResults(upstreamProjects);
        TargetPlatform result = tpFactory.createTargetPlatformWithUpdatedReactorContent(preliminaryTargetPlatform,
                upstreamProjectResults, pomDependencyCollector);
        return result;
    }

    private List<PublishingRepository> getBuildResults(List<? extends ReactorProjectIdentities> projects) {
        List<PublishingRepository> results = new ArrayList<>(projects.size());
        for (ReactorProjectIdentities project : projects) {
            results.add(repositoryManager.getPublishingRepository(project));
        }
        return results;
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
            logger.debug("Adding reactor project: " + reactorProject);
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
