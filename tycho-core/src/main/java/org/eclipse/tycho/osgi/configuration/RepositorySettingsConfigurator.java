/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Issue #797 - Implement a caching P2 transport  
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import java.net.URI;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;

@Component(role = EquinoxLifecycleListener.class, hint = "RepositorySettingsConfigurator")
public class RepositorySettingsConfigurator implements EquinoxLifecycleListener {

    private static final ArtifactRepositoryPolicy P2_REPOSITORY_POLICY = new ArtifactRepositoryPolicy(true,
            ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE);

    @Requirement
    private Logger logger;
    @Requirement
    private LegacySupport context;

    @Requirement
    private SettingsDecrypterHelper decrypter;

    @Requirement
    private RepositorySystem repositorySystem;
    @Requirement(hint = "p2")
    private ArtifactRepositoryLayout p2layout;

    public RepositorySettingsConfigurator() {
        // for plexus
    }

    public RepositorySettingsConfigurator(RepositorySystem repositorySystem) {
        // for test
        this.repositorySystem = repositorySystem;
    }

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        framework.registerService(MavenRepositorySettings.class, new MavenRepositorySettingsProvider());
    }

    private class MavenRepositorySettingsProvider implements MavenRepositorySettings {

        @Override
        public MavenRepositoryLocation getMirror(MavenRepositoryLocation location) {
            if (location.getId() == null) {
                return null;
            }
            ArtifactRepository locationAsMavenRepository = repositorySystem.createArtifactRepository(location.getId(),
                    location.getURL().toString(), p2layout, P2_REPOSITORY_POLICY, P2_REPOSITORY_POLICY);
            Mirror mirror = getTychoMirror(locationAsMavenRepository, context.getSession().getRequest().getMirrors());
            if (mirror != null) {
                return new MavenRepositoryLocation(mirror.getId(), URI.create(mirror.getUrl()));
            }
            return null;
        }

        @Override
        public MavenRepositorySettings.Credentials getCredentials(MavenRepositoryLocation location) {
            if (location.getId() == null) {
                return null;
            }

            Server serverSettings = context.getSession().getSettings().getServer(location.getId());

            if (serverSettings != null) {
                SettingsDecryptionResult result = decrypter.decryptAndLogProblems(serverSettings);
                Server decryptedServer = result.getServer();
                return new MavenRepositorySettings.Credentials(decryptedServer.getUsername(),
                        decryptedServer.getPassword(), location.getURL());
            }
            return null;
        }

    }

    public Mirror getTychoMirror(ArtifactRepository repository, List<Mirror> mirrors) {
        // if we find a mirror the default way (the maven way) we will use that mirror
        Mirror mavenMirror = repositorySystem.getMirror(repository, mirrors);
        if (mavenMirror != null || mirrors == null) {
            return mavenMirror;
        }
        for (Mirror mirror : mirrors) {
            if (isPrefixMirrorOf(repository, mirror)) {
                // We will create a new Mirror that does
                // have the artifacts URL replaced with the Prefix URL from the mirror
                return createMirror(repository, mirror);
            }
        }
        return null;
    }

    private static boolean isPrefixMirrorOf(ArtifactRepository repo, Mirror mirror) {
        boolean isMirrorOfRepoUrl = repo.getUrl() != null && repo.getUrl().startsWith(mirror.getMirrorOf());
        boolean matchesLayout = repo.getLayout() != null
                && repo.getLayout().getId().equals(mirror.getMirrorOfLayouts());
        return isMirrorOfRepoUrl && matchesLayout;
    }

    // We have to create a new Mirror
    private static Mirror createMirror(ArtifactRepository repo, Mirror toMirror) {
        Mirror mirror = toMirror.clone();
        String urlToReplace = toMirror.getMirrorOf();
        String newUrl = StringUtils.replaceOnce(repo.getUrl(), urlToReplace, toMirror.getUrl());
        mirror.setUrl(newUrl);
        mirror.setId(toMirror.getId());
        return mirror;
    }
}
