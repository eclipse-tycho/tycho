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
package org.eclipse.tycho.p2maven.repository;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.p2maven.helper.SettingsDecrypterHelper;

@Component(role = MavenRepositorySettings.class)
public class DefaultMavenRepositorySettings implements MavenRepositorySettings, Initializable {

    private static final ArtifactRepositoryPolicy P2_REPOSITORY_POLICY = new ArtifactRepositoryPolicy(true,
            ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE);

    @Requirement
    private Logger logger;
    @Requirement
	private LegacySupport legacySupport;

    @Requirement
    private SettingsDecrypterHelper decrypter;

    @Requirement(hint = "p2")
    private ArtifactRepositoryLayout p2layout;

	private Map<String, URI> idToMirrorMap = new HashMap<>();

	private Settings settings;

	private List<Mirror> mirrors;

	private RepositorySystemSession repositorySession;

    public DefaultMavenRepositorySettings() {
        // for plexus
    }

	public DefaultMavenRepositorySettings(RepositorySystemSession repositorySystemSession) {
		// for test
		repositorySession = repositorySystemSession;
    }

    @Override
    public MavenRepositoryLocation getMirror(MavenRepositoryLocation location) {
        if (location.getId() == null) {
            return null;
        }
		if (idToMirrorMap.containsKey(location.getId())) {
			return new MavenRepositoryLocation(location.getId(), idToMirrorMap.get(location.getId()));
		}
		ArtifactRepository locationAsMavenRepository = new MavenArtifactRepository(location.getId(),
                location.getURL().toString(), p2layout, P2_REPOSITORY_POLICY, P2_REPOSITORY_POLICY);
		Mirror mirror = getTychoMirror(locationAsMavenRepository, mirrors);
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
		Server serverSettings = settings.getServer(location.getId());

        if (serverSettings != null) {
            SettingsDecryptionResult result = decrypter.decryptAndLogProblems(serverSettings);
            Server decryptedServer = result.getServer();
            return new MavenRepositorySettings.Credentials(decryptedServer.getUsername(), decryptedServer.getPassword(),
                    location.getURL());
        }
        return null;
    }

    public Mirror getTychoMirror(ArtifactRepository repository, List<Mirror> mirrors) {
        // if we find a mirror the default way (the maven way) we will use that mirror
		Mirror mavenMirror = getMirror(repositorySession, RepositoryUtils.toRepo(repository)).orElse(null);
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

	public void addMirror(String repositoryId, URI mirroredUrl) {
		if (mirroredUrl == null) {
			idToMirrorMap.remove(repositoryId);
		} else {
			idToMirrorMap.put(repositoryId, mirroredUrl);
		}
	}

	@Override
	public void initialize() throws InitializationException {
		MavenSession session = legacySupport.getSession();
		if (session != null) {
			settings = session.getSettings();
			mirrors = session.getRequest().getMirrors();
			repositorySession = session.getRepositorySession();
		} else {
			settings = new Settings();
			mirrors = Collections.emptyList();
		}
	}

	@Override
	public Stream<MavenRepositoryLocation> getMirrors() {
		return mirrors.stream().map(m -> new MavenRepositoryLocation(m.getId(), URI.create(m.getUrl())));
	}

	private static Optional<Mirror> getMirror(RepositorySystemSession session, RemoteRepository remoteRepository) {
		if (session != null) {
			org.eclipse.aether.repository.MirrorSelector selector = session.getMirrorSelector();
			if (selector != null) {
				RemoteRepository repo = selector.getMirror(remoteRepository);
				if (repo != null) {
					Mirror mirror = new Mirror();
					mirror.setId(repo.getId());
					mirror.setUrl(repo.getUrl());
					mirror.setLayout(repo.getContentType());
					mirror.setBlocked(repo.isBlocked());
					return Optional.of(mirror);
				}
			}
		}
		return Optional.empty();
	}
}
