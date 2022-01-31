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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.MavenDependencyDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;

@Component(role = WorkspaceReader.class, hint = "TychoWorkspaceReader")
public class TychoWorkspaceReader implements WorkspaceReader {

    private WorkspaceRepository repository;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private EquinoxServiceFactory equinox;

    @Requirement
    private Logger logger;

    public TychoWorkspaceReader() {
        //TODO this requires https://issues.apache.org/jira/browse/MNG-7400
        repository = new WorkspaceRepository("tycho", null);
    }

    @Override
    public WorkspaceRepository getRepository() {
        return repository;
    }

    @Override
    public File findArtifact(Artifact artifact) {
        if ("pom".equals(artifact.getExtension())) {
            //TODO we have not a pom yet but we probably want to generate one, otherwise maven tries to download 
            //it from the repository and would not find it resulting in
            //[WARNING] The POM for org.eclipse.equinox:org.eclipse.equinox.preferences:jar:3.9.0.v20210726-0943 is missing, 
            //  no dependency information available
            return null;
        }
        MavenProject currentProject = legacySupport.getSession().getCurrentProject();
        ReactorProject reactorProject = DefaultReactorProject.adapt(currentProject);

        Optional<DependencyArtifacts> dependencyMetadata = TychoProjectUtils
                .getOptionalDependencyArtifacts(reactorProject);
        if (dependencyMetadata.isPresent()) {
            P2ResolverFactory factory = this.equinox.getService(P2ResolverFactory.class);
            logger.debug("Attempt to resolve " + artifact + " for project " + currentProject + " ...");
            for (ArtifactDescriptor descriptor : dependencyMetadata.get().getArtifacts()) {
                MavenDependencyDescriptor dependencyDescriptor = factory.resolveDependencyDescriptor(descriptor);
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
        return null;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        return Collections.emptyList();
    }

}
