/*******************************************************************************
 * Copyright (c) 2020, 2021 Christoph Läubrich and others.
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
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
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
import org.eclipse.tycho.core.shared.MavenArtifactRepositoryReference;
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
            String dependencyScope, Collection<MavenArtifactRepositoryReference> additionalRepositories,
            Object session) {
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
        MavenSession mavenSession = getMavenSession(session);
        request.setOffline(mavenSession.isOffline());
        request.setLocalRepository(mavenSession.getLocalRepository());
        request.setResolveTransitively(dependencyScope != null && !dependencyScope.isEmpty());
        if (additionalRepositories != null && additionalRepositories.size() > 0) {
            List<ArtifactRepository> repositories = new ArrayList<>(
                    mavenSession.getCurrentProject().getRemoteArtifactRepositories());
            for (MavenArtifactRepositoryReference reference : additionalRepositories) {
                repositories.add(repositorySystem.createArtifactRepository(reference.getId(), reference.getUrl(), null,
                        null, null));
            }
            request.setRemoteRepositories(repositorySystem.getEffectiveRepositories(repositories));
        } else {
            request.setRemoteRepositories(mavenSession.getCurrentProject().getRemoteArtifactRepositories());
        }
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        Set<Artifact> artifacts = result.getArtifacts();
        ArrayList<IArtifactFacade> list = new ArrayList<IArtifactFacade>();
        for (Artifact a : artifacts) {
            list.add(new MavenArtifactFacade(a));
        }
        return list;
    }

    protected MavenSession getMavenSession(Object session) {
        MavenSession mavenSession;
        if (session instanceof MavenSession) {
            mavenSession = (MavenSession) session;
        } else {
            mavenSession = Objects.requireNonNull(context.getSession(),
                    "Can't acquire maven session from context, called outside maven thread context?");
        }
        return mavenSession;
    }

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        framework.registerService(MavenDependenciesResolver.class, this);
    }

    @Override
    public File getRepositoryRoot() {
        return new File(getMavenSession(null).getLocalRepository().getBasedir());
    }

}
