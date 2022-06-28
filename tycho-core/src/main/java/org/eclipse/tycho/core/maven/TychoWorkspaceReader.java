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
import java.util.Optional;

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
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.MavenDependencyDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;

@Component(role = WorkspaceReader.class, hint = "TychoWorkspaceReader")
public class TychoWorkspaceReader implements MavenWorkspaceReader {

    private WorkspaceRepository repository;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private EquinoxServiceFactory equinox;

    @Requirement
    private Logger logger;

    @Requirement
    private ModelWriter modelWriter;

    public TychoWorkspaceReader() {
        repository = new WorkspaceRepository("tycho", null);
    }

    @Override
    public WorkspaceRepository getRepository() {
        return repository;
    }

    @Override
    public File findArtifact(Artifact artifact) {
        if ("pom".equals(artifact.getExtension())) {
            if (artifact.getGroupId().startsWith(TychoConstants.P2_GROUPID_PREFIX)) {
                //TODO Maven should actually call the findModel instead see: https://issues.apache.org/jira/browse/MNG-7496
                logger.debug("Find the pom for " + artifact);
                File pomFile = getFileForArtifact(artifact);
                if (pomFile.isFile()) {
                    return pomFile;
                }
                Model findModel = findModel(artifact);
                if (findModel != null) {
                    try {
                        pomFile.getParentFile().mkdirs();
                        modelWriter.write(pomFile, new HashMap<>(), findModel);
                        return pomFile;
                    } catch (IOException e) {
                        logger.debug("Can't write model!", e);
                    }
                }
            }
            return null;
        }
        if (artifact.getGroupId().startsWith(TychoConstants.P2_GROUPID_PREFIX)) {
            //For now, only take P2 items into account ...
            File cachedFile = getFileForArtifact(artifact);
            if (cachedFile.isFile()) {
                return cachedFile;
            }
            MavenSession session = legacySupport.getSession();
            if (session != null) {
                MavenProject currentProject = session.getCurrentProject();
                ReactorProject reactorProject = DefaultReactorProject.adapt(currentProject);

                Optional<DependencyArtifacts> dependencyMetadata = TychoProjectUtils
                        .getOptionalDependencyArtifacts(reactorProject);
                if (dependencyMetadata.isPresent()) {
                    P2ResolverFactory factory = this.equinox.getService(P2ResolverFactory.class);
                    logger.debug("Attempt to resolve " + artifact + " for project " + currentProject + " ...");
                    for (ArtifactDescriptor descriptor : dependencyMetadata.get().getArtifacts()) {
                        MavenDependencyDescriptor dependencyDescriptor = factory
                                .resolveDependencyDescriptor(descriptor);
                        if (dependencyDescriptor != null) {
                            if (dependencyDescriptor.getGroupId().equals(artifact.getGroupId())
                                    && dependencyDescriptor.getArtifactId().equals(artifact.getArtifactId())) {
                                ArtifactKey artifactKey = descriptor.getKey();
                                if (dependencyDescriptor.getVersion().equals(artifact.getVersion())
                                        || artifactKey.getVersion().equals(artifact.getVersion())) {
                                    //we have a match!
                                    return descriptor.getLocation(true);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // TODO This is a "standard" maven artifact, but we probably still know it from P2!
            // it would be good to reuse the P2 item as it might be signed but that must be
            // considered carefully as P2 items have a different location so we must relocate them.
            // But if we do so, this might confuse standard maven if it finds things not matching what is in central...
            // What should work quite good is to first ask all remote repositories for the file and then fall back to P2...
            // The main issue here is the pom=consider that requires special filename mapping while maven is happy with a different path as well.
            // So another approach might be to adjust the pom=consider to ignore dependencies added by tycho itself!

        }
        return null;
    }

    protected File getFileForArtifact(Artifact artifact) {
        RepositorySystemSession repositorySession = legacySupport.getRepositorySession();
        LocalRepository localRepository = repositorySession.getLocalRepository();
        File basedir = localRepository.getBasedir();
        File cachedFile = new File(basedir, "p2/osgi/bundle/" + artifact.getArtifactId() + "/" + artifact.getVersion()
                + "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getExtension());
        return cachedFile;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        return Collections.emptyList();
    }

    @Override
    public Model findModel(Artifact artifact) {
        if (artifact.getGroupId().startsWith(TychoConstants.P2_GROUPID_PREFIX)) {
            logger.debug("Find the model for: " + artifact);
            Model model = new Model();
            model.setModelVersion("4.0.0");
            model.setArtifactId(artifact.getArtifactId());
            model.setGroupId(artifact.getGroupId());
            model.setVersion(artifact.getVersion());
            model.setPackaging(artifact.getProperty("packaging", null));
            if (model.getPackaging() == null) {
                model.setPackaging(
                        artifact.getGroupId().substring(TychoConstants.P2_GROUPID_PREFIX.length()).replace('.', '-'));
            }
            return model;
        }
        return null;
    }

}
