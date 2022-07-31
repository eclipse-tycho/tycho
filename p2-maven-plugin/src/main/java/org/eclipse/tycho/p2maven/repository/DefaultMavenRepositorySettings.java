/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Issue #797 - Implement a caching P2 transport  
 *******************************************************************************/
package org.eclipse.tycho.p2maven.repository;

import java.net.URI;

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
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.p2maven.helper.SettingsDecrypterHelper;

@Component(role = MavenRepositorySettings.class)
public class DefaultMavenRepositorySettings implements MavenRepositorySettings {

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

	@Requirement(role = TychoMirrorSelector.class)
	private TychoMirrorSelector mirrorSelector;

	@Override
	public MavenRepositoryLocation getMirror(MavenRepositoryLocation location) {
		if (location.getId() == null) {
			return null;
		}
		ArtifactRepository locationAsMavenRepository = repositorySystem.createArtifactRepository(location.getId(),
				location.getURL().toString(), p2layout, P2_REPOSITORY_POLICY, P2_REPOSITORY_POLICY);
		Mirror mirror = repositorySystem.getMirror(locationAsMavenRepository,
				context.getSession().getRequest().getMirrors());

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
			return new MavenRepositorySettings.Credentials(decryptedServer.getUsername(), decryptedServer.getPassword(),
					location.getURL());
		}
		return null;
	}

}
