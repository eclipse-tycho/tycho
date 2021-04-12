/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.core.maven.MavenArtifactFacade;
import org.eclipse.tycho.core.shared.MavenDependenciesResolver;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

@Component(role = EquinoxLifecycleListener.class, hint = "MavenDependenciesResolver")
public class MavenDependenciesResolverConfigurer extends EquinoxLifecycleListener implements MavenDependenciesResolver {

    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport context;

    @Requirement
    private RepositorySystem repositorySystem;

    @Override
    public Collection<?> resolve(String groupId, String artifactId, String version, String packaging, String classifier,
            String dependencyScope) {
        Artifact artifact;
        if (classifier != null && !classifier.isEmpty()) {
            artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, packaging,
                    classifier);
            artifact.setScope(dependencyScope);
        } else {
            artifact = repositorySystem.createArtifact(groupId, artifactId, version, dependencyScope, packaging);
        }
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        MavenSession session = context.getSession();
        request.setOffline(session.isOffline());
        request.setLocalRepository(session.getLocalRepository());
        request.setResolveTransitively(dependencyScope != null && !dependencyScope.isEmpty());
        request.setRemoteRepositories(session.getCurrentProject().getRemoteArtifactRepositories());
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        Set<Artifact> artifacts = result.getArtifacts();
        ArrayList<IArtifactFacade> list = new ArrayList<IArtifactFacade>();
        for (Artifact a : artifacts) {
            list.add(new MavenArtifactFacade(a));
        }
        return list;
    }

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        framework.registerService(MavenDependenciesResolver.class, this);
    }

    @Override
    public File getRepositoryRoot() {

        return new File(context.getSession().getLocalRepository().getBasedir());
    }

}
