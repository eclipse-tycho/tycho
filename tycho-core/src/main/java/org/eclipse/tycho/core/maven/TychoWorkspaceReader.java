/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

@Component(role = WorkspaceReader.class, hint = "TychoWorkspaceReader")
public class TychoWorkspaceReader implements MavenWorkspaceReader {
    private final WorkspaceRepository repository;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private Logger logger;

    @Requirement
    private ModelWriter modelWriter;

    @Requirement
    private TychoProjectManager projectManager;

    public TychoWorkspaceReader() {
        repository = new WorkspaceRepository("tycho", null);
    }

    @Override
    public WorkspaceRepository getRepository() {
        return repository;
    }

    @Override
    public File findArtifact(final Artifact artifact) {
        final boolean isP2Artifact = artifact.getGroupId().startsWith(TychoConstants.P2_GROUPID_PREFIX);

        // TODO: Maven should actually call #findModel instead
        //  See https://issues.apache.org/jira/browse/MNG-7496
        if ("pom".equals(artifact.getExtension())) {
            if (isP2Artifact) {
                return findPomArtifact(artifact);
            }

            return null;
        }

        if (isP2Artifact) {
            return findP2Artifact(artifact);
        }

        // TODO: this is a "standard" Maven artifact, but we probably still know it from P2!
        //  It would be good to reuse the P2 item as it might be signed but that must be
        //  considered carefully as P2 items have a different location so we must relocate them.
        //  But if we do so, this might confuse standard Maven if it finds things not matching what is in central.
        //  What should work quite good is to first ask all remote repositories for the file and then fall back to P2.
        //  The main issue here is the 'pom=consider' that requires special filename mapping while Maven is happy with
        //  a different path as well. So another approach might be to adjust the 'pom=consider' to ignore dependencies
        //  added by tycho itself.
        return findMavenArtifact(artifact);
    }

    @Override
    public List<String> findVersions(final Artifact artifact) {
        return Collections.emptyList();
    }

    @Override
    public Model findModel(final Artifact artifact) {
        if (artifact.getGroupId().startsWith(TychoConstants.P2_GROUPID_PREFIX)) {
            logger.debug("Find the model for " + artifact);
            // TODO: due to a bug in Maven we can not use this here.
            //  See Tycho issue #1388
            //  See https://issues.apache.org/jira/browse/MNG-7544
            // return getP2Model(artifact);
        }

        return null;
    }

    private File findPomArtifact(final Artifact artifact) {
        logger.debug("Find the POM file for " + artifact);
        final File pomFile = getFileForArtifact(artifact);

        if (pomFile.isFile()) {
            return pomFile;
        }

        pomFile.getParentFile().mkdirs();

        try {
            modelWriter.write(pomFile, new HashMap<>(), getP2Model(artifact));
            return pomFile;
        } catch (final IOException e) {
            logger.debug("Cannot write the POM model", e);
        }

        return null;
    }

    private File findP2Artifact(final Artifact artifact) {
        // Attempt a fast lookup first
        final File cachedFile = getFileForArtifact(artifact);

        if (cachedFile.isFile()) {
            return cachedFile;
        }

        final MavenSession session = legacySupport.getSession();

        if (session != null) {
            final MavenProject currentProject = session.getCurrentProject();
            final ReactorProject reactorProject = DefaultReactorProject.adapt(currentProject);
            final DependencyArtifacts dependencyMetadata = (DependencyArtifacts) reactorProject
                    .getContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS);
            if (dependencyMetadata != null) {
                logger.debug("Attempting to resolve " + artifact + " for project " + currentProject);

                for (final ArtifactDescriptor descriptor : dependencyMetadata.getArtifacts()) {
                    if (isArtifactMatch(descriptor.getKey(), artifact)) {
                        return descriptor.getLocation(true);
                    }
                }
            }
        }

        return null;
    }

    private boolean isArtifactMatch(final ArtifactKey artifactKey, final Artifact artifact) {
        final String groupId = artifact.getGroupId();
        final String type = groupId.substring(TychoConstants.P2_GROUPID_PREFIX.length()).replace('.', '-');
        return artifactKey.getType().equals(type) && artifactKey.getId().equals(artifact.getArtifactId())
                && artifactKey.getVersion().equals(artifact.getVersion());
    }

    private File findMavenArtifact(@SuppressWarnings("unused") final Artifact artifact) {
        // TODO: the implementation should be quite similar to #findP2Artifact.
        //  Inject and use P2ResolverFactory#resolveDependencyDescriptor(ArtifactDescriptor)
        //  to get the Maven groupId/artifactId/version information, which must then be
        //  compared to the inputted artifact information.
        //  The returned Maven information should also be validated.
        //  For that, see MavenDependencyInjector#isValidMavenDescriptor
        return null;
    }

    private File getFileForArtifact(final Artifact artifact) {
        final RepositorySystemSession repositorySession = legacySupport.getRepositorySession();
        final LocalRepository localRepository = repositorySession.getLocalRepository();
        final File basedir = localRepository.getBasedir();
        final String repositoryPath = "p2/osgi/bundle/" + artifact.getArtifactId() + "/" + artifact.getVersion() + "/"
                + artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getExtension();

        return new File(basedir, repositoryPath);
    }

    private Model getP2Model(final Artifact artifact) {
        final String groupId = artifact.getGroupId();
        final Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setArtifactId(artifact.getArtifactId());
        model.setGroupId(groupId);
        model.setVersion(artifact.getVersion());
        model.setPackaging(artifact.getProperty("packaging", null));

        if (model.getPackaging() == null) {
            model.setPackaging(groupId.substring(TychoConstants.P2_GROUPID_PREFIX.length()).replace('.', '-'));
        }

        return model;
    }
}
