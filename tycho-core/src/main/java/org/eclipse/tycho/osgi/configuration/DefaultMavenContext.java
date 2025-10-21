/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;

@Named
@Singleton
public class DefaultMavenContext implements MavenContext {

    @Inject
    ArtifactHandlerManager artifactHandlerManager;
    @Inject
    LegacySupport legacySupport;

    @Inject
    @Named(FilteringMavenLogger.HINT)
    MavenLogger mavenLogger;

    private Properties globalProps;
    private List<MavenRepositoryLocation> repositoryLocations;
    private ChecksumPolicy checksumPolicy;
    private Boolean updateSnapshots;
    private File repoDir;
    private Collection<ReactorProject> projects;
    private Boolean isOffline;

    public DefaultMavenContext() {
    }

    @Override
    public String getExtension(String artifactType) {
        if (artifactType == null) {
            return "jar";
        }
        ArtifactHandler handler = artifactHandlerManager.getArtifactHandler(artifactType);
        return handler.getExtension();
    }

    @Override
    public boolean isUpdateSnapshots() {
        if (updateSnapshots == null) {
            updateSnapshots = getSession().map(s -> s.getRequest().isUpdateSnapshots()).orElse(false);
        }
        return updateSnapshots;
    }

    @Override
    public Stream<MavenRepositoryLocation> getMavenRepositoryLocations() {
        if (repositoryLocations == null) {
            repositoryLocations = getSession().map(s -> s.getProjects().stream()
                    .map(MavenProject::getRemoteArtifactRepositories).flatMap(Collection::stream)
                    .filter(r -> r.getLayout() instanceof P2ArtifactRepositoryLayout).map(r -> {
                        try {
                            return new MavenRepositoryLocation(r.getId(), new URL(r.getUrl()).toURI());
                        } catch (MalformedURLException | URISyntaxException e) {
                            return null;
                        }
                    }).filter(Objects::nonNull).collect(Collectors.toUnmodifiableList()))
                    .orElse(Collections.emptyList());
        }
        return repositoryLocations.stream();
    }

    @Override
    public ChecksumPolicy getChecksumsMode() {
        if (checksumPolicy == null) {
            if (MavenExecutionRequest.CHECKSUM_POLICY_FAIL
                    .equals(getSession().map(s -> s.getRequest().getGlobalChecksumPolicy()).orElse(null))) {
                checksumPolicy = ChecksumPolicy.STRICT;
            } else {
                checksumPolicy = ChecksumPolicy.LAX;
            }
        }
        return checksumPolicy;
    }

    @Override
    public File getLocalRepositoryRoot() {
        if (repoDir == null) {
            repoDir = getSession().map(s -> s.getLocalRepository().getBasedir()).map(File::new)
                    .orElse(TychoConstants.DEFAULT_USER_LOCALREPOSITORY);
        }
        return repoDir;
    }

    @Override
    public synchronized MavenLogger getLogger() {
        return mavenLogger;
    }

    @Override
    public boolean isOffline() {
        if (isOffline == null) {
            isOffline = getSession().map(s -> s.isOffline()).orElse(false);
        }
        return isOffline;
    }

    @Override
    public Properties getSessionProperties() {
        if (globalProps == null) {
            globalProps = getSession().map(session -> MavenContextConfigurator.getGlobalProperties(session))
                    .orElse(new Properties());
        }
        return globalProps;
    }

    @Override
    public Collection<ReactorProject> getProjects() {
        if (projects == null) {
            projects = getSession().map(s -> s.getProjects().stream().map(DefaultReactorProject::adapt)
                    .collect(Collectors.toUnmodifiableList())).orElse(Collections.emptyList());
        }
        return projects;
    }

    private Optional<MavenSession> getSession() {
        if (legacySupport == null) {
            mavenLogger.warn("Legacy support not available");
            return Optional.empty();
        }
        MavenSession session = legacySupport.getSession();
        if (session == null) {
            if (mavenLogger.isDebugEnabled()) {
                Thread.dumpStack();
            }
            mavenLogger.warn("Not called from a maven thread");
            return Optional.empty();
        }
        return Optional.of(session);
    }

}
